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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.gui.tabs.settings.BackupsAndLoggingSettingsTab;
import com.atlauncher.gui.tabs.settings.CommandsSettingsTab;
import com.atlauncher.gui.tabs.settings.EnvironmentVariablesTab;
import com.atlauncher.gui.tabs.settings.GeneralSettingsTab;
import com.atlauncher.gui.tabs.settings.JavaSettingsTab;
import com.atlauncher.gui.tabs.settings.ModsSettingsTab;
import com.atlauncher.gui.tabs.settings.NetworkSettingsTab;
import com.atlauncher.network.Analytics;
import com.atlauncher.viewmodel.impl.settings.BackupsSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.CommandsSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.EnvironmentVariablesViewModel;
import com.atlauncher.viewmodel.impl.settings.GeneralSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.JavaSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.LoggingSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.ModsSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.NetworkSettingsViewModel;
import com.atlauncher.viewmodel.impl.settings.SettingsViewModel;

/**
 * Settings tab: sub-tabs sit in a custom left-hand vertical button sidebar paired with a
 * CardLayout content area. Backups and Logging are combined into one distinct sidebar entry
 * (see BackupsAndLoggingSettingsTab) rather than each having their own, or being folded into
 * General.
 */
public class SettingsTab extends HierarchyPanel implements Tab {

    // same spacing constants as HomeTab, kept consistent across the app
    private static final int GAP = 8;
    private static final int PAD = 8;

    private static final int SIDEBAR_WIDTH = 220;
    private static final int SIDEBAR_BUTTON_HEIGHT = 44;
    private static final float SIDEBAR_FONT_SIZE = 17.0F;

    private static final Border BOX_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
        BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

    // regular bordered outline for sidebar/save buttons, matching the box style
    private static final Border BUTTON_BORDER_NORMAL = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
        BorderFactory.createEmptyBorder(8, 12, 8, 12));
    // same outline, but in the theme's green accent, used on the currently selected sub-tab
    private static final Border BUTTON_BORDER_SELECTED = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(accentColor(), 2),
        BorderFactory.createEmptyBorder(7, 11, 7, 11));

    private static Color accentColor() {
        // TabbedPane.underlineColor is explicitly set to the theme's green primary color
        // (see Dark.properties) - Component.accentColor/focusColor are FlatLaf's generic
        // defaults and stay blue unless separately overridden, so this is the reliable source.
        Color themeGreen = UIManager.getColor("TabbedPane.underlineColor");
        if (themeGreen != null) {
            return themeGreen;
        }

        Color accent = UIManager.getColor("Component.accentColor");
        return accent != null ? accent : UIManager.getColor("Component.focusColor");
    }

    @Nullable
    private JButton saveButton;

    private SettingsViewModel viewModel;

    // state maintained at the top level for all sub-tabs
    private BackupsSettingsViewModel backupSettingsViewModel;
    private CommandsSettingsViewModel commandsSettingsViewModel;
    private GeneralSettingsViewModel generalSettingsViewModel;
    private JavaSettingsViewModel javaSettingsViewModel;
    private EnvironmentVariablesViewModel environmentVariablesViewModel;
    private LoggingSettingsViewModel loggingSettingsViewModel;
    private ModsSettingsViewModel modsSettingsViewModel;
    private NetworkSettingsViewModel networkSettingsViewModel;

    @Nullable
    private List<Tab> tabs;
    @Nullable
    private List<JButton> sidebarButtons;
    @Nullable
    private CardLayout cardLayout;
    @Nullable
    private JPanel cardPanel;

    private int selectedTabIndex = 0;

    public SettingsTab() {
        setLayout(new BorderLayout());
    }

    @Override
    protected void createViewModel() {
        viewModel = new SettingsViewModel();

        backupSettingsViewModel = new BackupsSettingsViewModel();
        commandsSettingsViewModel = new CommandsSettingsViewModel();
        generalSettingsViewModel = new GeneralSettingsViewModel();
        javaSettingsViewModel = new JavaSettingsViewModel();
        environmentVariablesViewModel = new EnvironmentVariablesViewModel();
        loggingSettingsViewModel = new LoggingSettingsViewModel();
        modsSettingsViewModel = new ModsSettingsViewModel();
        networkSettingsViewModel = new NetworkSettingsViewModel();
    }

    @SuppressWarnings("null")
    @Override
    protected void onShow() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));

        container.add(buildSettingsRow(), BorderLayout.CENTER);

        add(container, BorderLayout.CENTER);
    }

    /** Two separate boxed panels side by side: the sidebar, and the settings content area. */
    private JPanel buildSettingsRow() {
        JPanel row = new JPanel(new BorderLayout(GAP, 0));

        tabs = Arrays.asList(
            new GeneralSettingsTab(generalSettingsViewModel),
            new ModsSettingsTab(modsSettingsViewModel),
            new JavaSettingsTab(javaSettingsViewModel),
            new NetworkSettingsTab(networkSettingsViewModel),
            new BackupsAndLoggingSettingsTab(backupSettingsViewModel, loggingSettingsViewModel),
            new CommandsSettingsTab(commandsSettingsViewModel),
            new EnvironmentVariablesTab(environmentVariablesViewModel));

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        for (int i = 0; i < tabs.size(); i++) {
            cardPanel.add((JPanel) tabs.get(i), String.valueOf(i));
        }

        JPanel contentBox = new JPanel(new BorderLayout());
        contentBox.setBorder(BOX_BORDER);
        contentBox.add(cardPanel, BorderLayout.CENTER);

        row.add(buildSidebarBox(), BorderLayout.WEST);
        row.add(contentBox, BorderLayout.CENTER);

        selectTab(selectedTabIndex);

        return row;
    }

    /**
     * Its own boxed panel: sub-tab buttons stacked top-down, then a single glue that pushes
     * Save to the very bottom of the same box - dense, no separate bottom bar needed.
     */
    private JPanel buildSidebarBox() {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BOX_BORDER);
        box.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.PAGE_AXIS));
        list.setOpaque(false);

        sidebarButtons = new ArrayList<>();
        for (int i = 0; i < tabs.size(); i++) {
            int index = i;
            JButton button = new JButton(tabs.get(i).getTitle());
            styleSidebarButton(button);
            button.addActionListener(e -> selectTab(index));

            sidebarButtons.add(button);
            list.add(button);
            list.add(Box.createVerticalStrut(GAP));
        }

        // pushes Save to the bottom of the box, filling whatever space is left
        list.add(Box.createVerticalGlue());

        saveButton = new JButton(GetText.tr("Save"));
        styleSidebarButton(saveButton);
        addDisposable(viewModel.getSaveEnabled().subscribe(saveButton::setEnabled));
        saveButton.addActionListener(e -> viewModel.save());
        list.add(saveButton);

        box.add(list, BorderLayout.CENTER);
        return box;
    }

    /** Applies the shared fixed size, font, and default (unselected) bordered outline. */
    private void styleSidebarButton(JButton button) {
        button.setFont(App.THEME.getNormalFont().deriveFont(SIDEBAR_FONT_SIZE));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        button.setBorder(BUTTON_BORDER_NORMAL);

        Dimension size = new Dimension(SIDEBAR_WIDTH - (PAD * 2), SIDEBAR_BUTTON_HEIGHT);
        button.setPreferredSize(size);
        button.setMaximumSize(size);
        button.setMinimumSize(size);
    }

    /** Switches the visible card and updates which sidebar button shows the accent outline. */
    private void selectTab(int index) {
        selectedTabIndex = index;
        cardLayout.show(cardPanel, String.valueOf(index));

        for (int i = 0; i < sidebarButtons.size(); i++) {
            sidebarButtons.get(i).setBorder(i == index ? BUTTON_BORDER_SELECTED : BUTTON_BORDER_NORMAL);
        }

        Analytics.sendScreenView(tabs.get(index).getAnalyticsScreenViewName() + " Settings");
    }

    @Override
    protected void onDestroy() {
        removeAll();
        saveButton = null;
        tabs = null;
        sidebarButtons = null;
        cardLayout = null;
        cardPanel = null;
    }

    @Override
    public String getTitle() {
        return GetText.tr("Settings");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        // since this is the default, this is the main view name
        return "General Settings";
    }
}
