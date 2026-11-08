package com.josheey3.panelpopout;

import com.formdev.flatlaf.FlatClientProperties;
import com.josheey3.core.ui.ClientSidebar;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.Timer;
import net.runelite.client.RuneLite;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dev-only check of Panel Popout against the real client sidebar. Runs when the dev client is
 * started with PANELPOPOUT_SELFTEST=1 and logs "SELFTEST PASS" or "SELFTEST FAIL". It touches only
 * the RuneLite sidebar UI, never the game, and stops/starts plugins without changing whether they're
 * enabled in the profile.
 */
class PopoutSelfTest
{
	private static final Logger log = LoggerFactory.getLogger(PopoutSelfTest.class);
	private static final String WRAPPED = "Notes";
	private static final String UNWRAPPED = "Time Tracking";
	private static final String OTHER = "XP Tracker";

	private final Deque<Runnable> steps = new ArrayDeque<>();
	private final List<String> failures = new ArrayList<>();
	private PluginManager pluginManager;
	private PopoutManager manager;
	private JTabbedPane sidebar;
	private int tabCountBefore;
	private int waits;

	static void schedule()
	{
		new PopoutSelfTest().begin();
	}

	private void begin()
	{
		steps.add(this::waitForClient);
		steps.add(this::popOutBoth);
		steps.add(this::checkPopped);
		steps.add(this::stopNotesPlugin);
		steps.add(this::checkOrphanRemoved);
		steps.add(this::startNotesPlugin);
		steps.add(this::checkRepopped);
		steps.add(this::rightClickMenu);
		steps.add(this::configurationProtected);
		steps.add(this::middleClickToggles);
		steps.add(this::applyPreset);
		steps.add(this::presetHotkeyPath);
		steps.add(this::restartSelf);
		steps.add(this::checkRestored);
		steps.add(this::popInAll);
		steps.add(this::finish);

		Timer timer = new Timer(1500, null);
		timer.addActionListener(e ->
		{
			Runnable step = steps.peek();
			if (step == null)
			{
				timer.stop();
				return;
			}
			try
			{
				step.run();
			}
			catch (Exception ex)
			{
				check(false, "step threw " + ex);
				log.warn("SELFTEST step error", ex);
				steps.clear();
				steps.add(this::finish);
			}
		});
		timer.start();
	}

	private void next()
	{
		steps.poll();
	}

	private void waitForClient()
	{
		if (RuneLite.getInjector() == null || waits++ > 120)
		{
			return;
		}
		pluginManager = RuneLite.getInjector().getInstance(PluginManager.class);
		Plugin self = plugin(PanelPopoutPlugin.class);
		sidebar = ClientSidebar.find();
		if (self == null || sidebar == null || plugin("NotesPlugin") == null || sidebar.getTabCount() < 6 || indexOf(WRAPPED) < 0 || indexOf(UNWRAPPED) < 0)
		{
			return;
		}
		manager = self.getInjector().getInstance(PopoutManager.class);
		if (!manager.isAttached())
		{
			return;
		}
		manager.popInAll();
		tabCountBefore = sidebar.getTabCount();
		Object leading = sidebar.getClientProperty(FlatClientProperties.TABBED_PANE_LEADING_COMPONENT);
		check(leading instanceof JButton, "pop-out button installed as leading component");
		if (leading instanceof JButton)
		{
			JButton b = (JButton) leading;
			log.warn("SELFTEST pop-out button showing={} bounds={}", b.isShowing(), b.getBounds());
			check(b.isShowing(), "pop-out button is showing");
		}
		check(indexOf(PanelPopoutPlugin.NAME) >= 0, "own sidebar panel present");
		log.warn("SELFTEST client ready: {} tabs", tabCountBefore);
		next();
	}

	private void popOutBoth()
	{
		manager.popOut(WRAPPED);
		manager.popOut(UNWRAPPED);
		next();
	}

	private void checkPopped()
	{
		check(sidebar.getTabCount() == tabCountBefore, "tab count unchanged after popping out");
		for (String name : new String[]{WRAPPED, UNWRAPPED})
		{
			int i = indexOf(name + " (popped out)");
			check(i >= 0, name + " tab still in sidebar, marked popped out");
			if (i >= 0)
			{
				check(sidebar.getComponentAt(i) instanceof PopoutPlaceholder, name + " tab shows placeholder");
				check(!sidebar.isEnabledAt(i), name + " tab disabled");
				Component content = ((PopoutPlaceholder) sidebar.getComponentAt(i)).getOriginal();
				check(content.isShowing(), name + " content showing in its window");
			}
		}
		check(manager.openPopouts().size() == 2, "two popouts open");
		next();
	}

	private void stopNotesPlugin()
	{
		stop("NotesPlugin");
		next();
	}

	private void checkOrphanRemoved()
	{
		check(sidebar.getTabCount() == tabCountBefore - 1, "RuneLite removed the popped-out Notes tab (count " + sidebar.getTabCount() + ")");
		check(indexOf(WRAPPED + " (popped out)") < 0 && indexOf(WRAPPED) < 0, "no Notes tab left behind");
		check(!manager.isPoppedOut(WRAPPED), "Notes window closed");
		check(manager.isPoppedOut(UNWRAPPED), "Time Tracking still popped out");
		next();
	}

	private void startNotesPlugin()
	{
		start("NotesPlugin");
		next();
	}

	private void checkRepopped()
	{
		check(sidebar.getTabCount() == tabCountBefore, "Notes tab back");
		check(manager.isPoppedOut(WRAPPED), "Notes popped out again automatically");
		next();
	}

	private void rightClickMenu()
	{
		int i = indexOf(OTHER);
		check(i >= 0, OTHER + " tab present");
		if (i >= 0)
		{
			Rectangle r = sidebar.getBoundsAt(i);
			sidebar.dispatchEvent(new MouseEvent(sidebar, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
				r.x + r.width / 2, r.y + r.height / 2, 1, false, MouseEvent.BUTTON3));
			// Our menu is shown via invokeLater; check on the next step.
		}
		steps.poll();
		steps.addFirst(this::checkMenu);
	}

	private void checkMenu()
	{
		JPopupMenu menu = null;
		for (MenuElement el : MenuSelectionManager.defaultManager().getSelectedPath())
		{
			if (el instanceof JPopupMenu)
			{
				menu = (JPopupMenu) el;
			}
		}
		boolean hasPopOut = false;
		if (menu != null)
		{
			for (Component c : menu.getComponents())
			{
				hasPopOut |= c instanceof JMenuItem && "Pop out".equals(((JMenuItem) c).getText());
			}
			menu.setVisible(false);
		}
		check(hasPopOut, "right-click menu offers Pop out");
		MenuSelectionManager.defaultManager().clearSelectedPath();
		next();
	}

	private void configurationProtected()
	{
		check(!manager.poppableNames().contains("Configuration"), "Configuration not offered for pop out");
		manager.popOut("Configuration");
		check(!manager.isPoppedOut("Configuration") && indexOf("Configuration") >= 0, "Configuration can't be popped out");
		next();
	}

	private void middleClickToggles()
	{
		int i = indexOf(OTHER);
		Rectangle r = sidebar.getBoundsAt(i);
		MouseEvent click = new MouseEvent(sidebar, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
			r.x + r.width / 2, r.y + r.height / 2, 1, false, MouseEvent.BUTTON2);
		sidebar.dispatchEvent(click);
		check(manager.isPoppedOut(OTHER), "middle-click pops out " + OTHER);
		r = sidebar.getBoundsAt(indexOf(OTHER + " (popped out)"));
		sidebar.dispatchEvent(new MouseEvent(sidebar, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
			r.x + r.width / 2, r.y + r.height / 2, 1, false, MouseEvent.BUTTON2));
		check(!manager.isPoppedOut(OTHER), "middle-click pops " + OTHER + " back in");
		next();
	}

	/**
	 * Hotkeys call PanelPopoutPlugin.openPreset(index); exercise that path with a temporary
	 * preset moved to the top, then remove it again (restores the user's preset order).
	 */
	private void presetHotkeyPath()
	{
		PanelPopoutPlugin self = (PanelPopoutPlugin) plugin(PanelPopoutPlugin.class);
		PresetStore store = self.getInjector().getInstance(PresetStore.class);
		Preset temp = new Preset("Self-test " + System.nanoTime(), new ArrayList<>(List.of(new PopoutState(UNWRAPPED, null, false))));
		store.add(temp);
		while (store.indexOf(temp) > 0)
		{
			store.move(temp, -1);
		}
		self.openPreset(0);
		Set<String> open = new HashSet<>();
		manager.openPopouts().forEach(s -> open.add(s.getName()));
		check(open.equals(Set.of(UNWRAPPED)), "preset at position 1 opened via hotkey path, got " + open);
		store.remove(temp);
		check(store.byName(temp.getName()) == null, "temporary preset removed");
		manager.popInAll();
		// Put the earlier preset layout back for the restart check.
		List<PopoutState> layout = new ArrayList<>();
		layout.add(new PopoutState(WRAPPED, null, false));
		layout.add(new PopoutState(OTHER, null, true));
		manager.applyLayout(layout);
		next();
	}

	private void applyPreset()
	{
		List<PopoutState> layout = new ArrayList<>();
		layout.add(new PopoutState(WRAPPED, null, false));
		layout.add(new PopoutState(OTHER, null, true));
		layout.add(new PopoutState("Not A Real Panel", null, false));
		List<String> missing = manager.applyLayout(layout);
		Set<String> open = new HashSet<>();
		manager.openPopouts().forEach(s -> open.add(s.getName()));
		check(open.equals(Set.of(WRAPPED, OTHER)), "preset opened exactly Notes + XP Tracker, got " + open);
		check(missing.equals(List.of("Not A Real Panel")), "preset reports missing panel");
		check(manager.openPopouts().stream().anyMatch(s -> s.getName().equals(OTHER) && s.isAlwaysOnTop()), "preset applied always-on-top");
		check(sidebar.getComponentAt(indexOf(UNWRAPPED)) != null && !(sidebar.getComponentAt(indexOf(UNWRAPPED)) instanceof PopoutPlaceholder),
			"Time Tracking returned to sidebar");
		next();
	}

	private void restartSelf()
	{
		Plugin self = plugin(PanelPopoutPlugin.class);
		stop(self);
		check(manager.openPopouts().isEmpty(), "all popouts docked when plugin stops");
		start(self);
		manager = self.getInjector().getInstance(PopoutManager.class);
		next();
	}

	private void checkRestored()
	{
		Set<String> open = new HashSet<>();
		manager.openPopouts().forEach(s -> open.add(s.getName()));
		check(open.equals(Set.of(WRAPPED, OTHER)), "popouts restored after plugin restart, got " + open);
		check(sidebar.getTabCount() == tabCountBefore, "tab count intact after restart (" + sidebar.getTabCount() + ")");
		next();
	}

	private void popInAll()
	{
		manager.popInAll();
		boolean anyPlaceholder = false;
		for (int i = 0; i < sidebar.getTabCount(); i++)
		{
			anyPlaceholder |= sidebar.getComponentAt(i) instanceof PopoutPlaceholder || !sidebar.isEnabledAt(i);
		}
		check(!anyPlaceholder, "every tab restored and enabled");
		next();
	}

	private void finish()
	{
		if (failures.isEmpty())
		{
			log.warn("SELFTEST PASS");
		}
		else
		{
			log.warn("SELFTEST FAIL ({}): {}", failures.size(), failures);
		}
		steps.clear();
	}

	// ---- helpers ----

	private void check(boolean ok, String what)
	{
		log.warn("SELFTEST {} {}", ok ? "ok  " : "FAIL", what);
		if (!ok)
		{
			failures.add(what);
		}
	}

	private int indexOf(String tooltip)
	{
		for (int i = 0; i < sidebar.getTabCount(); i++)
		{
			if (tooltip.equals(sidebar.getToolTipTextAt(i)))
			{
				return i;
			}
		}
		return -1;
	}

	private Plugin plugin(Class<?> type)
	{
		return plugin(type.getSimpleName());
	}

	private Plugin plugin(String simpleName)
	{
		for (Plugin p : pluginManager.getPlugins())
		{
			if (p.getClass().getSimpleName().equals(simpleName))
			{
				return p;
			}
		}
		return null;
	}

	private void stop(String simpleName)
	{
		stop(plugin(simpleName));
	}

	private void start(String simpleName)
	{
		start(plugin(simpleName));
	}

	private void stop(Plugin p)
	{
		try
		{
			pluginManager.stopPlugin(p);
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	private void start(Plugin p)
	{
		try
		{
			pluginManager.startPlugin(p);
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
}
