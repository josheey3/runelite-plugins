package com.josheey3.panelpopout;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.josheey3.core.config.JsonConfig;
import com.josheey3.core.ui.ClientSidebar;
import com.josheey3.core.ui.Icons;
import com.josheey3.core.ui.WindowBounds;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

/**
 * Moves sidebar panels in and out of their own windows.
 * <p>
 * How it stays compatible with RuneLite's sidebar (a {@link JTabbedPane} that RuneLite maps to
 * plugins by tab index):
 * <ul>
 * <li>Popping out swaps the tab's component for a {@link PopoutPlaceholder}, so the tab and its
 * index stay put, and disables the tab so it can't be selected.</li>
 * <li>A popped-out panel is never the selected tab, so RuneLite never deactivates it; we call
 * {@code onActivate}/{@code onDeactivate} ourselves when it leaves and returns.</li>
 * <li>When a plugin removes its panel while popped out, RuneLite's removal finds the placeholder
 * (see {@link PopoutPlaceholder#equals}) and we close the window from the container event.</li>
 * <li>The Configuration panel can never be popped out, so plugin settings (including turning this
 * plugin off) always stay reachable in the sidebar.</li>
 * <li>Any unexpected error puts every panel back in the sidebar ({@link #safely}).</li>
 * </ul>
 * All methods must be called on the Swing EDT.
 */
@Slf4j
@Singleton
class PopoutManager
{
	private static final TypeToken<List<PopoutState>> STATE_LIST = new TypeToken<List<PopoutState>>()
	{
	};
	private static final String POPPED_SUFFIX = " (popped out)";
	private static final int ATTACH_RETRY_MS = 500;
	private static final int ATTACH_MAX_TRIES = 120;
	private static final String CONFIG_PANEL_CLASS = "net.runelite.client.plugins.config.TopLevelConfigPanel";
	private static final String CONFIG_PANEL_NAME = "Configuration";

	private final ConfigManager configManager;
	private final PanelPopoutConfig config;
	private final Gson gson;

	private final List<Popout> popouts = new ArrayList<>();
	/**
	 * Popouts to reopen when their tab appears, by tab name. This is what gets persisted.
	 */
	private final Map<String, PopoutState> remembered = new LinkedHashMap<>();
	private final List<Runnable> changeListeners = new ArrayList<>();
	private final Timer saveTimer;

	@Nullable
	private JTabbedPane sidebar;
	@Nullable
	private Timer attachTimer;
	@Nullable
	private JButton sidebarButton;
	@Nullable
	private Frame clientFrame;
	private int attachTries;
	private boolean selfChange;

	private static final class Popout
	{
		final String name;
		final Component content;
		PopoutPlaceholder placeholder;
		PopoutWindow window;
		boolean minimizedWithClient;

		Popout(String name, Component content)
		{
			this.name = name;
			this.content = content;
		}
	}

	@Inject
	PopoutManager(ConfigManager configManager, PanelPopoutConfig config, Gson gson)
	{
		this.configManager = configManager;
		this.config = config;
		this.gson = gson;
		saveTimer = new Timer(750, e -> save());
		saveTimer.setRepeats(false);
	}

	// ---- lifecycle -------------------------------------------------------------------------

	void start()
	{
		remembered.clear();
		if (config.rememberPopouts())
		{
			for (PopoutState s : JsonConfig.load(configManager, gson, PanelPopoutConfig.GROUP, PanelPopoutConfig.STATE_KEY, STATE_LIST, Collections.emptyList()))
			{
				if (s != null && s.getName() != null)
				{
					remembered.put(s.getName(), s);
				}
			}
		}

		attachTries = 0;
		if (!tryAttach())
		{
			attachTimer = new Timer(ATTACH_RETRY_MS, e -> tryAttach());
			attachTimer.start();
		}
	}

	void stop()
	{
		if (attachTimer != null)
		{
			attachTimer.stop();
			attachTimer = null;
		}
		if (sidebar != null)
		{
			// Keep them remembered so they come back when the plugin is turned on again.
			for (Popout p : new ArrayList<>(popouts))
			{
				dock(p, false, false);
			}
			sidebar.removeMouseListener(mouseListener);
			sidebar.removeChangeListener(changeListener);
			sidebar.removeContainerListener(containerListener);
			removeSidebarButton();
			sidebar = null;
		}
		if (clientFrame != null)
		{
			clientFrame.removeWindowStateListener(clientStateListener);
			clientFrame = null;
		}
		saveTimer.stop();
		save();
		fireChanged();
	}

	/**
	 * Writes window positions immediately (client shutting down).
	 */
	void saveNow()
	{
		saveTimer.stop();
		save();
	}

	private boolean tryAttach()
	{
		JTabbedPane found = ClientSidebar.find();
		if (found == null)
		{
			if (++attachTries >= ATTACH_MAX_TRIES && attachTimer != null)
			{
				attachTimer.stop();
				attachTimer = null;
				log.warn("Couldn't find the RuneLite sidebar; Panel Popout is inactive");
			}
			return false;
		}
		if (attachTimer != null)
		{
			attachTimer.stop();
			attachTimer = null;
		}

		sidebar = found;
		sidebar.addMouseListener(mouseListener);
		sidebar.addChangeListener(changeListener);
		sidebar.addContainerListener(containerListener);
		Window ancestor = SwingUtilities.getWindowAncestor(sidebar);
		if (ancestor instanceof Frame)
		{
			// Only listened to, never changed: see the minimise-with-RuneLite option.
			clientFrame = (Frame) ancestor;
			clientFrame.addWindowStateListener(clientStateListener);
		}
		updateSidebarButton();
		restoreRemembered();
		fireChanged();
		log.debug("Attached to sidebar with {} tabs", sidebar.getTabCount());
		return true;
	}

	// ---- queries for the UI ----------------------------------------------------------------

	void addChangeListener(Runnable listener)
	{
		changeListeners.add(listener);
	}

	void removeChangeListener(Runnable listener)
	{
		changeListeners.remove(listener);
	}

	boolean isAttached()
	{
		return sidebar != null;
	}

	/**
	 * @return names of all sidebar panels, in sidebar order
	 */
	List<String> panelNames()
	{
		List<String> names = new ArrayList<>();
		if (sidebar != null)
		{
			for (int i = 0; i < sidebar.getTabCount(); i++)
			{
				String name = nameAt(i);
				if (name != null)
				{
					names.add(name);
				}
			}
		}
		return names;
	}

	/**
	 * @return the currently open popouts with their current window state
	 */
	List<PopoutState> openPopouts()
	{
		List<PopoutState> result = new ArrayList<>();
		for (Popout p : popouts)
		{
			result.add(capture(p));
		}
		return result;
	}

	boolean isPoppedOut(String name)
	{
		return byName(name) != null;
	}

	/**
	 * @return names of sidebar panels that can be popped out right now, in sidebar order
	 */
	List<String> poppableNames()
	{
		List<String> names = new ArrayList<>();
		if (sidebar != null)
		{
			for (int i = 0; i < sidebar.getTabCount(); i++)
			{
				if (canPopOut(i))
				{
					names.add(sidebar.getToolTipTextAt(i));
				}
			}
		}
		return names;
	}

	// ---- actions ---------------------------------------------------------------------------

	void popOut(String name)
	{
		safely("pop out " + name, () ->
		{
			int index = indexOfName(name);
			if (canPopOut(index))
			{
				popOut(index, null);
			}
		});
	}

	void popIn(String name, boolean openInSidebar)
	{
		safely("pop in " + name, () ->
		{
			Popout p = byName(name);
			if (p != null)
			{
				dock(p, true, openInSidebar);
			}
		});
	}

	void popInAll()
	{
		safely("pop everything back in", () ->
		{
			for (Popout p : new ArrayList<>(popouts))
			{
				dock(p, true, false);
			}
		});
	}

	void show(String name)
	{
		Popout p = byName(name);
		if (p != null)
		{
			p.window.bringToFront();
		}
	}

	/**
	 * Makes exactly the panels in {@code layout} popped out, at their saved positions where known,
	 * and puts every other popped-out panel back in the sidebar.
	 *
	 * @return names from the layout that aren't in the sidebar (plugin off or not installed)
	 */
	List<String> applyLayout(List<PopoutState> layout)
	{
		try
		{
			return doApplyLayout(layout);
		}
		catch (RuntimeException e)
		{
			log.warn("Panel Popout failed to apply a preset; putting all panels back in the sidebar", e);
			restoreAllQuietly();
			return Collections.emptyList();
		}
	}

	private List<String> doApplyLayout(List<PopoutState> layout)
	{
		Set<String> wanted = new LinkedHashSet<>();
		for (PopoutState s : layout)
		{
			wanted.add(s.getName());
		}
		for (Popout p : new ArrayList<>(popouts))
		{
			if (!wanted.contains(p.name))
			{
				dock(p, true, false);
			}
		}

		List<String> missing = new ArrayList<>();
		for (PopoutState s : layout)
		{
			Popout open = byName(s.getName());
			if (open != null)
			{
				if (s.getBounds() != null)
				{
					s.getBounds().applyIfVisible(open.window);
				}
				open.window.applyPinned(s.isAlwaysOnTop());
				continue;
			}
			int index = indexOfName(s.getName());
			if (canPopOut(index))
			{
				popOut(index, s);
			}
			else
			{
				missing.add(s.getName());
			}
		}
		scheduleSave();
		fireChanged();
		return missing;
	}

	void updateSidebarButton()
	{
		if (sidebar == null)
		{
			return;
		}
		if (!config.showPopoutButton())
		{
			removeSidebarButton();
			return;
		}
		if (sidebarButton == null)
		{
			Object existing = sidebar.getClientProperty(FlatClientProperties.TABBED_PANE_LEADING_COMPONENT);
			if (existing != null)
			{
				log.debug("Sidebar leading component already in use ({}); not adding the pop-out button", existing.getClass().getName());
				return;
			}
			sidebarButton = createSidebarButton();
			sidebar.putClientProperty(FlatClientProperties.TABBED_PANE_LEADING_COMPONENT, sidebarButton);
		}
		int index = sidebar.getSelectedIndex();
		boolean enabled = canPopOut(index);
		sidebarButton.setEnabled(enabled);
		sidebarButton.setToolTipText(enabled ? "Pop out " + sidebar.getToolTipTextAt(index) : "Open a panel, then click here to pop it out");
	}

	// ---- popping in and out ----------------------------------------------------------------

	private void popOut(int index, @Nullable PopoutState state)
	{
		assert sidebar != null;
		Component content = sidebar.getComponentAt(index);
		String name = sidebar.getToolTipTextAt(index);
		Icon icon = sidebar.getIconAt(index);

		if (sidebar.getSelectedIndex() == index)
		{
			// RuneLite deactivates it; we activate it again once it's in its window.
			sidebar.setSelectedIndex(-1);
		}

		Popout p = new Popout(name, content);
		p.placeholder = new PopoutPlaceholder(content, name, () -> p.window.bringToFront(),
			() -> safely("pop in " + name, () -> dock(p, true, true)));
		boolean onTop = state != null ? state.isAlwaysOnTop() : config.alwaysOnTopByDefault();
		p.window = new PopoutWindow(name, icon, onTop,
			() -> safely("pop in " + name, () -> dock(p, true, true)),
			() -> safely("close " + name, () -> dock(p, true, false)),
			this::scheduleSave);

		// Swap first: adding content to the window while it's still a tab would make Swing call
		// sidebar.remove(content), which removes the whole tab.
		selfChange = true;
		try
		{
			sidebar.setComponentAt(index, p.placeholder);
		}
		finally
		{
			selfChange = false;
		}
		// Tracked from here on, so a failure below can still put the panel back.
		popouts.add(p);
		sidebar.setEnabledAt(index, false);
		sidebar.setToolTipTextAt(index, name + POPPED_SUFFIX);

		content.setVisible(true);
		p.window.setContent(content);
		place(p.window, state);
		p.window.setVisible(true);
		SwingUtil.activate(ClientSidebar.findPluginPanel(content));

		remembered.put(name, capture(p));
		updateSidebarButton();
		scheduleSave();
		fireChanged();
		log.debug("Popped out {}", name);
	}

	/**
	 * @param forget        stop reopening it on restart
	 * @param openInSidebar select the tab afterwards (explicit "pop back in")
	 */
	private void dock(Popout p, boolean forget, boolean openInSidebar)
	{
		if (!popouts.remove(p))
		{
			return;
		}
		PopoutState last = capture(p);
		SwingUtil.deactivate(ClientSidebar.findPluginPanel(p.content));

		int index = sidebar == null ? -1 : ClientSidebar.indexOfTab(sidebar, p.placeholder);
		if (index >= 0)
		{
			selfChange = true;
			try
			{
				sidebar.setComponentAt(index, p.content);
			}
			finally
			{
				selfChange = false;
			}
			sidebar.setEnabledAt(index, true);
			sidebar.setToolTipTextAt(index, p.name);
		}
		else
		{
			p.window.releaseContent(p.content);
		}
		p.window.dispose();

		if (forget)
		{
			remembered.remove(p.name);
		}
		else
		{
			remembered.put(p.name, last);
		}
		if (index >= 0 && openInSidebar)
		{
			sidebar.setSelectedIndex(index);
		}
		updateSidebarButton();
		scheduleSave();
		fireChanged();
		log.debug("Popped {} back in", p.name);
	}

	/**
	 * The panel's plugin removed it from the sidebar while it was popped out.
	 */
	private void orphaned(Popout p)
	{
		popouts.remove(p);
		PopoutState last = capture(p);
		SwingUtil.deactivate(ClientSidebar.findPluginPanel(p.content));
		p.window.releaseContent(p.content);
		p.window.dispose();
		// Reopen it if the panel comes back (plugin turned back on, profile switched back).
		remembered.put(p.name, last);
		scheduleSave();
		fireChanged();
		log.debug("{} was removed from the sidebar; closed its window", p.name);
	}

	private void restoreRemembered()
	{
		if (sidebar == null || !config.rememberPopouts() || remembered.isEmpty())
		{
			return;
		}
		for (int i = 0; i < sidebar.getTabCount(); i++)
		{
			String name = sidebar.getToolTipTextAt(i);
			PopoutState state = name == null ? null : remembered.get(name);
			if (state != null && byName(name) == null && canPopOut(i))
			{
				popOut(i, state);
			}
		}
	}

	private void place(PopoutWindow window, @Nullable PopoutState state)
	{
		if (state != null && state.getBounds() != null && state.getBounds().applyIfVisible(window))
		{
			return;
		}
		Window main = SwingUtilities.getWindowAncestor(sidebar);
		int offset = 30 * (popouts.size() % 8);
		int width = PluginPanel.PANEL_WIDTH + PluginPanel.SCROLLBAR_WIDTH + 24;
		Rectangle desired;
		Rectangle screen;
		if (main != null)
		{
			desired = new Rectangle(main.getX() + main.getWidth() + 8 + offset, main.getY() + offset, width, main.getHeight());
			screen = WindowBounds.usableBounds(main.getGraphicsConfiguration());
		}
		else
		{
			desired = new Rectangle(100 + offset, 100 + offset, width, 700);
			List<Rectangle> screens = WindowBounds.screens();
			screen = screens.isEmpty() ? desired : screens.get(0);
		}
		window.setBounds(WindowBounds.clamp(desired, screen));
	}

	private static PopoutState capture(Popout p)
	{
		Rectangle r = p.window.getNormalBounds();
		return new PopoutState(p.name, new WindowBounds(r.x, r.y, r.width, r.height), p.window.isPinned());
	}

	// ---- sidebar listeners -----------------------------------------------------------------

	private final MouseListener mouseListener = new MouseAdapter()
	{
		@Override
		public void mouseClicked(MouseEvent e)
		{
			safely("handle a sidebar click", () -> sidebarClicked(e));
		}
	};

	private void sidebarClicked(MouseEvent e)
	{
		if (sidebar == null)
		{
			return;
		}
		int index = sidebar.indexAtLocation(e.getX(), e.getY());
		if (index < 0)
		{
			return;
		}
		Popout p = popoutAt(index);
		if (SwingUtilities.isLeftMouseButton(e))
		{
			// Popped-out tabs are disabled; clicking one brings its window forward instead.
			if (p != null)
			{
				p.window.bringToFront();
			}
		}
		else if (SwingUtilities.isMiddleMouseButton(e))
		{
			// Middle-click toggles.
			if (p != null)
			{
				dock(p, true, false);
			}
			else if (canPopOut(index))
			{
				popOut(index, null);
			}
		}
		else if (SwingUtilities.isRightMouseButton(e))
		{
			Component tab = sidebar.getComponentAt(index);
			int x = e.getX();
			int y = e.getY();
			// After RuneLite's own handler, which may already be showing a menu for this tab.
			SwingUtilities.invokeLater(() -> safely("show the sidebar menu", () -> showTabMenu(tab, x, y)));
		}
	}

	private final ChangeListener changeListener = e -> safely("handle a sidebar selection", () ->
	{
		if (sidebar == null)
		{
			return;
		}
		updateSidebarButton();
		int index = sidebar.getSelectedIndex();
		Popout p = popoutAt(index);
		if (p != null)
		{
			// Something selected a popped-out tab (a plugin opening its own panel, the sidebar
			// hotkey). Undo it after RuneLite has processed the selection, then reactivate the
			// panel, which RuneLite deactivates when the selection moves away.
			SwingUtilities.invokeLater(() -> safely("deselect a popped-out tab", () ->
			{
				if (sidebar != null && sidebar.getSelectedIndex() >= 0 && sidebar.getSelectedComponent() == p.placeholder)
				{
					sidebar.setSelectedIndex(-1);
				}
				if (popouts.contains(p))
				{
					SwingUtil.activate(ClientSidebar.findPluginPanel(p.content));
				}
			}));
		}
	});

	private final ContainerListener containerListener = new ContainerListener()
	{
		@Override
		public void componentAdded(ContainerEvent e)
		{
			if (selfChange || sidebar == null || ClientSidebar.indexOfTab(sidebar, e.getChild()) < 0)
			{
				return;
			}
			// A plugin added a panel. Let RuneLite finish adding it before touching it.
			SwingUtilities.invokeLater(() -> safely("restore a popout", () ->
			{
				restoreRemembered();
				fireChanged();
			}));
		}

		@Override
		public void componentRemoved(ContainerEvent e)
		{
			if (selfChange)
			{
				return;
			}
			if (e.getChild() instanceof PopoutPlaceholder)
			{
				Popout p = byPlaceholder(e.getChild());
				if (p != null)
				{
					safely("close a removed panel's window", () -> orphaned(p));
				}
			}
			else
			{
				SwingUtilities.invokeLater(() -> fireChanged());
			}
		}
	};

	/**
	 * Follows RuneLite being minimised and restored, if enabled. Only our own windows change.
	 */
	private final WindowStateListener clientStateListener = e -> safely("follow RuneLite minimising", () -> clientStateChanged(e));

	private void clientStateChanged(WindowEvent e)
	{
		boolean wasMinimized = (e.getOldState() & Frame.ICONIFIED) != 0;
		boolean isMinimized = (e.getNewState() & Frame.ICONIFIED) != 0;
		if (!config.minimizeWithClient() || wasMinimized == isMinimized)
		{
			return;
		}
		for (Popout p : popouts)
		{
			if (isMinimized && (p.window.getExtendedState() & Frame.ICONIFIED) == 0)
			{
				p.minimizedWithClient = true;
				p.window.minimize();
			}
			else if (!isMinimized && p.minimizedWithClient)
			{
				p.minimizedWithClient = false;
				p.window.restoreWithoutFocus();
			}
		}
	}

	/**
	 * Runs a UI action; if it throws, puts every panel back in the sidebar so a bug here can
	 * never leave the sidebar in a broken state.
	 */
	private void safely(String what, Runnable action)
	{
		try
		{
			action.run();
		}
		catch (RuntimeException e)
		{
			log.warn("Panel Popout failed to {}; putting all panels back in the sidebar", what, e);
			restoreAllQuietly();
		}
	}

	private void restoreAllQuietly()
	{
		for (Popout p : new ArrayList<>(popouts))
		{
			try
			{
				dock(p, false, false);
			}
			catch (RuntimeException e)
			{
				log.warn("Couldn't put {} back in the sidebar", p.name, e);
				popouts.remove(p);
			}
		}
	}

	private void showTabMenu(Component tab, int x, int y)
	{
		if (sidebar == null || ClientSidebar.indexOfTab(sidebar, tab) < 0)
		{
			return;
		}
		List<JMenuItem> items = new ArrayList<>();
		Popout p = tab instanceof PopoutPlaceholder ? byPlaceholder(tab) : null;
		if (p != null)
		{
			JMenuItem show = new JMenuItem("Show window");
			show.addActionListener(ev -> p.window.bringToFront());
			items.add(show);
			JMenuItem back = new JMenuItem("Pop back in");
			back.addActionListener(ev -> dock(p, true, true));
			items.add(back);
		}
		else
		{
			JMenuItem out = new JMenuItem("Pop out");
			int index = ClientSidebar.indexOfTab(sidebar, tab);
			out.setEnabled(canPopOut(index));
			if (isProtected(tab, sidebar.getToolTipTextAt(index)))
			{
				out.setToolTipText("Configuration stays in the sidebar so settings are always reachable");
			}
			out.addActionListener(ev -> safely("pop out a panel", () ->
			{
				int now = sidebar == null ? -1 : ClientSidebar.indexOfTab(sidebar, tab);
				if (canPopOut(now))
				{
					popOut(now, null);
				}
			}));
			items.add(out);
		}

		JPopupMenu existing = visibleSidebarMenu();
		if (existing != null)
		{
			existing.addSeparator();
			items.forEach(existing::add);
			existing.pack();
		}
		else
		{
			JPopupMenu menu = new JPopupMenu();
			items.forEach(menu::add);
			menu.show(sidebar, x, y);
		}
	}

	@Nullable
	private JPopupMenu visibleSidebarMenu()
	{
		for (MenuElement el : MenuSelectionManager.defaultManager().getSelectedPath())
		{
			if (el instanceof JPopupMenu && ((JPopupMenu) el).getInvoker() == sidebar && ((JPopupMenu) el).isVisible())
			{
				return (JPopupMenu) el;
			}
		}
		return null;
	}

	private JButton createSidebarButton()
	{
		JButton b = new JButton(Icons.popOut(16, ColorScheme.LIGHT_GRAY_COLOR));
		b.setRolloverIcon(Icons.popOut(16, ColorScheme.BRAND_ORANGE));
		SwingUtil.removeButtonDecorations(b);
		b.setFocusable(false);
		b.setPreferredSize(new Dimension(26, 26));
		b.addActionListener(e -> safely("pop out the open panel", () ->
		{
			if (sidebar != null && canPopOut(sidebar.getSelectedIndex()))
			{
				popOut(sidebar.getSelectedIndex(), null);
			}
		}));
		return b;
	}

	private void removeSidebarButton()
	{
		if (sidebarButton != null && sidebar != null
			&& sidebar.getClientProperty(FlatClientProperties.TABBED_PANE_LEADING_COMPONENT) == sidebarButton)
		{
			sidebar.putClientProperty(FlatClientProperties.TABBED_PANE_LEADING_COMPONENT, null);
		}
		sidebarButton = null;
	}

	// ---- helpers ---------------------------------------------------------------------------

	private boolean canPopOut(int index)
	{
		if (sidebar == null || index < 0 || index >= sidebar.getTabCount())
		{
			return false;
		}
		Component c = sidebar.getComponentAt(index);
		String name = sidebar.getToolTipTextAt(index);
		return c != null && name != null && !(c instanceof PopoutPlaceholder) && !isProtected(c, name);
	}

	/**
	 * Panels that must stay in the sidebar, so plugin settings (including turning this plugin off)
	 * are always reachable even if something goes wrong with a popout window.
	 */
	private static boolean isProtected(Component tabComponent, @Nullable String name)
	{
		return CONFIG_PANEL_CLASS.equals(tabComponent.getClass().getName()) || CONFIG_PANEL_NAME.equals(name);
	}

	@Nullable
	private String nameAt(int index)
	{
		Popout p = popoutAt(index);
		return p != null ? p.name : sidebar.getToolTipTextAt(index);
	}

	private int indexOfName(String name)
	{
		if (sidebar != null)
		{
			for (int i = 0; i < sidebar.getTabCount(); i++)
			{
				if (name.equals(nameAt(i)))
				{
					return i;
				}
			}
		}
		return -1;
	}

	@Nullable
	private Popout popoutAt(int index)
	{
		if (sidebar == null || index < 0 || index >= sidebar.getTabCount())
		{
			return null;
		}
		Component c = sidebar.getComponentAt(index);
		return c instanceof PopoutPlaceholder ? byPlaceholder(c) : null;
	}

	@Nullable
	private Popout byPlaceholder(Component c)
	{
		for (Popout p : popouts)
		{
			if (p.placeholder == c)
			{
				return p;
			}
		}
		return null;
	}

	@Nullable
	private Popout byName(String name)
	{
		for (Popout p : popouts)
		{
			if (p.name.equals(name))
			{
				return p;
			}
		}
		return null;
	}

	private void scheduleSave()
	{
		saveTimer.restart();
	}

	private void save()
	{
		for (Popout p : popouts)
		{
			remembered.put(p.name, capture(p));
		}
		JsonConfig.save(configManager, gson, PanelPopoutConfig.GROUP, PanelPopoutConfig.STATE_KEY, new ArrayList<>(remembered.values()));
	}

	private void fireChanged()
	{
		for (Runnable r : new ArrayList<>(changeListeners))
		{
			r.run();
		}
	}
}
