/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SettingsPageTest
{
	/** Settings held in a map; a reset turns a toggle off, standing in for its default. */
	private static final class FakeStore implements SettingsPage.Store
	{
		final Map<String, Object> values = new HashMap<>();
		final List<List<SettingsRows.Row>> resets = new ArrayList<>();
		final List<String> questions = new ArrayList<>();
		boolean answer;

		@Override
		public Object get(SettingsRows.Row row)
		{
			return values.get(row.key);
		}

		@Override
		public void set(SettingsRows.Row row, Object value)
		{
			values.put(row.key, value);
		}

		@Override
		public void pickColour(Component near, SettingsRows.Row row, Color current, Consumer<Color> chosen)
		{
		}

		@Override
		public void reset(List<SettingsRows.Row> rows)
		{
			resets.add(rows);
			for (SettingsRows.Row row : rows)
			{
				if (row.kind == SettingsRows.Kind.TOGGLE)
				{
					values.put(row.key, false);
				}
			}
		}

		@Override
		public boolean confirm(Component near, String question)
		{
			questions.add(question);
			return answer;
		}

		@Override
		public void back()
		{
		}
	}

	@Test
	public void rightClickResetPutsBackJustThatSettingAndShowsIt() throws Exception
	{
		List<SettingsRows.Row> rows = SettingsRowsTest.rows();
		FakeStore store = new FakeStore();
		for (SettingsRows.Row row : rows)
		{
			if (row.kind == SettingsRows.Kind.TOGGLE)
			{
				store.values.put(row.key, true);
			}
		}
		SwingUtilities.invokeAndWait(() ->
		{
			SettingsPage page = new SettingsPage(rows, store);
			List<JComponent> rowPanels = new ArrayList<>();
			for (JComponent c : all(page, JComponent.class))
			{
				if (c.getComponentPopupMenu() != null && !c.getInheritsPopupMenu())
				{
					rowPanels.add(c);
				}
			}
			assertEquals("every setting has the Reset menu", rows.size(), rowPanels.size());

			int index = 0;
			while (rows.get(index).kind != SettingsRows.Kind.TOGGLE)
			{
				index++;
			}
			JComponent rowPanel = rowPanels.get(index);
			JCheckBox box = all(rowPanel, JCheckBox.class).get(0);
			assertTrue(box.isSelected());

			JMenuItem reset = (JMenuItem) rowPanel.getComponentPopupMenu().getComponent(0);
			assertEquals("Reset", reset.getText());
			reset.doClick();

			assertEquals(1, store.resets.size());
			assertEquals(1, store.resets.get(0).size());
			assertEquals(rows.get(index).key, store.resets.get(0).get(0).key);
			assertFalse("the page shows the default at once", box.isSelected());
			assertTrue("no question for one setting", store.questions.isEmpty());
		});
	}

	@Test
	public void resetEverySettingAsksFirstAndKeepsEverythingOnNo() throws Exception
	{
		List<SettingsRows.Row> rows = SettingsRowsTest.rows();
		FakeStore store = new FakeStore();
		SwingUtilities.invokeAndWait(() ->
		{
			SettingsPage page = new SettingsPage(rows, store);
			JButton resetAll = null;
			for (JButton button : all(page, JButton.class))
			{
				if ("Reset every setting".equals(button.getText()))
				{
					resetAll = button;
				}
			}
			assertTrue(resetAll != null);

			store.answer = false;
			resetAll.doClick();
			assertEquals(1, store.questions.size());
			assertTrue("No keeps every setting", store.resets.isEmpty());

			store.answer = true;
			resetAll.doClick();
			assertEquals(1, store.resets.size());
			assertEquals("Yes resets every setting on the page", rows.size(), store.resets.get(0).size());
		});
	}

	private static <T> List<T> all(Container root, Class<T> type)
	{
		List<T> out = new ArrayList<>();
		for (Component c : root.getComponents())
		{
			if (type.isInstance(c))
			{
				out.add(type.cast(c));
			}
			if (c instanceof Container)
			{
				out.addAll(all((Container) c, type));
			}
		}
		return out;
	}
}
