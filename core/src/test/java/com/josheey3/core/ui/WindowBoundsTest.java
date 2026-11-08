package com.josheey3.core.ui;

import java.awt.Rectangle;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class WindowBoundsTest
{
	private static final Rectangle LEFT = new Rectangle(0, 0, 1920, 1040);
	private static final Rectangle RIGHT = new Rectangle(1920, 0, 2560, 1400);

	@Test
	public void windowOnSecondMonitorIsReachable()
	{
		assertTrue(WindowBounds.isReachable(new Rectangle(2500, 100, 260, 800), List.of(LEFT, RIGHT)));
	}

	@Test
	public void windowOnUnpluggedMonitorIsNotReachable()
	{
		assertFalse(WindowBounds.isReachable(new Rectangle(2500, 100, 260, 800), List.of(LEFT)));
	}

	@Test
	public void titleBarAboveScreenIsNotReachable()
	{
		assertFalse(WindowBounds.isReachable(new Rectangle(100, -200, 260, 800), List.of(LEFT)));
	}

	@Test
	public void sliverOnScreenEdgeIsNotReachable()
	{
		assertFalse(WindowBounds.isReachable(new Rectangle(1900, 100, 260, 800), List.of(LEFT)));
	}

	@Test
	public void clampMovesAndShrinksIntoScreen()
	{
		assertEquals(new Rectangle(1660, 0, 260, 1040), WindowBounds.clamp(new Rectangle(1800, -50, 260, 2000), LEFT));
		assertEquals(new Rectangle(10, 10, 260, 800), WindowBounds.clamp(new Rectangle(10, 10, 260, 800), LEFT));
	}
}
