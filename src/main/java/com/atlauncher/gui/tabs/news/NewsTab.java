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
package com.atlauncher.gui.tabs.news;

import java.awt.BorderLayout;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.gui.tabs.Tab;
import com.atlauncher.utils.OS;
import com.atlauncher.viewmodel.base.INewsViewModel;
import com.atlauncher.viewmodel.impl.NewsViewModel;

/**
 * Slimmed-down news panel: fetches and displays the latest news as scrollable HTML.
 * Deliberately uses no custom HTMLEditorKit/StyleSheet - same plain "text/html" JEditorPane
 * approach as the License / Third Party Libraries tabs, so link color and font formatting
 * stay consistent with the rest of the app instead of being overridden by a separate stylesheet.
 */
public class NewsTab extends HierarchyPanel implements Tab {
    private INewsViewModel viewModel;
    private JEditorPane newsPane;

    public NewsTab() {
        super(new BorderLayout());
    }

    @Override
    protected void createViewModel() {
        viewModel = new NewsViewModel();
    }

    @Override
    protected void onShow() {
        newsPane = new JEditorPane("text/html", GetText.tr("Loading news..."));
        newsPane.setEditable(false);
        newsPane.setFocusable(false);
        newsPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                OS.openWebBrowser(e.getURL());
            }
        });

        JScrollPane scrollPane = new JScrollPane(newsPane,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        addDisposable(viewModel.getNewsHTML().subscribe(html -> {
            if (html.isPresent()) {
                newsPane.setText(html.get());
                newsPane.setCaretPosition(0);
            }
        }));
    }

    @Override
    protected void onDestroy() {
        newsPane = null;
        removeAll();
    }

    @Override
    public String getTitle() {
        return GetText.tr("News");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "News";
    }
}
