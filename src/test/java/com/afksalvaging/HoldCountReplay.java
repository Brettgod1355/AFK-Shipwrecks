/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import com.afksalvaging.replay.Recording;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Item;
import net.runelite.api.MenuAction;
import net.runelite.api.WorldEntity;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;

/**
 * Plays Black Box recordings through {@link AfkSession}'s hold counting, the way {@link AfkSalvagingPlugin}
 * feeds it in game, and notes the count against every hold read. Test sources only: recordings stay on the
 * owner's PC (owner, 2026-10-13: "build it that way, local only"), so nothing here needs Black Box.
 * <p>
 * Recordings of one character are played in order, the count carried from one into the next as the plugin
 * remembers it across a logout. What a recording cannot give is filled in the plugin's own way or left out:
 * <ul>
 * <li>Crewmates are recorded by the game's id only; {@link #CREW} names them (worked out from the owner's
 * two boats on 2026-10-13). An id not there is reported in {@link Result#unknownCrew}.</li>
 * <li>Hook positions in the world are not recorded, so hooks go in without one and the session falls back
 * to the player's position with the longer reach, as the plugin does when it cannot place a hook.</li>
 * <li>Whether an item stacks is not recorded: it is taken to stack once any recording line shows it more
 * than one to a slot.</li>
 * <li>The hold's size comes from the hold object on the boat, not the hold window.</li>
 * </ul>
 */
final class HoldCountReplay
{
	/** The game's crewmate ids, from the crew slots and who spoke aboard on the owner's boats (2026-10-13). */
	static final Map<Integer, String> CREW;

	static
	{
		Map<Integer, String> crew = new HashMap<>();
		crew.put(1, "Jobless Jim");
		crew.put(4, "Jolly Jim");
		crew.put(9, "Ex-Captain Siad");
		crew.put(10, "Cabin Boy Jenkins");
		CREW = Collections.unmodifiableMap(crew);
	}

	private static final int[] CREW_SLOT_VARBITS = {
		VarbitID.SAILING_CREW_SLOT_1, VarbitID.SAILING_CREW_SLOT_2, VarbitID.SAILING_CREW_SLOT_3,
		VarbitID.SAILING_CREW_SLOT_4, VarbitID.SAILING_CREW_SLOT_5
	};
	private static final int[] CREW_POSITION_VARBITS = {
		VarbitID.SAILING_CREW_SLOT_1_POSITION, VarbitID.SAILING_CREW_SLOT_2_POSITION, VarbitID.SAILING_CREW_SLOT_3_POSITION,
		VarbitID.SAILING_CREW_SLOT_4_POSITION, VarbitID.SAILING_CREW_SLOT_5_POSITION
	};
	private static final DateTimeFormatter STARTED = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	/** One hold read: what the hold held and what the count said just before. */
	static final class Check
	{
		final String recording;
		final int tick;
		final int hold;
		final int countBefore;
		/** The first read after the hold window opened: a difference here is drift, not a click or a landing. */
		final boolean opening;

		Check(String recording, int tick, int hold, int countBefore, boolean opening)
		{
			this.recording = recording;
			this.tick = tick;
			this.hold = hold;
			this.countBefore = countBefore;
			this.opening = opening;
		}

		boolean drift()
		{
			return opening && countBefore != CargoHoldCapacity.UNKNOWN && countBefore != hold;
		}

		@Override
		public String toString()
		{
			return recording + " tick " + tick + ": hold " + hold + ", count " + countBefore + (opening ? " (opening)" : "");
		}
	}

	static final class Result
	{
		final List<Check> checks = new ArrayList<>();
		final Set<Integer> unknownCrew = new HashSet<>();

		List<Check> drift()
		{
			List<Check> out = new ArrayList<>();
			for (Check check : checks)
			{
				if (check.drift())
				{
					out.add(check);
				}
			}
			return out;
		}
	}

	private final Result result = new Result();
	private final Map<ShipwreckType, String> memory = new EnumMap<>(ShipwreckType.class);
	private final Set<Integer> stackable = new HashSet<>();
	/** The count carried between recordings, per boat slot, as the plugin saves it. */
	private final Map<Integer, Integer> savedUsed = new HashMap<>();

	// Per recording, as the plugin clears it at the login screen.
	private AfkSession session;
	private String name;
	private long started;
	private int slot;
	private int monitoredSlot;
	private boolean holdOpen;
	private boolean openingReadDue;
	private int lastSailingXp;
	private int boostedLevel;
	private int realLevel;
	private int playerView;
	private final int[] crewIds = new int[CREW_SLOT_VARBITS.length];
	private final int[] crewPositions = new int[CREW_POSITION_VARBITS.length];
	private final Map<Integer, Map<Long, Integer>> objectsByView = new HashMap<>();
	private final Set<Integer> holdItemIds = new HashSet<>();
	private Map<Integer, Integer> lastInventory = new HashMap<>();
	private Item[] lastInventoryItems = new Item[0];
	private int holdCapacity;

	/** Plays one character's recordings, oldest first. */
	Result play(List<Recording> recordings, List<String> names)
	{
		for (Recording recording : recordings)
		{
			for (Recording.Line line : recording.lines())
			{
				for (JsonElement e : itemsOf(line))
				{
					JsonArray pair = e.getAsJsonArray();
					if (pair.get(1).getAsInt() > 1)
					{
						stackable.add(pair.get(0).getAsInt());
					}
				}
			}
		}
		for (int i = 0; i < recordings.size(); i++)
		{
			playOne(recordings.get(i), names.get(i));
		}
		return result;
	}

	private void playOne(Recording recording, String recordingName)
	{
		session = new AfkSession(new AfkSession.MemoryStore()
		{
			@Override
			public String load(ShipwreckType wreck)
			{
				return memory.get(wreck);
			}

			@Override
			public void save(ShipwreckType wreck, String value)
			{
				memory.put(wreck, value);
			}

			@Override
			public void clear(ShipwreckType wreck)
			{
				memory.remove(wreck);
			}
		});
		session.settings().salvagingWorld = true;
		name = recordingName;
		started = 0;
		slot = 0;
		monitoredSlot = 0;
		holdOpen = false;
		openingReadDue = false;
		lastSailingXp = -1;
		boostedLevel = 1;
		realLevel = 1;
		playerView = -1;
		java.util.Arrays.fill(crewIds, 0);
		java.util.Arrays.fill(crewPositions, 0);
		objectsByView.clear();
		holdItemIds.clear();
		lastInventory = new HashMap<>();
		lastInventoryItems = new Item[0];
		holdCapacity = CargoHoldCapacity.UNKNOWN;

		for (Recording.Line line : recording.lines())
		{
			switch (line.kind())
			{
				case "session":
					started = LocalDateTime.parse(line.text("started"), STARTED).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
					break;
				case "var":
					var(line);
					break;
				case "stat":
					stat(line);
					break;
				case "objectAdd":
					objectAdd(line);
					break;
				case "objectGone":
					objectGone(line);
					break;
				case "open":
				case "close":
					if (line.number("interface", -1) == InterfaceID.SAILING_BOAT_CARGOHOLD)
					{
						holdOpen = line.kind().equals("open");
						openingReadDue = holdOpen;
					}
					break;
				case "items":
					items(line);
					break;
				case "click":
					click(line);
					break;
				case "npcSay":
					npcSay(line);
					break;
				case "chat":
					chat(line);
					break;
				case "tick":
					tick(line);
					break;
				default:
					break;
			}
		}
		if (slot != 0 && session.monitor().hasCount())
		{
			savedUsed.put(slot, session.monitor().getUsed());
		}
	}

	private long now(Recording.Line line)
	{
		return started + line.ms();
	}

	private void var(Recording.Line line)
	{
		int varbit = line.number("varbit", -1);
		int value = line.number("value", 0);
		if (varbit == VarbitID.SAILING_LAST_PERSONAL_BOAT_BOARDED)
		{
			slot = value;
			reloadHold();
			return;
		}
		for (int i = 0; i < CREW_SLOT_VARBITS.length; i++)
		{
			if (varbit == CREW_SLOT_VARBITS[i] || varbit == CREW_POSITION_VARBITS[i])
			{
				if (varbit == CREW_SLOT_VARBITS[i])
				{
					crewIds[i] = value;
				}
				else
				{
					crewPositions[i] = value;
				}
				Crewmate crewmate = null;
				if (crewIds[i] != 0)
				{
					String crewName = CREW.get(crewIds[i]);
					if (crewName == null)
					{
						result.unknownCrew.add(crewIds[i]);
						crewName = "";
					}
					crewmate = new Crewmate(crewIds[i], crewName, Crewmate.knownDeckhandiness(crewName));
				}
				session.roster().setCrewmate(i, crewmate);
				session.roster().setPosition(i, crewPositions[i]);
				return;
			}
		}
	}

	/** The plugin's reloadHold, without a window to read: the remembered count until the hold is seen. */
	private void reloadHold()
	{
		if (slot != monitoredSlot)
		{
			session.monitor().reset();
			holdItemIds.clear();
			monitoredSlot = slot;
			session.monitor().setCapacity(holdCapacity);
		}
		Integer saved = savedUsed.get(slot);
		if (slot != 0 && saved != null && !session.monitor().hasCount())
		{
			session.monitor().setUsed(saved);
		}
	}

	private void stat(Recording.Line line)
	{
		if (!"SAILING".equals(line.text("skill")))
		{
			return;
		}
		boostedLevel = line.number("boosted", boostedLevel);
		realLevel = line.number("level", realLevel);
		int xp = line.number("xp", 0);
		int delta = lastSailingXp >= 0 ? xp - lastSailingXp : 0;
		lastSailingXp = xp;
		if (delta > 0)
		{
			session.sailingXp(delta, line.tick());
		}
	}

	private void objectAdd(Recording.Line line)
	{
		int view = line.number("wv", -1);
		int id = line.number("id", -1);
		long hash = line.json().get("hash").getAsLong();
		objectsByView.computeIfAbsent(view, v -> new HashMap<>()).put(hash, id);
		if (view <= 0)
		{
			if (ShipwreckType.fromObjectId(id) != null)
			{
				session.wreckSeen(point(line, "x", "y", "plane"), id, now(line));
			}
			return;
		}
		if (CargoHoldCapacity.isCargoHold(id) && view == session.boat().getWorldViewId())
		{
			holdCapacity = CargoHoldCapacity.forObjectId(id);
			session.monitor().setCapacity(holdCapacity);
		}
		session.boat().add(view, hash, id);
	}

	private void objectGone(Recording.Line line)
	{
		int view = line.number("wv", -1);
		int id = line.number("id", -1);
		long hash = line.json().get("hash").getAsLong();
		Map<Long, Integer> objects = objectsByView.get(view);
		if (objects != null)
		{
			objects.remove(hash);
		}
		session.boat().remove(view, hash);
		if (view <= 0)
		{
			session.wreckGone(point(line, "x", "y", "plane"), id, now(line));
		}
	}

	/** The plugin's followBoat: the boat's objects seen before it was followed are picked up at once. */
	private void followBoat(int view)
	{
		if (session.boat().getWorldViewId() == view)
		{
			return;
		}
		session.boat().follow(view);
		Map<Long, Integer> objects = objectsByView.get(view);
		if (objects == null)
		{
			return;
		}
		for (Map.Entry<Long, Integer> object : objects.entrySet())
		{
			if (CargoHoldCapacity.isCargoHold(object.getValue()))
			{
				holdCapacity = CargoHoldCapacity.forObjectId(object.getValue());
				session.monitor().setCapacity(holdCapacity);
			}
			session.boat().add(view, object.getKey(), object.getValue());
		}
	}

	private void items(Recording.Line line)
	{
		int container = line.number("container", -1);
		Item[] items = items(line.json().getAsJsonArray("items"));
		if (container == InventoryID.INV)
		{
			inventory(items, line.tick());
			return;
		}
		int holdSlot = CargoHoldContainers.slotFor(container);
		if (holdSlot == 0 || holdSlot != slot)
		{
			return;
		}
		holdItemIds.clear();
		for (Item item : items)
		{
			if (item.getId() > -1 && item.getQuantity() > 0)
			{
				holdItemIds.add(item.getId());
			}
		}
		session.holdSpaceRecounted(holdSpace(lastInventoryItems));
		if (holdSlot != monitoredSlot)
		{
			session.monitor().reset();
			monitoredSlot = holdSlot;
		}
		int used = CargoHoldMonitor.countUsed(items);
		boolean opening = openingReadDue;
		openingReadDue = false;
		int before = session.monitor().hasCount() ? session.monitor().getUsed() : CargoHoldCapacity.UNKNOWN;
		result.checks.add(new Check(name, line.tick(), used, before, opening));
		session.holdCount(used, line.tick());
	}

	private void inventory(Item[] items, int tick)
	{
		int occupied = 0;
		int salvage = 0;
		Map<Integer, Integer> counts = new HashMap<>();
		Set<ShipwreckType> types = EnumSet.noneOf(ShipwreckType.class);
		for (Item item : items)
		{
			if (item.getId() <= -1 || item.getQuantity() <= 0)
			{
				continue;
			}
			occupied++;
			counts.merge(item.getId(), item.getQuantity(), Integer::sum);
			ShipwreckType type = ShipwreckType.fromSalvageItemId(item.getId());
			if (type != null)
			{
				salvage += item.getQuantity();
				types.add(type);
			}
		}
		session.inventoryChanged(occupied, holdSpace(items), salvage, types, tick);
		if (session.holdDepositPending())
		{
			for (int id : HoldItems.gone(lastInventory, counts))
			{
				boolean salvageItem = ShipwreckType.fromSalvageItemId(id) != null;
				if (!HoldWhitelist.TAKES_NO_SPACE.contains(id) && (salvageItem || stackable.contains(id)))
				{
					holdItemIds.add(id);
				}
			}
		}
		lastInventory = counts;
		lastInventoryItems = items;
	}

	/** The plugin's holdSpace: inventory slots whose items would take room in the hold if deposited. */
	private int holdSpace(Item[] items)
	{
		int space = 0;
		for (Item item : items)
		{
			int id = item.getId();
			if (id > -1 && item.getQuantity() > 0 && !HoldWhitelist.TAKES_NO_SPACE.contains(id) && !(holdItemIds.contains(id) && stackable.contains(id)))
			{
				space++;
			}
		}
		return space;
	}

	/** The hold actions of the plugin's onMenuOptionClicked. */
	private void click(Recording.Line line)
	{
		String option = line.has("option") ? line.text("option").toLowerCase(Locale.ROOT) : "";
		String target = line.has("target") ? line.text("target").toLowerCase(Locale.ROOT) : "";
		MenuAction action = menuAction(line.has("action") ? line.text("action") : "");
		session.playerClicked();
		if (action == MenuAction.CANCEL || option.startsWith("examine"))
		{
			return;
		}
		boolean objectOp = action == MenuAction.GAME_OBJECT_FIRST_OPTION || action == MenuAction.GAME_OBJECT_SECOND_OPTION
			|| action == MenuAction.GAME_OBJECT_THIRD_OPTION || action == MenuAction.GAME_OBJECT_FOURTH_OPTION
			|| action == MenuAction.GAME_OBJECT_FIFTH_OPTION;
		if (objectOp && (target.contains("crystal extractor") || BoatFacilities.isExtractor(line.number("id", -1))))
		{
			session.extractorUsed(line.tick());
			return;
		}
		if (option.startsWith("bank") && (option.contains("cargo") || target.contains("cargo")))
		{
			session.holdBanked();
			holdItemIds.clear();
			return;
		}
		boolean holdAction = option.contains("deposit") || option.startsWith("withdraw");
		if (holdAction && (target.contains("cargo hold") || holdOpen))
		{
			session.holdActionClicked(option.startsWith("withdraw"), holdOpen);
			return;
		}
		session.holdActionCanceled();
	}

	/** The plugin's onOverheadTextChanged: only NPCs aboard the player's own world view count. */
	private void npcSay(Recording.Line line)
	{
		if (playerView <= 0 || session.boat().getWorldViewId() != playerView || line.number("wv", -2) != playerView)
		{
			return;
		}
		String text = line.has("text") ? line.text("text") : "";
		if (CrewSpeech.reportsFullHold(text))
		{
			session.fullLine();
		}
		else if (CrewSpeech.reportsCrewSalvage(text))
		{
			session.crewLine(line.has("name") ? line.text("name") : "", line.tick());
		}
		else if (CrewSpeech.isGhostSpeech(text))
		{
			session.ghostLine(line.tick());
		}
	}

	/** The plugin's onChatMessage, for the lines that move the count. */
	private void chat(Recording.Line line)
	{
		String type = line.has("type") ? line.text("type") : "";
		if (!type.equals("GAMEMESSAGE") && !type.equals("SPAM") && !type.equals("ENGINE") && !type.equals("MESBOX"))
		{
			return;
		}
		String text = line.has("text") ? line.text("text") : "";
		if (CrewSpeech.reportsFullHold(text))
		{
			session.fullLine();
		}
		else if (CrewSpeech.reportsPlayerDeposit(text))
		{
			session.depositLine();
		}
	}

	private void tick(Recording.Line line)
	{
		boolean sailing = line.has("boatX");
		int view = line.number("wv", -1);
		playerView = view;
		if (sailing && view > 0)
		{
			followBoat(view);
		}
		AfkSession.TickInputs in = new AfkSession.TickInputs();
		in.tick = line.tick();
		in.now = now(line);
		in.sailing = sailing;
		in.ownBoat = sailing && line.number("boatOwner", -1) == WorldEntity.OWNER_TYPE_SELF_PLAYER;
		in.boatCrewCapacity = CREW_SLOT_VARBITS.length;
		in.animation = line.number("anim", -1);
		in.boostedSailingLevel = boostedLevel;
		in.realSailingLevel = realLevel;
		List<AfkSession.HookInput> hooks = new ArrayList<>();
		for (BoatFacilities.Hook hook : session.boat().hooks())
		{
			hooks.add(new AfkSession.HookInput(null, hook.getTier(), hook.isSecond()));
		}
		in.hooks = hooks;
		in.boatPoint = sailing ? point(line, "boatX", "boatY", "boatPlane") : null;
		in.playerPoint = line.has("topX") ? point(line, "topX", "topY", "topPlane") : point(line, "x", "y", "plane");
		in.cargoInterfaceOpen = holdOpen;
		session.tick(in);
	}

	private static WorldPoint point(Recording.Line line, String x, String y, String plane)
	{
		return new WorldPoint(line.number(x, 0), line.number(y, 0), line.number(plane, 0));
	}

	private static MenuAction menuAction(String name)
	{
		try
		{
			return MenuAction.valueOf(name);
		}
		catch (IllegalArgumentException e)
		{
			return MenuAction.UNKNOWN;
		}
	}

	private static Iterable<JsonElement> itemsOf(Recording.Line line)
	{
		if (!line.kind().equals("items") || !line.json().has("items"))
		{
			return Collections.emptyList();
		}
		return line.json().getAsJsonArray("items");
	}

	private static Item[] items(JsonArray array)
	{
		Item[] items = new Item[array.size()];
		for (int i = 0; i < array.size(); i++)
		{
			JsonArray pair = array.get(i).getAsJsonArray();
			items[i] = new Item(pair.get(0).getAsInt(), pair.get(1).getAsInt());
		}
		return items;
	}
}
