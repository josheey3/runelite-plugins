package com.josheey3.panelpopout;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * Stands in for a popped-out panel in its sidebar tab.
 * <p>
 * RuneLite removes a plugin's tab with {@code sidebar.remove(originalComponent)}, which looks the
 * tab up through {@link javax.swing.JTabbedPane#indexOfComponent}, and that compares with
 * {@code equals}. By also equalling the original component, this placeholder lets that lookup
 * find the right tab, so RuneLite's own sidebar bookkeeping stays correct when a plugin removes
 * a panel while it's popped out (plugin disabled, profile switch, panel hidden in its settings).
 */
class PopoutPlaceholder extends JPanel
{
	private final Component original;

	PopoutPlaceholder(Component original, String name, Runnable showWindow, Runnable popIn)
	{
		this.original = original;

		setLayout(new GridBagLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setPreferredSize(original.getPreferredSize());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 0, 8, 0);

		JLabel label = new JLabel("<html><div style='text-align:center'>" + escape(name) + " is open in its own window.</div></html>",
			SwingConstants.CENTER);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		add(label, c);

		JButton show = new JButton("Show window");
		show.addActionListener(e -> showWindow.run());
		add(show, c);

		JButton back = new JButton("Pop back in");
		back.addActionListener(e -> popIn.run());
		add(back, c);
	}

	Component getOriginal()
	{
		return original;
	}

	@Override
	public boolean equals(Object o)
	{
		return o == this || o == original;
	}

	@Override
	public int hashCode()
	{
		return System.identityHashCode(this);
	}

	@Override
	public java.awt.Dimension getMinimumSize()
	{
		return new java.awt.Dimension(PluginPanel.PANEL_WIDTH, 0);
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
