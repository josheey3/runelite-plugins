package com.josheey3.core.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;
import javax.swing.ImageIcon;

/**
 * Small vector-drawn UI icons, so plugins don't need PNG resources for basic toolbar glyphs.
 * Icons are drawn on a 16x16 design grid and scaled to {@code size}.
 */
public final class Icons
{
	private Icons()
	{
	}

	/**
	 * A window with an arrow leaving it towards the top-right ("open in new window").
	 */
	public static ImageIcon popOut(int size, Color color)
	{
		return new ImageIcon(popOutImage(size, color));
	}

	/**
	 * {@link #popOut} as an image, e.g. for a {@code NavigationButton} icon.
	 */
	public static BufferedImage popOutImage(int size, Color color)
	{
		return paint(size, color, (g, s) ->
		{
			Path2D box = new Path2D.Float();
			box.moveTo(7, 3);
			box.lineTo(3, 3);
			box.lineTo(3, 13);
			box.lineTo(13, 13);
			box.lineTo(13, 9);
			g.draw(box);
			g.draw(line(7.5f, 8.5f, 13, 3));
			g.draw(polyline(9, 3, 13, 3, 13, 7));
		});
	}

	/**
	 * An arrow entering a window from the top-right ("put back").
	 */
	public static ImageIcon popIn(int size, Color color)
	{
		return draw(size, color, (g, s) ->
		{
			Path2D box = new Path2D.Float();
			box.moveTo(7, 3);
			box.lineTo(3, 3);
			box.lineTo(3, 13);
			box.lineTo(13, 13);
			box.lineTo(13, 9);
			g.draw(box);
			g.draw(line(13, 3, 7.5f, 8.5f));
			g.draw(polyline(7.5f, 4.5f, 7.5f, 8.5f, 11.5f, 8.5f));
		});
	}

	/**
	 * A push pin ("keep on top"). {@code filled} draws the active state.
	 */
	public static ImageIcon pin(int size, Color color, boolean filled)
	{
		return draw(size, color, (g, s) ->
		{
			Path2D head = new Path2D.Float();
			head.moveTo(6, 2.5);
			head.lineTo(10, 2.5);
			head.lineTo(9.5, 7);
			head.lineTo(11.5, 9.5);
			head.lineTo(4.5, 9.5);
			head.lineTo(6.5, 7);
			head.closePath();
			if (filled)
			{
				g.fill(head);
			}
			g.draw(head);
			g.draw(line(8, 9.5f, 8, 14));
		});
	}

	/**
	 * Three horizontal dots ("more actions").
	 */
	public static ImageIcon more(int size, Color color)
	{
		return draw(size, color, (g, s) ->
		{
			g.fill(new Ellipse2D.Float(2, 6.75f, 2.5f, 2.5f));
			g.fill(new Ellipse2D.Float(6.75f, 6.75f, 2.5f, 2.5f));
			g.fill(new Ellipse2D.Float(11.5f, 6.75f, 2.5f, 2.5f));
		});
	}

	private static ImageIcon draw(int size, Color color, BiConsumer<Graphics2D, Integer> painter)
	{
		return new ImageIcon(paint(size, color, painter));
	}

	private static BufferedImage paint(int size, Color color, BiConsumer<Graphics2D, Integer> painter)
	{
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.scale(size / 16.0, size / 16.0);
			g.setColor(color);
			g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			painter.accept(g, size);
		}
		finally
		{
			g.dispose();
		}
		return img;
	}

	private static Path2D line(float x1, float y1, float x2, float y2)
	{
		return polyline(x1, y1, x2, y2);
	}

	private static Path2D polyline(float... xy)
	{
		Path2D p = new Path2D.Float();
		p.moveTo(xy[0], xy[1]);
		for (int i = 2; i < xy.length; i += 2)
		{
			p.lineTo(xy[i], xy[i + 1]);
		}
		return p;
	}
}
