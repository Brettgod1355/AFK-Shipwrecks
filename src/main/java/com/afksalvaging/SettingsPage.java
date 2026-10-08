/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.config.Notification;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.Text;

/**
 * The plugin's settings inside its own sidebar, behind the cog beside the title (owner, 2026-10-08):
 * the same settings as RuneLite's panel, in folding sections, with a search box that shows only the
 * matching settings. Every change is written to RuneLite's settings at once, and a change made in
 * RuneLite's panel shows here too, so the two never disagree.
 * <p>
 * Notifications are an on/off here; their sound, popup and the rest stay in RuneLite's panel, where
 * the cog beside each notification setting opens them.
 * <p>
 * Resetting mirrors RuneLite's too (owner, 2026-10-08: "let's add a reset option"): right-click a
 * setting for Reset, or Reset every setting at the bottom after a yes. Unlike RuneLite's, that one
 * leaves the hidden values alone.
 */
final class SettingsPage extends JPanel
{
	/** What the page reads and writes; the plugin provides it. */
	interface Store
	{
		Object get(SettingsRows.Row row);

		void set(SettingsRows.Row row, Object value);

		/** Opens RuneLite's colour picker by {@code near}, reporting the colour chosen. */
		void pickColour(Component near, SettingsRows.Row row, Color current, Consumer<Color> chosen);

		/** Puts these settings back to their defaults, as RuneLite's Reset does. */
		void reset(List<SettingsRows.Row> rows);

		/** Asks a yes or no question by {@code near}; true for yes. */
		boolean confirm(Component near, String question);

		/** The Back button: show the Spots, Sorting, Tips and Feedback tabs again. */
		void back();
	}

	private static final int TEXT_WIDTH = 132;
	private static final int WIDE_TEXT_WIDTH = 178;

	private final Store store;
	private final JTextField query = new JTextField();
	private final JPanel sectionsPanel = new JPanel();
	private final JLabel noMatch = new JLabel();
	/** Per setting, how to show a value that changed elsewhere. */
	private final Map<String, Consumer<Object>> showers = new HashMap<>();
	private final Map<String, SettingsRows.Row> byKey = new HashMap<>();
	private final List<Section> sections = new ArrayList<>();
	private final List<SettingsRows.Row> rows;
	/** Set while values are being shown, so showing one does not write it back. */
	private boolean showing;

	SettingsPage(List<SettingsRows.Row> rows, Store store)
	{
		this.store = store;
		this.rows = rows;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel header = new JPanel(new BorderLayout());
		header.setOpaque(false);
		header.setAlignmentX(LEFT_ALIGNMENT);
		JLabel title = new JLabel("Settings");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		JButton back = new JButton("Back");
		back.setFont(FontManager.getRunescapeSmallFont());
		back.setToolTipText("Back to the spots, sorting, tips and feedback");
		back.addActionListener(e -> store.back());
		header.add(title, BorderLayout.WEST);
		header.add(back, BorderLayout.EAST);
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		add(header);
		add(Box.createVerticalStrut(6));

		JLabel searchLabel = new JLabel("Search settings");
		small(searchLabel);
		add(searchLabel);
		add(Box.createVerticalStrut(2));
		query.setFont(FontManager.getRunescapeSmallFont());
		query.setToolTipText("Type a word or two, such as banner, sound or colour.");
		query.setAlignmentX(LEFT_ALIGNMENT);
		query.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		query.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				filter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				filter();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				filter();
			}
		});
		add(query);
		add(Box.createVerticalStrut(8));

		noMatch.setText(SalvagingSpotPanel.html("No setting matches that.", WIDE_TEXT_WIDTH));
		small(noMatch);
		noMatch.setVisible(false);
		add(noMatch);

		sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));
		sectionsPanel.setOpaque(false);
		sectionsPanel.setAlignmentX(LEFT_ALIGNMENT);
		for (Map.Entry<String, List<SettingsRows.Row>> group : SettingsRows.bySection(rows).entrySet())
		{
			Section section = new Section(group.getKey(), group.getValue());
			sections.add(section);
			sectionsPanel.add(section);
			sectionsPanel.add(Box.createVerticalStrut(4));
		}
		add(sectionsPanel);
		add(Box.createVerticalStrut(8));
		JButton resetAll = new JButton("Reset every setting");
		resetAll.setFont(FontManager.getRunescapeSmallFont());
		resetAll.setAlignmentX(LEFT_ALIGNMENT);
		resetAll.setToolTipText("Puts every setting on this page back to its default, after a yes. "
			+ "Your sorting lists and what the plugin learned about the hold are kept.");
		resetAll.addActionListener(e ->
		{
			if (store.confirm(resetAll, "Put every AFK Shipwrecks setting back to its default?"))
			{
				reset(this.rows);
			}
		});
		add(resetAll);
		for (SettingsRows.Row row : rows)
		{
			byKey.put(row.key, row);
		}
		showAll();
		filter();
	}

	/** Reads one setting again from RuneLite's settings and shows it, after it changed elsewhere. */
	void refresh(String key)
	{
		SettingsRows.Row row = byKey.get(key);
		if (row != null)
		{
			show(key, store.get(row));
		}
	}

	/** Puts these settings back to their defaults and shows them. */
	private void reset(List<SettingsRows.Row> which)
	{
		store.reset(which);
		for (SettingsRows.Row row : which)
		{
			refresh(row.key);
		}
	}

	/** Shows every setting's current value, read again from RuneLite's settings. */
	void showAll()
	{
		for (Section section : sections)
		{
			for (SettingsRows.Row row : section.rows)
			{
				show(row.key, store.get(row));
			}
		}
	}

	/** Shows a value that changed elsewhere, such as in RuneLite's own panel. */
	void show(String key, Object value)
	{
		Consumer<Object> shower = showers.get(key);
		if (shower == null)
		{
			return;
		}
		showing = true;
		try
		{
			shower.accept(value);
		}
		finally
		{
			showing = false;
		}
	}

	/** Whether this page shows the setting under that key. */
	boolean shows(String key)
	{
		return showers.containsKey(key);
	}

	/** Empties the search box, so the page opens on its folded sections. */
	void clearSearch()
	{
		query.setText("");
	}

	/** Shows only the settings matching the search box; with it empty, the sections fold as left. */
	private void filter()
	{
		List<String> words = SettingsRows.words(query.getText());
		boolean any = false;
		for (Section section : sections)
		{
			any |= section.filter(words);
		}
		noMatch.setVisible(!words.isEmpty() && !any);
		revalidate();
		repaint();
	}

	/** A section heading that folds its settings away, as RuneLite's panel does. */
	private final class Section extends JPanel
	{
		final List<SettingsRows.Row> rows;
		private final String name;
		private final JLabel heading = new JLabel();
		private final JPanel body = new JPanel();
		private final List<JPanel> rowPanels = new ArrayList<>();
		/** Open or folded as the player left it, apart from a search. */
		private boolean open;

		Section(String name, List<SettingsRows.Row> rows)
		{
			this.name = name;
			this.rows = rows;
			setLayout(new BorderLayout(0, 4));
			setOpaque(false);
			setAlignmentX(LEFT_ALIGNMENT);
			heading.setFont(FontManager.getRunescapeBoldFont());
			heading.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			heading.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			heading.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR));
			heading.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					open = !open;
					SettingsPage.this.filter();
				}

				@Override
				public void mouseEntered(MouseEvent e)
				{
					heading.setForeground(Color.WHITE);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					heading.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				}
			});
			body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
			body.setOpaque(false);
			for (SettingsRows.Row row : rows)
			{
				JPanel panel = rowFor(row);
				rowPanels.add(panel);
				body.add(panel);
			}
			add(heading, BorderLayout.NORTH);
			add(body, BorderLayout.CENTER);
		}

		/** Applies the search; returns whether any of this section's settings match it. */
		boolean filter(List<String> words)
		{
			boolean searching = !words.isEmpty();
			int matching = 0;
			for (int i = 0; i < rows.size(); i++)
			{
				boolean match = !searching || SettingsRows.matches(rows.get(i), words);
				rowPanels.get(i).setVisible(match);
				matching += match ? 1 : 0;
			}
			setVisible(!searching || matching > 0);
			boolean shown = searching ? matching > 0 : open;
			body.setVisible(shown);
			heading.setText((shown ? "- " : "+ ") + name + (searching ? " (" + matching + ")" : ""));
			heading.setToolTipText(shown ? "Fold " + name + " away" : "Show " + name);
			return matching > 0;
		}

		@Override
		public Dimension getMaximumSize()
		{
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}
	}

	/** One setting: its name and its control, the description as the tooltip. */
	private JPanel rowFor(SettingsRows.Row row)
	{
		JPanel panel = new JPanel(new BorderLayout(6, 2))
		{
			@Override
			public Dimension getMaximumSize()
			{
				return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
			}
		};
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		panel.setAlignmentX(LEFT_ALIGNMENT);
		String tooltip = SalvagingSpotPanel.html((row.description.isEmpty() ? "" : row.description + " ")
			+ "Right-click to reset it.", 200);
		panel.setToolTipText(tooltip);
		// Right-click a setting for Reset, as in RuneLite's panel.
		JPopupMenu menu = new JPopupMenu();
		JMenuItem resetItem = new JMenuItem("Reset");
		resetItem.addActionListener(e -> reset(Collections.singletonList(row)));
		menu.add(resetItem);
		panel.setComponentPopupMenu(menu);

		JLabel name = new JLabel();
		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(Color.WHITE);
		name.setToolTipText(tooltip);
		name.setInheritsPopupMenu(true);
		String label = row.name + (row.units.isEmpty() ? "" : " (" + row.units.trim() + ")");

		switch (row.kind)
		{
			case TOGGLE:
			case NOTIFICATION:
			{
				JCheckBox box = new JCheckBox();
				box.setOpaque(false);
				box.setToolTipText(row.kind == SettingsRows.Kind.NOTIFICATION
					? "On or off here; the sound and the rest are in RuneLite's settings, behind this setting's cog"
					: tooltip);
				box.addActionListener(e ->
				{
					if (showing)
					{
						return;
					}
					if (row.kind == SettingsRows.Kind.NOTIFICATION)
					{
						Object current = store.get(row);
						Notification base = current instanceof Notification ? (Notification) current : Notification.OFF;
						store.set(row, base.withEnabled(box.isSelected()));
					}
					else
					{
						store.set(row, box.isSelected());
					}
				});
				showers.put(row.key, value -> box.setSelected(value instanceof Notification
					? ((Notification) value).isEnabled() : Boolean.TRUE.equals(value)));
				name.setText(SalvagingSpotPanel.html(label, TEXT_WIDTH));
				panel.add(name, BorderLayout.CENTER);
				panel.add(box, BorderLayout.EAST);
				break;
			}
			case NUMBER:
			{
				SpinnerNumberModel model = new SpinnerNumberModel(row.min, row.min, row.max, 1);
				JSpinner spinner = new JSpinner(model);
				spinner.setFont(FontManager.getRunescapeSmallFont());
				spinner.setPreferredSize(new Dimension(62, 22));
				spinner.setToolTipText(tooltip);
				spinner.addChangeListener(e ->
				{
					if (!showing)
					{
						store.set(row, model.getNumber().intValue());
					}
				});
				showers.put(row.key, value ->
				{
					if (value instanceof Integer)
					{
						model.setValue(Math.max(row.min, Math.min(row.max, (Integer) value)));
					}
				});
				name.setText(SalvagingSpotPanel.html(label, TEXT_WIDTH - 20));
				panel.add(name, BorderLayout.CENTER);
				panel.add(spinner, BorderLayout.EAST);
				break;
			}
			case TEXT:
			{
				JTextField field = new JTextField();
				field.setFont(FontManager.getRunescapeSmallFont());
				field.setToolTipText(tooltip);
				// Written when the player is done: Enter, or clicking elsewhere.
				Runnable commit = () ->
				{
					if (!showing)
					{
						store.set(row, field.getText());
					}
				};
				field.addActionListener(e -> commit.run());
				field.addFocusListener(new FocusAdapter()
				{
					@Override
					public void focusLost(FocusEvent e)
					{
						commit.run();
					}
				});
				showers.put(row.key, value -> field.setText(value == null ? "" : value.toString()));
				name.setText(SalvagingSpotPanel.html(label, WIDE_TEXT_WIDTH));
				panel.add(name, BorderLayout.NORTH);
				panel.add(field, BorderLayout.CENTER);
				break;
			}
			case CHOICE:
			{
				JComboBox<Object> combo = new JComboBox<>(row.options);
				combo.setFont(FontManager.getRunescapeSmallFont());
				// The words RuneLite's own panel shows for a choice: its name made readable, or its own wording.
				combo.setRenderer(new DefaultListCellRenderer()
				{
					@Override
					public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
						boolean focused)
					{
						Object shown = value instanceof Enum ? Text.titleCase((Enum<?>) value) : value;
						return super.getListCellRendererComponent(list, shown, index, selected, focused);
					}
				});
				combo.setToolTipText(tooltip);
				combo.addActionListener(e ->
				{
					if (!showing && combo.getSelectedItem() != null)
					{
						store.set(row, combo.getSelectedItem());
					}
				});
				showers.put(row.key, combo::setSelectedItem);
				name.setText(SalvagingSpotPanel.html(label, WIDE_TEXT_WIDTH));
				panel.add(name, BorderLayout.NORTH);
				panel.add(combo, BorderLayout.CENTER);
				break;
			}
			case COLOUR:
			{
				JButton swatch = new JButton();
				swatch.setPreferredSize(new Dimension(40, 20));
				swatch.setFocusPainted(false);
				swatch.setToolTipText("Pick a colour");
				Color[] current = {Color.WHITE};
				swatch.addActionListener(e -> store.pickColour(swatch, row, current[0], chosen ->
				{
					current[0] = chosen;
					swatch.setBackground(chosen);
					store.set(row, chosen);
				}));
				showers.put(row.key, value ->
				{
					if (value instanceof Color)
					{
						current[0] = (Color) value;
						swatch.setBackground((Color) value);
					}
				});
				name.setText(SalvagingSpotPanel.html(label, TEXT_WIDTH));
				panel.add(name, BorderLayout.CENTER);
				panel.add(swatch, BorderLayout.EAST);
				break;
			}
			default:
				break;
		}
		return panel;
	}

	private static void small(JLabel label)
	{
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setAlignmentX(LEFT_ALIGNMENT);
	}
}
