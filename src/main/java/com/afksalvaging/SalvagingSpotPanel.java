/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar: every salvaging hotspot, filtered by wreck, each with a button to show it on the
 * world map and one to route there with the Shortest Path plugin.
 * <p>
 * Swing only. Anything that touches the game goes back through {@link Actions} so the plugin can
 * run it on the client thread.
 */
public class SalvagingSpotPanel extends PluginPanel
{
	/** What the buttons do; the plugin provides it. */
	public interface Actions
	{
		void showOnMap(SalvagingSpot spot);

		void routeTo(SalvagingSpot spot);

		void clearRoute();
	}

	private static final Color IN_LEVEL = new Color(70, 200, 110);
	private static final Color BELOW_LEVEL = new Color(200, 110, 110);
	private static final String ALL_WRECKS = "All wrecks";
	private static final String MY_LEVEL = "Spots I can salvage";
	/** Text widths that wrap inside the panel: the top block, and a row with its own padding. */
	private static final int TOP_TEXT_WIDTH = 190;
	private static final int ROW_TEXT_WIDTH = 172;

	private final Actions actions;
	private final JComboBox<String> wreckFilter = new JComboBox<>();
	private final JPanel list = new JPanel();
	private final JLabel levelNote = new JLabel();
	private final JLabel status = new JLabel();
	private int sailingLevel;
	private SalvagingSpot picked;

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

		wreckFilter.addItem(ALL_WRECKS);
		wreckFilter.addItem(MY_LEVEL);
		for (ShipwreckType type : ShipwreckType.values())
		{
			wreckFilter.addItem(SalvagingSpot.salvageName(type) + "  (level " + type.getSailingLevel() + ")");
		}
		wreckFilter.setFont(FontManager.getRunescapeSmallFont());
		wreckFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		wreckFilter.addActionListener(e -> rebuild());
		wreckFilter.setAlignmentX(LEFT_ALIGNMENT);
		top.add(wreckFilter);
		top.add(Box.createVerticalStrut(6));

		levelNote.setFont(FontManager.getRunescapeSmallFont());
		levelNote.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		levelNote.setAlignmentX(LEFT_ALIGNMENT);
		top.add(levelNote);
		top.add(Box.createVerticalStrut(4));

		JButton clear = new JButton("Clear route");
		clear.setFont(FontManager.getRunescapeSmallFont());
		clear.setToolTipText("Take down the Shortest Path route.");
		clear.addActionListener(e -> actions.clearRoute());
		clear.setAlignmentX(LEFT_ALIGNMENT);
		top.add(clear);
		top.add(Box.createVerticalStrut(4));

		JLabel note = new JLabel(html("Map pans the world map while it is open; otherwise open it and it "
			+ "jumps there. Route draws the way there with the Shortest Path plugin.", TOP_TEXT_WIDTH));
		note.setFont(FontManager.getRunescapeSmallFont());
		note.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		note.setAlignmentX(LEFT_ALIGNMENT);
		top.add(note);
		top.add(Box.createVerticalStrut(4));

		status.setFont(FontManager.getRunescapeSmallFont());
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setAlignmentX(LEFT_ALIGNMENT);
		top.add(status);

		add(top, BorderLayout.NORTH);

		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(list, BorderLayout.CENTER);

		add(tipsBlock(), BorderLayout.SOUTH);

		rebuild();
	}

	/** "Tips" at the bottom: closed by default, opens to the things worth knowing. */
	private JPanel tipsBlock()
	{
		JPanel block = new JPanel(new BorderLayout(0, 4));
		block.setBackground(ColorScheme.DARK_GRAY_COLOR);
		block.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

		JLabel body = new JLabel(SalvagingTips.html(TOP_TEXT_WIDTH));
		body.setFont(FontManager.getRunescapeSmallFont());
		body.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		body.setVisible(false);

		JButton toggle = new JButton("Show tips");
		toggle.setFont(FontManager.getRunescapeSmallFont());
		toggle.setToolTipText("How the boxes and the buttons work.");
		toggle.addActionListener(e ->
		{
			body.setVisible(!body.isVisible());
			toggle.setText(body.isVisible() ? "Hide tips" : "Show tips");
			block.revalidate();
			block.repaint();
		});

		block.add(toggle, BorderLayout.NORTH);
		block.add(body, BorderLayout.CENTER);
		return block;
	}

	/** Recolours the levels for the player's Sailing level. Safe to call from any thread. */
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
	 * Shortest Path not being installed. Null clears it. Safe to call from any thread.
	 */
	public void setStatus(String text, boolean problem)
	{
		SwingUtilities.invokeLater(() ->
		{
			status.setText(text == null ? "" : html(text, TOP_TEXT_WIDTH));
			status.setForeground(problem ? BELOW_LEVEL : IN_LEVEL);
		});
	}

	/** Highlights the spot last sent to the map or routed to. Safe to call from any thread. */
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

	/** The wreck kind picked in the dropdown, or null for "all" and "spots I can salvage". */
	private ShipwreckType selectedWreck()
	{
		int index = wreckFilter.getSelectedIndex();
		return index <= 1 ? null : ShipwreckType.values()[index - 2];
	}

	private boolean onlyMyLevel()
	{
		return wreckFilter.getSelectedIndex() == 1;
	}

	private void rebuild()
	{
		list.removeAll();
		levelNote.setText(sailingLevel > 0 ? "Your Sailing level: " + sailingLevel : "Log in to see which spots you can salvage.");
		List<SalvagingSpot> spots = SalvagingSpot.forWreck(selectedWreck());
		int shown = 0;
		for (SalvagingSpot spot : spots)
		{
			if (onlyMyLevel() && sailingLevel < spot.getSailingLevel())
			{
				continue;
			}
			list.add(row(spot));
			list.add(Box.createVerticalStrut(6));
			shown++;
		}
		if (shown == 0)
		{
			JLabel none = new JLabel(html(sailingLevel > 0
				? "No spot is within your level yet. Small salvage opens at level 15."
				: "Log in, and the spots within your level are listed here.", TOP_TEXT_WIDTH));
			none.setFont(FontManager.getRunescapeSmallFont());
			none.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			none.setAlignmentX(LEFT_ALIGNMENT);
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
		JPanel row = new JPanel(new BorderLayout(0, 4));
		row.setBackground(spot == picked ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

		JLabel name = new JLabel(spot.getSalvageName());
		name.setFont(FontManager.getRunescapeBoldFont());
		name.setForeground(Color.WHITE);

		JLabel level = new JLabel("Level " + spot.getSailingLevel());
		level.setFont(FontManager.getRunescapeSmallFont());
		level.setForeground(sailingLevel > 0 ? (canSalvage ? IN_LEVEL : BELOW_LEVEL) : ColorScheme.LIGHT_GRAY_COLOR);

		JPanel heading = new JPanel(new BorderLayout());
		heading.setOpaque(false);
		heading.setAlignmentX(LEFT_ALIGNMENT);
		heading.add(name, BorderLayout.WEST);
		heading.add(level, BorderLayout.EAST);

		JLabel where = new JLabel(html(spot.getWhere(), ROW_TEXT_WIDTH));
		where.setFont(FontManager.getRunescapeSmallFont());
		where.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		where.setAlignmentX(LEFT_ALIGNMENT);

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		text.add(heading);
		text.add(where);

		JButton map = new JButton("Map");
		map.setFont(FontManager.getRunescapeSmallFont());
		map.setToolTipText("Show this spot on the world map.");
		map.addActionListener(e -> actions.showOnMap(spot));

		JButton route = new JButton("Route");
		route.setFont(FontManager.getRunescapeSmallFont());
		route.setToolTipText("Ask the Shortest Path plugin to draw a route here.");
		route.addActionListener(e -> actions.routeTo(spot));

		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setOpaque(false);
		buttons.add(map);
		buttons.add(route);

		JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		buttonsRow.setOpaque(false);
		buttonsRow.add(buttons);

		row.add(text, BorderLayout.CENTER);
		row.add(buttonsRow, BorderLayout.SOUTH);
		return row;
	}
}
