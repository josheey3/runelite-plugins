package com.josheey3.panelpopout;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatNativeWindowBorder;
import com.josheey3.core.ui.Icons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.WindowConstants;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.SwingUtil;

/**
 * A standalone window showing one popped-out sidebar panel. The pin and pop-in buttons sit in the
 * title bar (FlatLaf embeds the menu bar there when native window decorations are available, as
 * RuneLite's own window does) and fall back to a strip under the title bar otherwise.
 */
class PopoutWindow extends JFrame
{
	private static final Color ICON = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color ICON_HOVER = Color.WHITE;
	private static final int ICON_SIZE = 14;

	private final JPanel root = new JPanel(new BorderLayout());
	private final JToggleButton pinButton;
	private Rectangle normalBounds;

	/**
	 * @param onPopIn   the pop-in button: put the panel back and open it in the sidebar
	 * @param onClose   the window's close button: put the panel back without opening it
	 * @param onChanged bounds or always-on-top changed (to persist)
	 */
	PopoutWindow(String name, Icon tabIcon, boolean alwaysOnTop, Runnable onPopIn, Runnable onClose, Runnable onChanged)
	{
		super(name);

		List<Image> icons = new ArrayList<>();
		if (tabIcon instanceof ImageIcon)
		{
			icons.add(((ImageIcon) tabIcon).getImage());
		}
		icons.add(ClientUI.ICON_128);
		setIconImages(icons);

		if (FlatNativeWindowBorder.isSupported())
		{
			getRootPane().putClientProperty(FlatClientProperties.USE_WINDOW_DECORATIONS, true);
		}

		pinButton = new JToggleButton(Icons.pin(ICON_SIZE, ICON, false));
		pinButton.setSelectedIcon(Icons.pin(ICON_SIZE, ColorScheme.BRAND_ORANGE, true));
		pinButton.setRolloverIcon(Icons.pin(ICON_SIZE, ICON_HOVER, false));
		pinButton.setRolloverSelectedIcon(Icons.pin(ICON_SIZE, ColorScheme.BRAND_ORANGE, true));
		decorate(pinButton);
		pinButton.addActionListener(e ->
		{
			applyPinned(pinButton.isSelected());
			onChanged.run();
		});
		applyPinned(alwaysOnTop);

		JButton popInButton = new JButton(Icons.popIn(ICON_SIZE, ICON));
		popInButton.setRolloverIcon(Icons.popIn(ICON_SIZE, ICON_HOVER));
		popInButton.setToolTipText("Pop back into the sidebar");
		decorate(popInButton);
		popInButton.addActionListener(e -> onPopIn.run());

		JMenuBar bar = new JMenuBar();
		bar.add(Box.createGlue());
		bar.add(pinButton);
		bar.add(popInButton);
		setJMenuBar(bar);

		root.setBackground(ColorScheme.DARK_GRAY_COLOR);
		setContentPane(root);

		// Don't steal focus from the game when windows open (restores, presets).
		setAutoRequestFocus(false);
		setMinimumSize(new Dimension(180, 150));
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				onClose.run();
			}
		});
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentMoved(ComponentEvent e)
			{
				boundsChanged(onChanged);
			}

			@Override
			public void componentResized(ComponentEvent e)
			{
				boundsChanged(onChanged);
			}
		});
	}

	void setContent(Component content)
	{
		root.removeAll();
		root.add(content, BorderLayout.CENTER);
		root.revalidate();
		root.repaint();
	}

	void releaseContent(Component content)
	{
		root.remove(content);
	}

	boolean isPinned()
	{
		return pinButton.isSelected();
	}

	void applyPinned(boolean pinned)
	{
		pinButton.setSelected(pinned);
		pinButton.setToolTipText(pinned ? "Always on top (click to turn off)" : "Keep this window on top");
		setAlwaysOnTop(pinned);
	}

	/**
	 * @return the window's bounds when not minimised or maximised, for saving
	 */
	Rectangle getNormalBounds()
	{
		return normalBounds != null ? new Rectangle(normalBounds) : getBounds();
	}

	/**
	 * Restores, raises and focuses the window. Only call when the user asks for it.
	 */
	void bringToFront()
	{
		if ((getExtendedState() & Frame.ICONIFIED) != 0)
		{
			setExtendedState(getExtendedState() & ~Frame.ICONIFIED);
		}
		setVisible(true);
		toFront();
		requestFocus();
	}

	private void boundsChanged(Runnable onChanged)
	{
		if (getExtendedState() == Frame.NORMAL && isShowing())
		{
			normalBounds = getBounds();
			onChanged.run();
		}
	}

	private static void decorate(AbstractButton button)
	{
		SwingUtil.removeButtonDecorations(button);
		button.setFocusable(false);
		button.setPreferredSize(new Dimension(28, 24));
	}
}
