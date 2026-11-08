package com.josheey3.core.ui;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Window position and size that can be stored in config (via Gson) and safely restored, even when
 * the monitor it was on has since been unplugged.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WindowBounds
{
	/**
	 * How much of a window's title bar must be on a screen for it to count as reachable.
	 */
	static final int MIN_VISIBLE = 48;
	private static final int TITLE_BAR_HEIGHT = 32;

	private int x;
	private int y;
	private int width;
	private int height;

	public static WindowBounds of(Window window)
	{
		Rectangle r = window.getBounds();
		return new WindowBounds(r.x, r.y, r.width, r.height);
	}

	public Rectangle toRectangle()
	{
		return new Rectangle(x, y, width, height);
	}

	/**
	 * Applies these bounds if the window would still be reachable on the current screens.
	 *
	 * @return false (and leaves the window alone) if the saved position is off every screen
	 */
	public boolean applyIfVisible(Window window)
	{
		Rectangle r = toRectangle();
		if (width <= 0 || height <= 0 || !isReachable(r, screens()))
		{
			return false;
		}
		window.setBounds(r);
		return true;
	}

	/**
	 * A window is reachable if enough of its title bar overlaps one screen to grab and drag it.
	 */
	static boolean isReachable(Rectangle bounds, Collection<Rectangle> screens)
	{
		Rectangle titleBar = new Rectangle(bounds.x, bounds.y, bounds.width, Math.min(TITLE_BAR_HEIGHT, bounds.height));
		for (Rectangle screen : screens)
		{
			Rectangle overlap = titleBar.intersection(screen);
			if (!overlap.isEmpty() && overlap.width >= Math.min(MIN_VISIBLE, bounds.width) && overlap.y == titleBar.y)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Moves and if needed shrinks {@code bounds} so it fits entirely inside {@code screen}.
	 */
	public static Rectangle clamp(Rectangle bounds, Rectangle screen)
	{
		int w = Math.min(bounds.width, screen.width);
		int h = Math.min(bounds.height, screen.height);
		int nx = Math.max(screen.x, Math.min(bounds.x, screen.x + screen.width - w));
		int ny = Math.max(screen.y, Math.min(bounds.y, screen.y + screen.height - h));
		return new Rectangle(nx, ny, w, h);
	}

	/**
	 * @return the usable area (excluding taskbars) of every screen
	 */
	public static List<Rectangle> screens()
	{
		List<Rectangle> result = new ArrayList<>();
		if (GraphicsEnvironment.isHeadless())
		{
			return result;
		}
		for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
		{
			result.add(usableBounds(device.getDefaultConfiguration()));
		}
		return result;
	}

	public static Rectangle usableBounds(GraphicsConfiguration gc)
	{
		Rectangle b = gc.getBounds();
		Insets in = Toolkit.getDefaultToolkit().getScreenInsets(gc);
		return new Rectangle(b.x + in.left, b.y + in.top, b.width - in.left - in.right, b.height - in.top - in.bottom);
	}
}
