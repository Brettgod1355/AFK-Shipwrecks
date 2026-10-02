/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * The sidebar: every salvaging hotspot, filtered and sorted from two dropdowns, each with buttons
 * to show it on the world map, route there with the Shortest Path plugin, pin it as a favourite
 * and mark it for the automatic route when boarding. Above the list: the nearest dock with its
 * own Map and Route, and the session totals. Below it: a test alert, forgetting the learned rates,
 * and the tips.
 * <p>
 * Swing only. Anything that touches the game or the settings goes back through {@link Actions} so
 * the plugin can run it on the client thread.
 */
public class SalvagingSpotPanel extends PluginPanel
{
	/** What the buttons and dropdowns do; the plugin provides it. */
	public interface Actions
	{
		void showOnMap(SalvagingSpot spot);

		void routeTo(SalvagingSpot spot);

		void clearRoute();

		void showDockOnMap(Mooring dock);

		void routeToDock(Mooring dock);

		/** The dropdown changed; the key is one of the {@link SpotList} filters or a wreck name. */
		void filterChanged(String filterKey);

		void sortChanged(boolean nearestFirst);

		void favouriteChanged(SalvagingSpot spot, boolean favourite);

		/** The spot marked for the automatic route, or null for none. */
		void autoRouteChanged(SalvagingSpot spot);

		void testAlert();

		void forgetLearnedRates();

		/** Add an item typed by name to one of the sorting lists. */
		void sortItemAdd(String name, SortRule rule);

		void sortItemRemove(int itemId);
	}

	/** The sidebar's colours, from the settings; see {@link #setPalette}. */
	private Color inLevel = new Color(70, 200, 110);
	private Color belowLevel = new Color(200, 110, 110);
	private Color marked = new Color(255, 200, 60);
	private Color good = new Color(120, 220, 120);
	private final Map<SortRule, Color> sortColours = new EnumMap<>(SortRule.class);
	private Map<SortRule, List<String[]>> lastSortLists = new EnumMap<>(SortRule.class);
	private SortDefaults lastSortDefaults = SortDefaults.NONE;
	/** Text widths that wrap inside the panel: the top block, and a row with its own padding. */
	private static final int TOP_TEXT_WIDTH = 190;
	private static final int ROW_TEXT_WIDTH = 172;
	private static final String[][] FILTERS = {
		{SpotList.FILTER_ALL, "All wrecks"},
		{SpotList.FILTER_MY_LEVEL, "Spots I can salvage"},
		{SpotList.FILTER_FAVOURITES, "Favourites"},
	};

	private final Actions actions;
	private final JComboBox<String> filter = new JComboBox<>();
	private final JComboBox<String> sort = new JComboBox<>();
	private final JPanel list = new JPanel();
	private final JLabel levelNote = new JLabel();
	private final JLabel status = new JLabel();
	private final JPanel dockBlock = new JPanel(new BorderLayout(0, 4));
	private final JLabel dockLabel = new JLabel();
	private final JLabel statsLabel = new JLabel();
	private final JPanel sortLists = new JPanel();
	private final JTextField sortName = new JTextField();
	private final JComboBox<String> sortRule = new JComboBox<>();
	private final List<String> filterKeys = new ArrayList<>();
	private final Set<SalvagingSpot> favourites = EnumSet.noneOf(SalvagingSpot.class);
	/** The distance label of each listed row, updated in place as the player moves. */
	private final List<SalvagingSpot> shown = new ArrayList<>();
	private final List<JLabel> shownDistance = new ArrayList<>();
	private int sailingLevel;
	private SalvagingSpot picked;
	private SalvagingSpot autoRoute;
	/** The nearest dock the player can use, and the nearest of all when that is a different one. */
	private Mooring dock;
	private Mooring nearestDock;
	private WorldPoint position;
	private boolean dockShownSetting = true;
	/** Set while the dropdowns are being put to a stored value, so that does not count as a choice. */
	private boolean loading;

	public SalvagingSpotPanel(Actions actions)
	{
		super();
		this.actions = actions;
		setLayout(new BorderLayout(0, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Salvaging spots");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(LEFT_ALIGNMENT);
		top.add(title);
		top.add(Box.createVerticalStrut(6));

		for (String[] choice : FILTERS)
		{
			filterKeys.add(choice[0]);
			filter.addItem(choice[1]);
		}
		for (ShipwreckType type : ShipwreckType.values())
		{
			filterKeys.add(type.name());
			filter.addItem(SalvagingSpot.salvageName(type) + "  (level " + type.getSailingLevel() + ")");
		}
		dropdown(filter, "Which spots to list.");
		filter.addActionListener(e ->
		{
			rebuild();
			if (!loading)
			{
				actions.filterChanged(filterKey());
			}
		});
		top.add(filter);
		top.add(Box.createVerticalStrut(4));

		sort.addItem("Sort: by wreck");
		sort.addItem("Sort: nearest first");
		dropdown(sort, "Nearest first orders the list by the distance from where you are.");
		sort.addActionListener(e ->
		{
			rebuild();
			if (!loading)
			{
				actions.sortChanged(nearestFirst());
			}
		});
		top.add(sort);
		top.add(Box.createVerticalStrut(6));

		small(levelNote);
		top.add(levelNote);
		top.add(Box.createVerticalStrut(4));

		JButton clear = button("Clear route", "Take down the Shortest Path route.");
		clear.addActionListener(e -> actions.clearRoute());
		top.add(clear);
		top.add(Box.createVerticalStrut(4));

		JLabel note = new JLabel(html("Map pans the world map while it is open; otherwise open it and it "
			+ "jumps there. Route draws the way there with the Shortest Path plugin.", TOP_TEXT_WIDTH));
		small(note);
		top.add(note);
		top.add(Box.createVerticalStrut(4));

		small(status);
		top.add(status);
		top.add(Box.createVerticalStrut(6));

		dockBlock.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		dockBlock.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		dockBlock.setAlignmentX(LEFT_ALIGNMENT);
		dockBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
		small(dockLabel);
		dockLabel.setForeground(Color.WHITE);
		dockBlock.add(dockLabel, BorderLayout.CENTER);
		JButton dockMap = button("Map", "Show the nearest dock on the world map.");
		dockMap.addActionListener(e -> withDock(actions::showDockOnMap));
		JButton dockRoute = button("Route", "Ask the Shortest Path plugin to draw a route to the nearest dock.");
		dockRoute.addActionListener(e -> withDock(actions::routeToDock));
		dockBlock.add(buttons(dockMap, dockRoute), BorderLayout.SOUTH);
		dockBlock.setVisible(false);
		top.add(dockBlock);
		top.add(Box.createVerticalStrut(6));

		small(statsLabel);
		top.add(statsLabel);

		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Three tabs: the spots and tools, inventory sorting, and the tips.
		JPanel spotsTab = new JPanel(new BorderLayout(0, 8));
		spotsTab.setBackground(ColorScheme.DARK_GRAY_COLOR);
		spotsTab.add(top, BorderLayout.NORTH);
		spotsTab.add(list, BorderLayout.CENTER);
		spotsTab.add(toolsBlock(), BorderLayout.SOUTH);

		JPanel display = new JPanel(new BorderLayout());
		display.setBackground(ColorScheme.DARK_GRAY_COLOR);
		MaterialTabGroup tabs = new MaterialTabGroup(display);
		tabs.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		MaterialTab spots = new MaterialTab("Spots", tabs, spotsTab);
		tabs.addTab(spots);
		tabs.addTab(new MaterialTab("Sorting", tabs, sortBlock()));
		tabs.addTab(new MaterialTab("Tips", tabs, tipsBlock()));
		tabs.select(spots);
		add(tabs, BorderLayout.NORTH);
		add(display, BorderLayout.CENTER);

		rebuild();
	}

	/** The tips, in their own tab. */
	private JPanel tipsBlock()
	{
		JPanel block = new JPanel();
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel body = new JLabel(SalvagingTips.html(TOP_TEXT_WIDTH));
		small(body);
		body.setAlignmentX(LEFT_ALIGNMENT);
		block.add(body);
		return block;
	}

	/** Test alert and Forget rates, under the spot list. */
	private JPanel toolsBlock()
	{
		JPanel block = new JPanel();
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setBackground(ColorScheme.DARK_GRAY_COLOR);
		block.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

		JButton test = button("Test alert", "Send the full-hold notification now, with the banner, so you can check "
			+ "the sound and popup you set up.");
		test.addActionListener(e -> actions.testAlert());
		JButton forget = button("Forget rates", "Throw away what the timer has learned about how fast your "
			+ "crew salvage each wreck. It starts again from the published tables.");
		forget.addActionListener(e -> actions.forgetLearnedRates());
		JPanel tools = buttons(test, forget);
		tools.setAlignmentX(LEFT_ALIGNMENT);
		tools.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		block.add(tools);
		return block;
	}

	/** The four sorting lists with a remove button per item, and a row to add one by name. */
	private JPanel sortBlock()
	{
		JPanel block = new JPanel();
		block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
		block.setBackground(ColorScheme.DARK_GRAY_COLOR);
		block.setAlignmentX(LEFT_ALIGNMENT);

		JLabel title = new JLabel("Inventory sorting");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		title.setAlignmentX(LEFT_ALIGNMENT);
		block.add(title);
		JLabel note = new JLabel(html("Boxes round inventory items on your boat at a salvaging spot: keep, hold, alch or "
			+ "drop. Unmarked items sort themselves by the defaults under each list; mark one by "
			+ "shift-right-clicking it, or type its name here.", TOP_TEXT_WIDTH));
		small(note);
		block.add(note);
		block.add(Box.createVerticalStrut(4));

		for (SortRule rule : SortRule.values())
		{
			sortRule.addItem(rule.getLabel());
		}
		dropdown(sortRule, "Which list the typed item goes in.");
		sortName.setFont(FontManager.getRunescapeSmallFont());
		sortName.setToolTipText("An item's name, as the game spells it.");
		sortName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		sortName.setAlignmentX(LEFT_ALIGNMENT);
		JButton add = button("Add", "Add the named item to the chosen list.");
		add.addActionListener(e ->
		{
			actions.sortItemAdd(sortName.getText(), SortRule.values()[Math.max(0, sortRule.getSelectedIndex())]);
			sortName.setText("");
		});
		sortName.addActionListener(add.getActionListeners()[0]);
		block.add(sortName);
		block.add(Box.createVerticalStrut(4));
		JPanel addRow = new JPanel(new BorderLayout(6, 0));
		addRow.setOpaque(false);
		addRow.add(sortRule, BorderLayout.CENTER);
		addRow.add(add, BorderLayout.EAST);
		addRow.setAlignmentX(LEFT_ALIGNMENT);
		addRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		block.add(addRow);
		block.add(Box.createVerticalStrut(8));

		sortLists.setLayout(new BoxLayout(sortLists, BoxLayout.Y_AXIS));
		sortLists.setBackground(ColorScheme.DARK_GRAY_COLOR);
		sortLists.setAlignmentX(LEFT_ALIGNMENT);
		block.add(sortLists);
		return block;
	}

	/** What each list does for items nobody marked, for showing under the marks. */
	public static final class SortDefaults
	{
		/** Names of the items the cargo hold is known to accept, sorted. */
		final List<String> holdNames;
		final int alchThreshold;
		final int geOverAlchPercent;

		public SortDefaults(List<String> holdNames, int alchThreshold, int geOverAlchPercent)
		{
			this.holdNames = holdNames;
			this.alchThreshold = alchThreshold;
			this.geOverAlchPercent = geOverAlchPercent;
		}

		static final SortDefaults NONE = new SortDefaults(new ArrayList<>(), 0, 0);
	}

	/** The default rule of one list, in words. */
	static String defaultLine(SortRule rule, SortDefaults defaults)
	{
		switch (rule)
		{
			case KEEP:
				return defaults.geOverAlchPercent > 0
					? "By default: items whose Grand Exchange price beats their alch value by "
						+ defaults.geOverAlchPercent + "% or more."
					: "By default: nothing; only what you mark.";
			case HOLD:
				return "By default: the " + defaults.holdNames.size() + " items the cargo hold is known to take. "
					+ "The list grows as the game answers each time you open the hold. Never noted items.";
			case ALCH:
				return "By default: tradeable items that alch for at least "
					+ String.format("%,d", defaults.alchThreshold) + " coins.";
			default:
				return "By default: every other tradeable item. Untradeable items get no box unless you mark them.";
		}
	}

	/**
	 * The sorting lists: the player's marks with item names (each row {id, name}), then each list's
	 * default. Safe to call from any thread.
	 */
	public void setSortLists(Map<SortRule, List<String[]>> lists, SortDefaults defaults)
	{
		SwingUtilities.invokeLater(() ->
		{
			lastSortLists = lists;
			lastSortDefaults = defaults;
			sortLists.removeAll();
			Map<SortRule, Color> colours = sortColours;
			for (SortRule rule : SortRule.values())
			{
				List<String[]> rows = lists.get(rule);
				int count = rows == null ? 0 : rows.size();
				JLabel heading = new JLabel(rule.getLabel() + (count == 0 ? ": nothing marked" : ": " + count + " marked"));
				heading.setFont(FontManager.getRunescapeBoldFont());
				heading.setForeground(colours.getOrDefault(rule, Color.WHITE));
				heading.setAlignmentX(LEFT_ALIGNMENT);
				sortLists.add(heading);
				if (rows != null)
				{
					for (String[] row : rows)
					{
						JPanel line = new JPanel(new BorderLayout(4, 0));
						line.setOpaque(false);
						line.setAlignmentX(LEFT_ALIGNMENT);
						line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
						JLabel name = new JLabel(row[1]);
						small(name);
						name.setForeground(Color.WHITE);
						JButton remove = button("×", "Take " + row[1] + " off the list.");
						remove.setMargin(new Insets(0, 4, 0, 4));
						int id = Integer.parseInt(row[0]);
						remove.addActionListener(e -> actions.sortItemRemove(id));
						line.add(name, BorderLayout.CENTER);
						line.add(remove, BorderLayout.EAST);
						sortLists.add(line);
					}
				}
				JLabel ruleText = new JLabel(html(defaultLine(rule, defaults), TOP_TEXT_WIDTH));
				small(ruleText);
				ruleText.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				ruleText.setAlignmentX(LEFT_ALIGNMENT);
				sortLists.add(ruleText);
				if (rule == SortRule.HOLD && !defaults.holdNames.isEmpty())
				{
					// Whole names separated by commas; the label wraps between words, never inside one.
					String joined = String.join(", ", defaults.holdNames).replace("&", "&amp;").replace("<", "&lt;");
					JLabel names = new JLabel(html(joined + ".", TOP_TEXT_WIDTH));
					small(names);
					names.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
					names.setAlignmentX(LEFT_ALIGNMENT);
					sortLists.add(names);
				}
				sortLists.add(Box.createVerticalStrut(8));
			}
			sortLists.revalidate();
			sortLists.repaint();
		});
	}

	// ---- What the plugin tells the panel; all safe to call from any thread ----

	/**
	 * The colours from the settings: the level within and above reach, the marks, the good status
	 * line, and the four sorting lists. Transparency is dropped; the sidebar is opaque.
	 */
	public void setPalette(Color within, Color above, Color marks, Color goodStatus, Map<SortRule, Color> sorting)
	{
		SwingUtilities.invokeLater(() ->
		{
			inLevel = opaque(within);
			belowLevel = opaque(above);
			marked = opaque(marks);
			good = opaque(goodStatus);
			sortColours.clear();
			for (Map.Entry<SortRule, Color> entry : sorting.entrySet())
			{
				sortColours.put(entry.getKey(), opaque(entry.getValue()));
			}
			rebuild();
			setSortLists(lastSortLists, lastSortDefaults);
		});
	}

	private static Color opaque(Color colour)
	{
		return new Color(colour.getRed(), colour.getGreen(), colour.getBlue());
	}

	/** Recolours the levels for the player's Sailing level. */
	public void setSailingLevel(int level)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (level != sailingLevel)
			{
				sailingLevel = level;
				rebuild();
			}
		});
	}

	/**
	 * Shows what happened after a button press, or why it could not: in red for a problem such as
	 * Shortest Path not being installed. Null clears it.
	 */
	public void setStatus(String text, boolean problem)
	{
		SwingUtilities.invokeLater(() ->
		{
			status.setText(text == null ? "" : html(text, TOP_TEXT_WIDTH));
			status.setForeground(problem ? belowLevel : good);
		});
	}

	/** Highlights the spot last sent to the map or routed to. */
	public void setPicked(SalvagingSpot spot)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (spot != picked)
			{
				picked = spot;
				rebuild();
			}
		});
	}

	/** Puts the dropdowns to the remembered choices without reporting them back as new choices. */
	public void setChoices(String filterKey, boolean nearestFirst)
	{
		SwingUtilities.invokeLater(() ->
		{
			loading = true;
			try
			{
				int index = filterKeys.indexOf(SpotList.validFilter(filterKey));
				filter.setSelectedIndex(Math.max(0, index));
				sort.setSelectedIndex(nearestFirst ? 1 : 0);
			}
			finally
			{
				loading = false;
			}
		});
	}

	public void setFavourites(Set<SalvagingSpot> spots)
	{
		SwingUtilities.invokeLater(() ->
		{
			favourites.clear();
			favourites.addAll(spots);
			rebuild();
		});
	}

	/** The spot marked for the automatic route, or null. */
	public void setAutoRoute(SalvagingSpot spot)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (spot != autoRoute)
			{
				autoRoute = spot;
				rebuild();
			}
		});
	}

	/**
	 * Where the player is in the top-level world, or null when unknown. Distances are updated in
	 * place; the list is only rebuilt when the order changes.
	 */
	public void setPosition(WorldPoint point)
	{
		SwingUtilities.invokeLater(() ->
		{
			position = point;
			refreshDock();
			if (nearestFirst() && !arranged().equals(shown))
			{
				rebuild();
			}
			else
			{
				refreshDistances();
			}
		});
	}

	/**
	 * The nearest dock the player can disembark at (null when none), and the nearest dock of all,
	 * which is named with what it needs when it is a different one. Safe to call from any thread.
	 */
	public void setDock(Mooring usable, Mooring nearest)
	{
		SwingUtilities.invokeLater(() ->
		{
			dock = usable;
			nearestDock = nearest;
			refreshDock();
		});
	}

	/** Whether the nearest dock block is shown at all (a setting). */
	public void setDockShown(boolean shown)
	{
		SwingUtilities.invokeLater(() ->
		{
			dockShownSetting = shown;
			dockBlock.setVisible(shown && (dock != null || nearestDock != null) && position != null);
			revalidate();
		});
	}

	/** The session line, already worded; empty hides it. */
	public void setStats(String text)
	{
		SwingUtilities.invokeLater(() -> statsLabel.setText(text == null || text.isEmpty() ? "" : html(text, TOP_TEXT_WIDTH)));
	}

	// ---- Internals ----

	private String filterKey()
	{
		int index = filter.getSelectedIndex();
		return index < 0 ? SpotList.FILTER_ALL : filterKeys.get(index);
	}

	private boolean nearestFirst()
	{
		return sort.getSelectedIndex() == 1;
	}

	private List<SalvagingSpot> arranged()
	{
		return SpotList.arrange(filterKey(), sailingLevel, favourites, nearestFirst(), position);
	}

	private void withDock(Consumer<Mooring> action)
	{
		if (dock != null)
		{
			action.accept(dock);
		}
	}

	private void refreshDock()
	{
		if (position == null || (dock == null && nearestDock == null))
		{
			dockLabel.setText("");
			return;
		}
		StringBuilder text = new StringBuilder();
		if (dock != null)
		{
			text.append("<b>Nearest dock you can use:</b> ").append(dock.getDisplayName())
				.append("<br>").append(tiles(dock.tilesFrom(position))).append(" from you");
		}
		else
		{
			text.append("<b>No dock you can use yet.</b>");
		}
		if (nearestDock != null && nearestDock != dock)
		{
			text.append("<br><i>Nearer but not yet: ").append(nearestDock.getDisplayName()).append(", ")
				.append(tiles(nearestDock.tilesFrom(position))).append(": needs ")
				.append(nearestDock.requirementText()).append(".</i>");
		}
		dockLabel.setText(html(text.toString(), ROW_TEXT_WIDTH));
		if (!dockBlock.isVisible() && dockShownSetting)
		{
			dockBlock.setVisible(true);
			revalidate();
		}
	}

	private void refreshDistances()
	{
		for (int i = 0; i < shown.size(); i++)
		{
			shownDistance.get(i).setText(distanceText(shown.get(i)));
		}
	}

	private String distanceText(SalvagingSpot spot)
	{
		return position == null ? "" : tiles(Mooring.distance(position, spot.getPoint())) + " from you";
	}

	private static String tiles(int tiles)
	{
		return String.format("%,d tiles", tiles);
	}

	private void rebuild()
	{
		list.removeAll();
		shown.clear();
		shownDistance.clear();
		levelNote.setText(html(sailingLevel > 0 ? "Your Sailing level: " + sailingLevel
			: "Log in to see which spots you can salvage.", TOP_TEXT_WIDTH));
		for (SalvagingSpot spot : arranged())
		{
			list.add(row(spot));
			list.add(Box.createVerticalStrut(6));
			shown.add(spot);
		}
		if (shown.isEmpty())
		{
			String why;
			if (SpotList.FILTER_FAVOURITES.equals(filterKey()))
			{
				why = "No favourites yet. Press the star on a spot to pin it here and to the top of every list.";
			}
			else if (sailingLevel > 0)
			{
				why = "No spot is within your level yet. Small salvage opens at level 15.";
			}
			else
			{
				why = "Log in, and the spots within your level are listed here.";
			}
			JLabel none = new JLabel(html(why, TOP_TEXT_WIDTH));
			small(none);
			list.add(none);
		}
		list.revalidate();
		list.repaint();
	}

	/**
	 * Wraps text to a fixed width in pixels: a Swing label only wraps HTML when the body has a
	 * width. Swing's stylesheet scales a CSS "px" by 1.3 and a "pt" by 1, so the width is given in
	 * points to come out as pixels.
	 */
	static String html(String text, int width)
	{
		return "<html><body style='width:" + width + "pt'>" + text + "</body></html>";
	}

	private JPanel row(SalvagingSpot spot)
	{
		boolean canSalvage = sailingLevel >= spot.getSailingLevel();
		boolean favourite = favourites.contains(spot);
		JPanel row = new JPanel(new BorderLayout(0, 4));
		row.setBackground(spot == picked ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		JLabel name = new JLabel((favourite ? "★ " : "") + spot.getSalvageName());
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(Color.WHITE);

		JLabel level = new JLabel("Level " + spot.getSailingLevel());
		level.setFont(FontManager.getRunescapeSmallFont());
		level.setForeground(sailingLevel > 0 ? (canSalvage ? inLevel : belowLevel) : ColorScheme.LIGHT_GRAY_COLOR);

		JPanel heading = new JPanel(new BorderLayout());
		heading.setOpaque(false);
		heading.setAlignmentX(LEFT_ALIGNMENT);
		heading.add(name, BorderLayout.WEST);
		heading.add(level, BorderLayout.EAST);

		JLabel where = new JLabel(html(spot.getWhere(), ROW_TEXT_WIDTH));
		small(where);

		JLabel distance = new JLabel(distanceText(spot));
		small(distance);
		shownDistance.add(distance);

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		text.add(heading);
		text.add(where);
		text.add(distance);
		if (spot == autoRoute)
		{
			JLabel auto = new JLabel("Auto route when you board");
			small(auto);
			auto.setForeground(marked);
			text.add(auto);
		}

		JButton map = button("Map", "Show this spot on the world map.");
		map.addActionListener(e -> actions.showOnMap(spot));
		JButton route = button("Route", "Ask the Shortest Path plugin to draw a route here.");
		route.addActionListener(e -> actions.routeTo(spot));
		JButton star = button(favourite ? "★" : "☆", favourite ? "Unpin this spot." : "Pin this spot to the top of every list.");
		if (favourite)
		{
			star.setForeground(marked);
		}
		star.addActionListener(e ->
		{
			boolean now = !favourites.contains(spot);
			if (now)
			{
				favourites.add(spot);
			}
			else
			{
				favourites.remove(spot);
			}
			rebuild();
			actions.favouriteChanged(spot, now);
		});
		JButton auto = button("Auto", spot == autoRoute ? "Stop routing here automatically when you board."
			: "Route here automatically whenever you board your boat from a dock. Only one spot can be marked.");
		if (spot == autoRoute)
		{
			auto.setForeground(marked);
		}
		auto.addActionListener(e ->
		{
			autoRoute = spot == autoRoute ? null : spot;
			rebuild();
			actions.autoRouteChanged(autoRoute);
		});

		JPanel buttons = new JPanel(new GridLayout(1, 4, 4, 0));
		buttons.setOpaque(false);
		buttons.add(map);
		buttons.add(route);
		buttons.add(star);
		buttons.add(auto);

		row.add(text, BorderLayout.CENTER);
		row.add(buttons, BorderLayout.SOUTH);
		return row;
	}

	private static JPanel buttons(JButton left, JButton right)
	{
		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setOpaque(false);
		buttons.add(left);
		buttons.add(right);
		return buttons;
	}

	private static JButton button(String text, String tooltip)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		// The look and feel pads buttons for a wide panel; four across a row need less.
		button.setMargin(new Insets(2, 4, 2, 4));
		button.setToolTipText(tooltip);
		button.setAlignmentX(LEFT_ALIGNMENT);
		return button;
	}

	private static void small(JLabel label)
	{
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(LEFT_ALIGNMENT);
	}

	private static void dropdown(JComboBox<String> box, String tooltip)
	{
		box.setFont(FontManager.getRunescapeSmallFont());
		box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		box.setAlignmentX(LEFT_ALIGNMENT);
		box.setToolTipText(tooltip);
	}
}
