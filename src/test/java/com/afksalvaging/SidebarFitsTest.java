/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Lays the sidebar out at RuneLite's sidebar width and fails on anything that would be cut off: a
 * label or button given less room than it needs, or anything reaching past the right edge. Found
 * needed on 2026-10-08, when a fourth tab wrapped out of sight and a button's words were cut short.
 */
public class SidebarFitsTest
{
	@Test
	public void everyTabFitsTheSidebarWithNothingCutOff() throws Exception
	{
		List<String> problems = new ArrayList<>();
		int[] tabsSeen = {0};
		SalvagingSpotPanel[] holder = new SalvagingSpotPanel[1];
		SwingUtilities.invokeAndWait(() ->
		{
			SalvagingSpotPanel.Actions actions = (SalvagingSpotPanel.Actions) Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[]{SalvagingSpotPanel.Actions.class}, (p, m, a) -> null);
			SalvagingSpotPanel panel = new SalvagingSpotPanel(actions);
			holder[0] = panel;
			Map<String, Boolean> open = new HashMap<>();
			open.put(SalvagingSpotPanel.FOLD_TOOLS, true);
			open.put(SalvagingSpotPanel.FOLD_DOCKS, true);
			open.put(SalvagingSpotPanel.FOLD_SESSION, true);
			panel.setFolds(open);
			panel.setStats("This session: 120 salvage, 3 holds filled, 12 min waiting for wrecks.");
		});
		// The setters queue their work; let it run.
		SwingUtilities.invokeAndWait(() -> { });
		SwingUtilities.invokeAndWait(() ->
		{
			SalvagingSpotPanel panel = holder[0];
			MaterialTabGroup group = first(panel, MaterialTabGroup.class);
			for (Component c : group.getComponents())
			{
				if (c instanceof MaterialTab)
				{
					tabsSeen[0]++;
					group.select((MaterialTab) c);
					check(panel, ((MaterialTab) c).getText(), problems);
				}
			}
		});
		assertEquals("the four tabs", 4, tabsSeen[0]);
		assertTrue("cut off at the sidebar's width:\n" + String.join("\n", problems), problems.isEmpty());
	}

	private static void check(SalvagingSpotPanel panel, String tab, List<String> problems)
	{
		int width = PluginPanel.PANEL_WIDTH;
		panel.setSize(width, 3000);
		layout(panel);
		for (JComponent c : all(panel, JComponent.class))
		{
			if (!shown(c) || c.getWidth() == 0)
			{
				continue;
			}
			if ((c instanceof JLabel || c instanceof AbstractButton)
				&& (c.getPreferredSize().width > c.getWidth() + 1 || c.getPreferredSize().height > c.getHeight() + 1))
			{
				String text = c instanceof JLabel ? ((JLabel) c).getText() : ((AbstractButton) c).getText();
				problems.add(tab + ": \"" + (text == null ? "" : text.replaceAll("<[^>]+>", "").trim()) + "\" needs "
					+ c.getPreferredSize().width + "x" + c.getPreferredSize().height + ", given " + c.getWidth() + "x" + c.getHeight());
			}
			Point at = SwingUtilities.convertPoint(c.getParent(), c.getLocation(), panel);
			if (at.x + c.getWidth() > width + 1)
			{
				problems.add(tab + ": " + c.getClass().getSimpleName() + " reaches past the right edge");
			}
		}
	}

	private static boolean shown(Component c)
	{
		for (Component p = c; p != null; p = p.getParent())
		{
			if (!p.isVisible())
			{
				return false;
			}
		}
		return true;
	}

	private static void layout(Container c)
	{
		c.doLayout();
		for (Component child : c.getComponents())
		{
			if (child instanceof Container)
			{
				layout((Container) child);
			}
		}
	}

	private static <T> T first(Container root, Class<T> type)
	{
		List<T> found = all(root, type);
		return found.isEmpty() ? null : found.get(0);
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
