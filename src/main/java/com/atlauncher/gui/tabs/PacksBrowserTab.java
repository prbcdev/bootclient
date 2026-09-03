/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.listener.TabChangeListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.evnt.manager.TabChangeManager;
import com.atlauncher.gui.WheelScrollLayerUI;
import com.atlauncher.gui.layouts.WrapLayout;
import com.atlauncher.gui.panels.packbrowser.UnifiedPacksPanel;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.formdev.flatlaf.icons.FlatSearchIcon;

/**
 * Single unified pack browser - no more per-platform sub-tabs (Search/ATLauncher/CurseForge/
 * FTB/Modrinth/Technic). Every card already shows its own source's platform icon next to its
 * title (see PackCard), so a single search across all sources covers what those separate tabs
 * used to provide, without the extra navigation. Cards are centered as a grid (WrapLayout with
 * FlowLayout.CENTER) so leftover space splits evenly on both sides instead of collecting on
 * one side.
 */
public final class PacksBrowserTab extends JPanel
    implements Tab, RelocalizationListener, TabChangeListener {

    // grid spacing between pack cards, consistent with the rest of the app's GAP constant
    private static final int GRID_GAP = 8;

    private final JPanel actionsPanel = new JPanel();
    private final JTextField searchField = new JTextField(16);

    private final UnifiedPacksPanel packsPanel = new UnifiedPacksPanel();

    private JScrollPane scrollPane;
    private JLayer<JScrollPane> layerForScrollPane;
    // WrapLayout arranges fixed-size pack cards left-to-right, wrapping to a new row
    // automatically based on available width. CENTER alignment (vs LEFT) means each row's
    // leftover space is split evenly on both sides instead of collecting entirely on the right.
    private final JPanel contentPanel = new JPanel(new WrapLayout(FlowLayout.CENTER, GRID_GAP, GRID_GAP));

    private boolean loaded = false;
    private boolean loading = false;

    public PacksBrowserTab() {
        super(new BorderLayout());
        setName("packsBrowserPanel");
        RelocalizationManager.addListener(this);

        initComponents();
    }

    private void initComponents() {
        actionsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        actionsPanel
            .setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Component.borderColor")));
        actionsPanel.setPreferredSize(new Dimension(0, 34));

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    executeSearch();
                }
            }
        });
        searchField.putClientProperty("JTextField.placeholderText", GetText.tr("Search"));
        searchField.putClientProperty("JTextField.leadingIcon", new FlatSearchIcon());
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.putClientProperty("JTextField.clearCallback", (Runnable) () -> {
            searchField.setText("");
            executeSearch();
        });
        actionsPanel.add(searchField);

        add(actionsPanel, BorderLayout.NORTH);

        // scrollpane

        scrollPane = new JScrollPane(contentPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        layerForScrollPane = new JLayer<>(scrollPane, new WheelScrollLayerUI());

        add(layerForScrollPane, BorderLayout.CENTER);

        TabChangeManager.addListener(this);
    }

    private void executeSearch() {
        loading = true;
        searchField.setEnabled(false);

        if (!searchField.getText().isEmpty()) {
            Analytics.trackEvent(
                AnalyticsEvent.forSearchEventPlatform("add_pack", searchField.getText(), 1,
                    packsPanel.getPlatformName()));
        }

        load();
    }

    private void load() {
        loaded = true;

        new Thread(() -> {
            packsPanel.load(contentPanel, null, null, null, false, searchField.getText(), 1);

            SwingUtilities.invokeLater(() -> {
                scrollPane.getVerticalScrollBar().setValue(0);
                loading = false;
                searchField.setEnabled(true);
            });

            revalidate();
            repaint();
        }).start();
    }

    public void reload() {
        searchField.setText("");
        load();
    }

    public void refresh() {
    }

    @Override
    public String getTitle() {
        return GetText.tr("Packs");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Unified ModPack Search";
    }

    @Override
    public void onRelocalization() {
        searchField.putClientProperty("JTextField.placeholderText", GetText.tr("Search"));
    }

    @Override
    public void onTabChange(int tabIndex) {
        if (!loaded && !loading) {
            Analytics.sendScreenView("Unified ModPack Search");
            load();
        }
    }
}
