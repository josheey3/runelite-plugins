package com.josheey3.core.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.annotation.Nullable;
import javax.swing.JTabbedPane;
import net.runelite.client.ui.PluginPanel;

/**
 * Locates RuneLite's plugin sidebar (the vertical tab strip on the right of the client) using only
 * the public Swing component tree.
 * <p>
 * RuneLite has no API for other plugins' sidebar entries. The sidebar is a {@link JTabbedPane}
 * with {@link JTabbedPane#RIGHT} tab placement inside the client frame; each tab's tooltip is the
 * plugin's navigation button tooltip. This depends on RuneLite internals, so callers must cope
 * with {@link #find()} returning {@code null}.
 */
public final class ClientSidebar
{
	private ClientSidebar()
	{
	}

	/**
	 * Must be called on the Swing EDT.
	 *
	 * @return the client's plugin sidebar, or {@code null} if the client UI isn't built yet
	 */
	@Nullable
	public static JTabbedPane find()
	{
		for (Frame frame : Frame.getFrames())
		{
			if (!frame.isDisplayable())
			{
				continue;
			}
			JTabbedPane found = findIn(frame);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	@Nullable
	private static JTabbedPane findIn(Component root)
	{
		Deque<Component> queue = new ArrayDeque<>();
		queue.add(root);
		while (!queue.isEmpty())
		{
			Component c = queue.poll();
			if (c instanceof JTabbedPane && ((JTabbedPane) c).getTabPlacement() == JTabbedPane.RIGHT)
			{
				return (JTabbedPane) c;
			}
			if (c instanceof Container)
			{
				for (Component child : ((Container) c).getComponents())
				{
					queue.add(child);
				}
			}
		}
		return null;
	}

	/**
	 * Finds the {@link PluginPanel} that a sidebar tab shows. For panels built with
	 * {@code super()} the tab component is a scroll wrapper around the panel; for
	 * {@code super(false)} it is the panel itself.
	 */
	@Nullable
	public static PluginPanel findPluginPanel(@Nullable Component tabComponent)
	{
		if (tabComponent == null)
		{
			return null;
		}
		Deque<Component> queue = new ArrayDeque<>();
		queue.add(tabComponent);
		while (!queue.isEmpty())
		{
			Component c = queue.poll();
			if (c instanceof PluginPanel)
			{
				return (PluginPanel) c;
			}
			if (c instanceof Container)
			{
				for (Component child : ((Container) c).getComponents())
				{
					queue.add(child);
				}
			}
		}
		return null;
	}

	/**
	 * @return the index of the tab whose component is exactly {@code component} (identity, unlike
	 * {@link JTabbedPane#indexOfComponent}, which uses {@code equals}), or -1
	 */
	public static int indexOfTab(JTabbedPane sidebar, Component component)
	{
		for (int i = 0; i < sidebar.getTabCount(); i++)
		{
			if (sidebar.getComponentAt(i) == component)
			{
				return i;
			}
		}
		return -1;
	}
}
