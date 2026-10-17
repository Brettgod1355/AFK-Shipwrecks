/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

/**
 * While a hook stands empty and a crewmate could take it, a flashing neon green box shows the next click
 * (owner, 2026-10-07): the Sailing tab, then the Facilities tab, then the Assign button of the empty
 * hook, then the idle crewmate with the most deckhandiness who can work that hook. A row scrolled out
 * of view gets its scrollbar boxed instead, and a crew list opened for some other facility gets its
 * Back button. Only boxes are drawn; every click is the player's.
 * <p>
 * The Facilities rows carry no hook names, so the empty hook's row is found by its pictures: the hook
 * icon on the left and, on its Assign button, the empty-place picture where a crewmate's face would
 * be (read from the client log, 2026-10-07).
 */
public class HookGuideOverlay extends Overlay
{
	/** The hook icon at the left of a Facilities row. */
	static final int HOOK_ICON = SpriteID.IconSailingFacilities24x24._5;
	/** The picture on an Assign button nobody fills. */
	static final int EMPTY_PLACE = SpriteID.IconSailingPlayer18x18._0;
	/** The hook's requirement in the crew list's title: "Rune salvaging hook (D3)". */
	private static final Pattern NEEDED = Pattern.compile("\\(D(\\d+)\\)");
	private static final int[] TAB_STONES = {
		InterfaceID.Toplevel.STONE0,
		InterfaceID.ToplevelOsrsStretch.STONE0,
		InterfaceID.ToplevelPreEoc.STONE0,
		InterfaceID.ToplevelOsm.STONE0,
	};
	private static final int FLASH_MILLIS = 400;

	private final Client client;
	private final AfkSalvagingPlugin plugin;
	private final AfkSalvagingConfig config;

	/** Whether the crew list showing was opened from the empty hook's Assign button. */
	private boolean listForEmptyHook;

	@Inject
	HookGuideOverlay(Client client, AfkSalvagingPlugin plugin, AfkSalvagingConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	/** The player clicked Assign in the Facilities list: remember whether it was an empty hook's. */
	void assignClicked(int childIndex)
	{
		listForEmptyHook = emptyHookButtons().stream().anyMatch(b -> b.getIndex() == childIndex);
	}

	void reset()
	{
		listForEmptyHook = false;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.hookGuide() || !plugin.isOwnBoat() || !plugin.getSession().view().assignGuide)
		{
			return null;
		}
		Rectangle target = target();
		if (target == null || (System.currentTimeMillis() / FLASH_MILLIS) % 2 == 1)
		{
			return null;
		}
		Color color = Palette.HOOK_GUIDE;
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
		graphics.fill(target);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
		graphics.setStroke(new BasicStroke(2));
		graphics.draw(new Rectangle(target.x - 1, target.y - 1, target.width + 2, target.height + 2));
		return null;
	}

	private Rectangle target()
	{
		if (!visible(InterfaceID.SailingSidepanel.UNIVERSE))
		{
			for (int stone : TAB_STONES)
			{
				if (visible(stone))
				{
					return client.getWidget(stone).getBounds();
				}
			}
			return null;
		}
		if (visible(InterfaceID.SailingSidepanel.CREW_ASSIGNATION_CONTAINER))
		{
			return crewStep();
		}
		if (!visible(InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CONTAINER))
		{
			return bounds(InterfaceID.SailingSidepanel.FACILITIES_TAB);
		}
		List<Widget> buttons = emptyHookButtons();
		return buttons.isEmpty() ? null
			: inView(buttons.get(0), InterfaceID.SailingSidepanel.FACILITIES_SCROLLABLE, InterfaceID.SailingSidepanel.FACILITIES_SCROLLBAR);
	}

	/** In the crew list: the best idle crewmate for this hook, or Back when the list is for something else. */
	private Rectangle crewStep()
	{
		Widget title = client.getWidget(InterfaceID.SailingSidepanel.TAB_TITLE);
		String heading = title == null ? "" : titleText(title);
		if (!listForEmptyHook || !heading.toLowerCase().contains("salvaging hook"))
		{
			return bounds(InterfaceID.SailingSidepanel.CREW_ASSIGNATION_BACK_BUTTON);
		}
		Crewmate best = plugin.getSession().bestIdleFor(needed(heading), client.getTickCount());
		Widget rows = client.getWidget(InterfaceID.SailingSidepanel.CREW_ASSIGNATION_ROWS);
		Widget clicks = client.getWidget(InterfaceID.SailingSidepanel.CREW_ASSIGNATION_CLICKLAYER);
		if (best == null || rows == null || clicks == null)
		{
			return null;
		}
		for (Widget row : children(rows))
		{
			if (best.getName().equalsIgnoreCase(rowName(row.getText())))
			{
				Widget button = buttonAt(clicks, row, "Assign");
				return button == null ? null
					: inView(button, InterfaceID.SailingSidepanel.CREW_ASSIGNATION_SCROLLABLE, InterfaceID.SailingSidepanel.CREW_ASSIGNATION_SCROLLBAR);
			}
		}
		return null;
	}

	/** The Assign buttons of hooks nobody is on, top to bottom. */
	private List<Widget> emptyHookButtons()
	{
		List<Widget> found = new ArrayList<>();
		Widget rows = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_ROWS);
		Widget clicks = client.getWidget(InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER);
		if (rows == null || clicks == null)
		{
			return found;
		}
		List<Widget> hookIcons = new ArrayList<>();
		for (Widget child : children(rows))
		{
			if (child.getSpriteId() == HOOK_ICON)
			{
				hookIcons.add(child);
			}
		}
		for (Widget place : children(rows))
		{
			if (place.getSpriteId() != EMPTY_PLACE)
			{
				continue;
			}
			for (Widget icon : hookIcons)
			{
				if (sameRow(icon, place))
				{
					Widget button = buttonAt(clicks, place, "Assign");
					if (button != null)
					{
						found.add(button);
					}
					break;
				}
			}
		}
		return found;
	}

	static boolean sameRow(Widget a, Widget b)
	{
		int ay = a.getRelativeY() + a.getHeight() / 2;
		int by = b.getRelativeY() + b.getHeight() / 2;
		return Math.abs(ay - by) <= 10;
	}

	/** The click layer's button with this option over the middle of a picture or text in the rows. */
	private static Widget buttonAt(Widget clicks, Widget over, String option)
	{
		int x = over.getRelativeX() + over.getWidth() / 2;
		int y = over.getRelativeY() + over.getHeight() / 2;
		for (Widget button : children(clicks))
		{
			if (hasOption(button, option) && x >= button.getRelativeX() && x < button.getRelativeX() + button.getWidth()
				&& y >= button.getRelativeY() && y < button.getRelativeY() + button.getHeight())
			{
				return button;
			}
		}
		return null;
	}

	private static boolean hasOption(Widget widget, String option)
	{
		String[] actions = widget.getActions();
		if (actions == null)
		{
			return false;
		}
		for (String action : actions)
		{
			if (option.equalsIgnoreCase(action))
			{
				return true;
			}
		}
		return false;
	}

	/** The button, or the list's scrollbar while the button is scrolled out of view. */
	private Rectangle inView(Widget button, int scrollable, int scrollbar)
	{
		Rectangle target = button.getBounds();
		Widget view = client.getWidget(scrollable);
		if (view == null || view.getBounds().contains(target))
		{
			return target;
		}
		return bounds(scrollbar);
	}

	/** "Ex-Captain Siad<br>H3 P2 D3" is Ex-Captain Siad. */
	static String rowName(String text)
	{
		if (text == null)
		{
			return "";
		}
		int br = text.indexOf("<br>");
		return Text.removeTags(br < 0 ? text : text.substring(0, br)).trim();
	}

	/** The deckhandiness a hook needs, from "Rune salvaging hook (D3)"; 1 when it does not say. */
	static int needed(String heading)
	{
		Matcher m = NEEDED.matcher(heading);
		return m.find() ? Integer.parseInt(m.group(1)) : 1;
	}

	private static String titleText(Widget title)
	{
		Widget[] children = title.getDynamicChildren();
		if (children != null && children.length > 0 && children[0] != null && children[0].getText() != null)
		{
			return children[0].getText();
		}
		return title.getText() == null ? "" : title.getText();
	}

	private static List<Widget> children(Widget parent)
	{
		List<Widget> list = new ArrayList<>();
		Widget[] children = parent.getDynamicChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				if (child != null && !child.isSelfHidden())
				{
					list.add(child);
				}
			}
		}
		return list;
	}

	private boolean visible(int id)
	{
		Widget widget = client.getWidget(id);
		return widget != null && !widget.isHidden();
	}

	private Rectangle bounds(int id)
	{
		Widget widget = client.getWidget(id);
		return widget == null || widget.isHidden() ? null : widget.getBounds();
	}
}
