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
package com.atlauncher.gui.panels.packbrowser;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.Pack;
import com.atlauncher.data.minecraft.VersionManifestVersion;
import com.atlauncher.data.minecraft.VersionManifestVersionType;
import com.atlauncher.exceptions.InvalidPack;
import com.atlauncher.graphql.UnifiedModPackHomeQuery;
import com.atlauncher.graphql.UnifiedModPackSearchQuery;
import com.atlauncher.graphql.fragment.UnifiedModPackResultsFragment;
import com.atlauncher.graphql.type.ModPackPlatformType;
import com.atlauncher.gui.card.NilCard;
import com.atlauncher.gui.card.packbrowser.PackCard;
import com.atlauncher.gui.components.BackgroundImageLabel;
import com.atlauncher.gui.dialogs.InstanceInstallerDialog;
import com.atlauncher.managers.PackManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.GraphqlClient;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.utils.Markdown;

public class UnifiedPacksPanel extends PackBrowserPlatformPanel {

    /**
     * A properly-scaling image component for a Pack's icon - unlike PackImagePanel (which
     * always draws at a fixed 300x150 regardless of its actual bounds and gets clipped when
     * resized), this scales its drawing to whatever size it's actually given, matching how
     * BackgroundImageLabel behaves for every other source.
     */
    private static JComponent buildPackImage(Pack pack) {
        Image image = pack.getImage().getImage();
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private JComponent buildImage(UnifiedModPackResultsFragment result) {
        if (result.platform() == ModPackPlatformType.ATLAUNCHER) {
            try {
                return buildPackImage(PackManager.getPackByID(Integer.parseInt(result.id())));
            } catch (InvalidPack | NumberFormatException e) {
                // fall through to a plain image label below
            }
        }

        BackgroundImageLabel imageLabel = new BackgroundImageLabel(result.iconUrl(), PackCard.CARD_WIDTH,
                PackCard.IMAGE_HEIGHT);
        imageLabel.setPreferredSize(new Dimension(PackCard.CARD_WIDTH, PackCard.IMAGE_HEIGHT));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        return imageLabel;
    }

    private PackCard buildCard(UnifiedModPackResultsFragment result) {
        String description = String.format("<html>%s</html>", Markdown.render(result.summary()));
        String platformIconKey = result.platform().toString().toLowerCase(Locale.ENGLISH);

        return new PackCard(result.name(), buildImage(result), description, platformIconKey,
                () -> {
                    Analytics.trackEvent(AnalyticsEvent.forPackInstall(result));
                    new InstanceInstallerDialog(result, false).setVisible(true);
                },
                result.url());
    }

    @Override
    protected void loadPacks(JPanel contentPanel, String minecraftVersion, String category, String sort,
            boolean sortDescending, String search, int page) {
        if (search.length() == 1) {
            contentPanel.removeAll();
            contentPanel.add(
                    new NilCard(new HTMLBuilder().text(GetText
                            .tr("To find a pack, search for one at the top.<br/><br/>Alternatively choose a modpack platform on the left hand side."))
                            .build()));
            return;
        }

        List<UnifiedModPackResultsFragment> items = new ArrayList<>();
        if (search.isEmpty()) {
            UnifiedModPackHomeQuery.Data response = GraphqlClient
                    .callAndWait(new UnifiedModPackHomeQuery());

            if (response != null && response.unifiedModPackHome() != null) {
                items.addAll(
                        response.unifiedModPackHome().stream().map(i -> i.fragments().unifiedModPackResultsFragment())
                                .collect(Collectors.toList()));
            }

        } else {
            UnifiedModPackSearchQuery.Data response = GraphqlClient
                    .callAndWait(new UnifiedModPackSearchQuery(search));

            if (response != null && response.unifiedModPackSearch() != null) {
                items.addAll(
                        response.unifiedModPackSearch().stream().map(i -> i.fragments().unifiedModPackResultsFragment())
                                .collect(Collectors.toList()));
            }
        }

        contentPanel.removeAll();

        if (items.isEmpty()) {
            contentPanel.add(
                    new NilCard(new HTMLBuilder().text(GetText
                            .tr("There are no packs to display.<br/><br/>Try another search query or choose a platform on the left hand side."))
                            .build()));
            return;
        }

        for (UnifiedModPackResultsFragment result : items) {
            contentPanel.add(buildCard(result));
        }
    }

    @Override
    public void loadMorePacks(JPanel contentPanel, String minecraftVersion, String category, String sort,
            boolean sortDescending, String search, int page) {
        // no pagination
    }

    @Override
    public String getPlatformName() {
        return "UnifiedModPackSearch";
    }

    @Override
    public String getAnalyticsCategory() {
        return "UnifiedPack";
    }

    @Override
    public boolean supportsSearch() {
        return true;
    }

    @Override
    public boolean hasCategories() {
        return false;
    }

    @Override
    public Map<String, String> getCategoryFields() {
        return new LinkedHashMap<>();
    }

    @Override
    public boolean hasSort() {
        return false;
    }

    @Override
    public Map<String, String> getSortFields() {
        return new LinkedHashMap<>();
    }

    @Override
    public Map<String, Boolean> getSortFieldsDefaultOrder() {
        return new LinkedHashMap<>();
    }

    @Override
    public boolean supportsSortOrder() {
        return false;
    }

    @Override
    public boolean supportsMinecraftVersionFiltering() {
        return false;
    }

    @Override
    public List<VersionManifestVersionType> getSupportedMinecraftVersionTypesForFiltering() {
        List<VersionManifestVersionType> supportedTypes = new ArrayList<>();

        return supportedTypes;
    }

    @Override
    public List<VersionManifestVersion> getSupportedMinecraftVersionsForFiltering() {
        List<VersionManifestVersion> supportedTypes = new ArrayList<>();

        return supportedTypes;
    }

    @Override
    public boolean supportsManualAdding() {
        return false;
    }

    @Override
    public void addById(String id) {}

    @Override
    public boolean hasPagination() {
        return false;
    }

    @Override
    public boolean hasMorePages() {
        return false;
    }

    @Override
    public String getPlatformMessage() {
        return null;
    }
}
