package com.josheey3.panelpopout;

import com.josheey3.core.ui.Icons;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.config.Keybind;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

/**
 * The plugin's own sidebar panel: open popout windows and presets.
 */
class PanelPopoutPanel extends PluginPanel
{
	private static final Color ROW = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;

	private final PopoutManager popouts;
	private final PresetStore presets;
	private final PanelPopoutConfig config;

	/**
	 * Preset being edited, or null for the main view.
	 */
	@Nullable
	private Preset editing;
	private final Set<String> editSelection = new LinkedHashSet<>();
	private String status = "";
	private boolean rebuildQueued;

	@Inject
	PanelPopoutPanel(PopoutManager popouts, PresetStore presets, PanelPopoutConfig config)
	{
		this.popouts = popouts;
		this.presets = presets;
		this.config = config;
		rebuild();
	}

	/**
	 * Coalesces bursts of changes (applying a preset) into one rebuild.
	 */
	void refresh()
	{
		if (rebuildQueued)
		{
			return;
		}
		rebuildQueued = true;
		SwingUtilities.invokeLater(() ->
		{
			rebuildQueued = false;
			rebuild();
		});
	}

	private void rebuild()
	{
		removeAll();
		if (editing != null)
		{
			buildEditView(editing);
		}
		else
		{
			buildMainView();
		}
		revalidate();
		repaint();
	}

	// ---- main view -------------------------------------------------------------------------

	private void buildMainView()
	{
		add(title("Panel Popout"));
		if (!popouts.isAttached())
		{
			add(muted("Waiting for the RuneLite sidebar..."));
			return;
		}

		List<PopoutState> open = popouts.openPopouts();
		add(section("Open windows"));
		if (open.isEmpty())
		{
			add(muted("Nothing is popped out. Right-click (or middle-click) any sidebar icon, or use the button at the top of the icon strip."));
		}
		else
		{
			for (PopoutState s : open)
			{
				add(windowRow(s.getName()));
			}
		}
		JPanel windowButtons = new JPanel(new GridLayout(1, open.isEmpty() ? 1 : 2, 6, 0));
		windowButtons.setOpaque(false);
		JButton popOutMenu = new JButton("Pop out...");
		popOutMenu.setFocusable(false);
		popOutMenu.setToolTipText("Pick a sidebar panel to pop out");
		popOutMenu.addActionListener(e -> popOutMenu().show(popOutMenu, 0, popOutMenu.getHeight()));
		windowButtons.add(popOutMenu);
		if (!open.isEmpty())
		{
			windowButtons.add(button("Pop all in", popouts::popInAll));
		}
		add(windowButtons);

		add(section("Presets"));
		JButton save = button("Save open windows as preset", this::createPreset);
		save.setEnabled(!open.isEmpty());
		save.setToolTipText(open.isEmpty() ? "Pop out some panels and arrange them first" : "Remember these windows and their positions");
		add(save);

		Set<String> openNames = open.stream().map(PopoutState::getName).collect(Collectors.toSet());
		if (presets.all().isEmpty())
		{
			add(muted("A preset opens a set of panels in saved positions and closes all other popouts. Arrange some windows, then save them."));
		}
		List<Preset> all = presets.all();
		for (int i = 0; i < all.size(); i++)
		{
			Preset p = all.get(i);
			add(presetRow(p, i, !openNames.isEmpty() && openNames.equals(names(p))));
		}

		if (!status.isEmpty())
		{
			add(muted(status));
		}
	}

	private JPanel windowRow(String name)
	{
		JPanel row = row();
		row.add(ellipsized(name, Color.WHITE), BorderLayout.CENTER);
		JPanel actions = actions();
		actions.add(iconButton(Icons.popOut(14, MUTED), Icons.popOut(14, Color.WHITE), "Show window", () -> popouts.show(name)));
		actions.add(iconButton(Icons.popIn(14, MUTED), Icons.popIn(14, Color.WHITE), "Pop back in", () -> popouts.popIn(name, false)));
		row.add(actions, BorderLayout.EAST);
		return row;
	}

	private JPanel presetRow(Preset preset, int position, boolean active)
	{
		JPanel row = row();
		if (active)
		{
			row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 3, 0, 0, ColorScheme.BRAND_ORANGE),
				new EmptyBorder(5, 5, 5, 4)));
		}

		String summary = preset.getPanels().isEmpty()
			? "No panels"
			: preset.getPanels().stream().map(PopoutState::getName).collect(Collectors.joining(", "));
		Keybind hotkey = PanelPopoutPlugin.presetHotkey(config, position);
		if (!Keybind.NOT_SET.equals(hotkey))
		{
			summary = hotkey + " · " + summary;
		}
		JPanel text = new JPanel(new GridLayout(2, 1));
		text.setOpaque(false);
		text.add(ellipsized(preset.getName(), Color.WHITE));
		JLabel sub = ellipsized(summary, MUTED);
		sub.setFont(FontManager.getRunescapeSmallFont());
		text.add(sub);
		text.setToolTipText(summary);
		row.add(text, BorderLayout.CENTER);

		JPanel actions = actions();
		JButton open = new JButton("Open");
		open.setFocusable(false);
		open.setToolTipText("Pop out these panels and close all other popouts");
		open.addActionListener(e -> applyPreset(preset));
		open.setEnabled(!preset.getPanels().isEmpty());
		actions.add(open);

		JButton more = iconButton(Icons.more(14, MUTED), Icons.more(14, Color.WHITE), "More", null);
		more.addActionListener(e -> presetMenu(preset).show(more, 0, more.getHeight()));
		actions.add(more);
		row.add(actions, BorderLayout.EAST);
		return row;
	}

	private JPopupMenu presetMenu(Preset preset)
	{
		JPopupMenu menu = new JPopupMenu();
		JMenuItem update = new JMenuItem("Update from open windows");
		update.setEnabled(!popouts.openPopouts().isEmpty());
		update.addActionListener(e ->
		{
			preset.setPanels(popouts.openPopouts());
			presets.save();
			setStatus("Updated \"" + preset.getName() + "\" from " + preset.getPanels().size() + " open windows.");
		});
		menu.add(update);

		JMenuItem edit = new JMenuItem("Choose panels...");
		edit.addActionListener(e -> startEditing(preset));
		menu.add(edit);

		JMenuItem rename = new JMenuItem("Rename...");
		rename.addActionListener(e ->
		{
			String name = askName("Rename preset", preset.getName(), preset);
			if (name != null)
			{
				preset.setName(name);
				presets.save();
				rebuild();
			}
		});
		menu.add(rename);

		int index = presets.indexOf(preset);
		JMenuItem up = new JMenuItem("Move up");
		up.setEnabled(index > 0);
		up.addActionListener(e ->
		{
			presets.move(preset, -1);
			rebuild();
		});
		menu.add(up);
		JMenuItem down = new JMenuItem("Move down");
		down.setEnabled(index >= 0 && index < presets.all().size() - 1);
		down.addActionListener(e ->
		{
			presets.move(preset, 1);
			rebuild();
		});
		menu.add(down);

		menu.addSeparator();
		JMenuItem delete = new JMenuItem("Delete");
		delete.addActionListener(e ->
		{
			int answer = JOptionPane.showConfirmDialog(this, "Delete preset \"" + preset.getName() + "\"?", "Delete preset",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer == JOptionPane.YES_OPTION)
			{
				presets.remove(preset);
				setStatus("");
			}
		});
		menu.add(delete);
		return menu;
	}

	private JPopupMenu popOutMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		List<String> names = popouts.poppableNames();
		if (names.isEmpty())
		{
			JMenuItem none = new JMenuItem("No panels available");
			none.setEnabled(false);
			menu.add(none);
		}
		for (String name : names)
		{
			JMenuItem item = new JMenuItem(name);
			item.addActionListener(e -> popouts.popOut(name));
			menu.add(item);
		}
		return menu;
	}

	private void createPreset()
	{
		List<PopoutState> open = popouts.openPopouts();
		if (open.isEmpty())
		{
			return;
		}
		String name = askName("New preset", "Preset " + (presets.all().size() + 1), null);
		if (name != null)
		{
			presets.add(new Preset(name, open));
			setStatus("Saved \"" + name + "\" with " + open.size() + " windows.");
		}
	}

	private void applyPreset(Preset preset)
	{
		List<String> missing = popouts.applyLayout(preset.getPanels());
		setStatus(missing.isEmpty()
			? ""
			: "Not in the sidebar (plugin turned off?): " + String.join(", ", missing));
	}

	// ---- edit view -------------------------------------------------------------------------

	private void startEditing(Preset preset)
	{
		editing = preset;
		editSelection.clear();
		editSelection.addAll(names(preset));
		rebuild();
	}

	private void buildEditView(Preset preset)
	{
		add(title("Choose panels"));
		add(muted("Tick the panels \"" + preset.getName() + "\" pops out. Saved positions are kept; new panels open next to RuneLite. Use \"Update from open windows\" to save positions."));

		Map<String, Boolean> choices = new LinkedHashMap<>();
		for (String name : popouts.panelNames())
		{
			if (!name.equals(PanelPopoutPlugin.NAME))
			{
				choices.put(name, true);
			}
		}
		for (String name : names(preset))
		{
			choices.putIfAbsent(name, false);
		}

		for (Map.Entry<String, Boolean> choice : choices.entrySet())
		{
			String name = choice.getKey();
			JCheckBox box = new JCheckBox(choice.getValue() ? name : name + " (not loaded)", editSelection.contains(name));
			box.setBackground(ColorScheme.DARK_GRAY_COLOR);
			box.setFocusable(false);
			box.addActionListener(e ->
			{
				if (box.isSelected())
				{
					editSelection.add(name);
				}
				else
				{
					editSelection.remove(name);
				}
			});
			add(box);
		}

		JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
		buttons.setOpaque(false);
		buttons.add(button("Cancel", () ->
		{
			editing = null;
			rebuild();
		}));
		buttons.add(button("Save", () -> finishEditing(preset)));
		add(buttons);
	}

	private void finishEditing(Preset preset)
	{
		List<PopoutState> panels = new ArrayList<>();
		Set<String> kept = new HashSet<>();
		for (PopoutState s : preset.getPanels())
		{
			if (editSelection.contains(s.getName()))
			{
				panels.add(s);
				kept.add(s.getName());
			}
		}
		for (String name : editSelection)
		{
			if (!kept.contains(name))
			{
				panels.add(new PopoutState(name, null, false));
			}
		}
		preset.setPanels(panels);
		presets.save();
		editing = null;
		setStatus("Saved \"" + preset.getName() + "\".");
	}

	// ---- helpers ---------------------------------------------------------------------------

	@Nullable
	private String askName(String title, String initial, @Nullable Preset current)
	{
		Object input = JOptionPane.showInputDialog(this, "Preset name:", title, JOptionPane.PLAIN_MESSAGE, null, null, initial);
		if (input == null)
		{
			return null;
		}
		String name = input.toString().trim();
		if (name.isEmpty())
		{
			return null;
		}
		Preset clash = presets.byName(name);
		if (clash != null && clash != current)
		{
			JOptionPane.showMessageDialog(this, "There's already a preset called \"" + name + "\".", title, JOptionPane.WARNING_MESSAGE);
			return null;
		}
		return name;
	}

	private void setStatus(String text)
	{
		status = text;
		rebuild();
	}

	private static Set<String> names(Preset preset)
	{
		return preset.getPanels().stream().map(PopoutState::getName).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static JLabel title(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(Color.WHITE);
		l.setBorder(new EmptyBorder(0, 0, 4, 0));
		return l;
	}

	private static JLabel section(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(MUTED);
		l.setBorder(new EmptyBorder(8, 0, 0, 0));
		return l;
	}

	private static JLabel muted(String text)
	{
		JLabel l = new JLabel("<html>" + escape(text) + "</html>");
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(MUTED);
		return l;
	}

	private static JLabel ellipsized(String text, Color color)
	{
		JLabel l = new JLabel(text);
		l.setForeground(color);
		l.setToolTipText(text);
		// Let BorderLayout squeeze it; Swing adds "..." when it doesn't fit.
		l.setMinimumSize(new Dimension(0, l.getPreferredSize().height));
		l.setPreferredSize(new Dimension(0, l.getPreferredSize().height));
		return l;
	}

	private static JPanel row()
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ROW);
		row.setBorder(new EmptyBorder(5, 8, 5, 4));
		return row;
	}

	private static JPanel actions()
	{
		JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		p.setOpaque(false);
		return p;
	}

	private static JButton button(String text, Runnable action)
	{
		JButton b = new JButton(text);
		b.setFocusable(false);
		b.addActionListener(e -> action.run());
		return b;
	}

	private static JButton iconButton(Icon icon, Icon hover, String tooltip, @Nullable Runnable action)
	{
		JButton b = new JButton(icon);
		b.setRolloverIcon(hover);
		b.setToolTipText(tooltip);
		decorate(b);
		if (action != null)
		{
			b.addActionListener(e -> action.run());
		}
		return b;
	}

	private static void decorate(AbstractButton b)
	{
		SwingUtil.removeButtonDecorations(b);
		b.setFocusable(false);
		b.setPreferredSize(new Dimension(22, 22));
	}

	private static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
