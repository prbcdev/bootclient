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

import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.joda.time.format.ISODateTimeFormat;
import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.Pack;
import com.atlauncher.data.minecraft.VersionManifestVersion;
import com.atlauncher.data.minecraft.VersionManifestVersionType;
import com.atlauncher.gui.card.NilCard;
import com.atlauncher.gui.card.packbrowser.PackCard;
import com.atlauncher.gui.dialogs.InstanceInstallerDialog;
import com.atlauncher.managers.PackManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;

public class ATLauncherPacksPanel extends PackBrowserPlatformPanel {
    private final List<Pack> packs = new ArrayList<>();
    private final List<PackCard> cards = new ArrayList<>();

    private void loadPacksToShow(String minecraftVersion, String sort, boolean sortDescending, String searchText) {
        List<Pack> packs = sort.equalsIgnoreCase("name") ? PackManager.getPacksSortedAlphabetically(false,
                sortDescending)
                : PackManager.getPacksSortedPositionally(false, sortDescending);

        this.packs.addAll(packs.stream().filter(Pack::canInstall).filter(pack -> {
            if (minecraftVersion != null) {
                return pack.versions.stream().anyMatch(pv -> pv.minecraftVersion.id.equals(minecraftVersion));
            }

            return true;
        }).filter(pack -> {
            if (!searchText.isEmpty()) {
                return (pack.getDescription() != null
                        && Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE)
                                .matcher(pack.getDescription()).find())
                        || Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE).matcher(pack.getName())
                                .find();
            }

            return true;
        }).collect(Collectors.toList()));
    }

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

    private PackCard buildCard(Pack pack) {
        String description = new HTMLBuilder().text(pack.getDescription()).build();
        return new PackCard(pack.name, buildPackImage(pack), description, "atlauncher",
                () -> {
                    Analytics.trackEvent(AnalyticsEvent.forPackInstall(pack));
                    new InstanceInstallerDialog(pack).setVisible(true);
                },
                pack.getWebsiteURL());
    }

    @Override
    protected void loadPacks(JPanel contentPanel, String minecraftVersion, String category, String sort,
            boolean sortDescending, String search, int page) {
        contentPanel.removeAll();
        this.packs.clear();
        this.cards.clear();
        loadPacksToShow(minecraftVersion, sort, sortDescending, search);

        loadMorePacks(contentPanel, minecraftVersion, category, sort, sortDescending, search, page);
    }

    @Override
    public void loadMorePacks(JPanel contentPanel, String minecraftVersion, String category, String sort,
            boolean sortDescending, String search,
            int page) {
        this.packs.stream().skip(this.cards.size()).limit(10)
                .forEach(pack -> this.cards.add(buildCard(pack)));

        contentPanel.removeAll();

        for (PackCard card : this.cards) {
            contentPanel.add(card);
        }

        if (this.cards.isEmpty()) {
            contentPanel.add(
                    new NilCard(new HTMLBuilder().text(GetText
                            .tr("There are no packs to display.<br/><br/>Try removing your search query and try again."))
                            .build()));
        }
    }

    @Override
    public String getPlatformName() {
        return "ATLauncher";
    }

    @Override
    public String getAnalyticsCategory() {
        return "Pack";
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
        return true;
    }

    @Override
    public Map<String, String> getSortFields() {
        Map<String, String> sortFields = new LinkedHashMap<>();

        sortFields.put("popular", GetText.tr("Popularity"));
        sortFields.put("name", GetText.tr("Name"));

        return sortFields;
    }

    @Override
    public Map<String, Boolean> getSortFieldsDefaultOrder() {
        // Sort field / if in descending order
        Map<String, Boolean> sortFieldsOrder = new LinkedHashMap<>();

        sortFieldsOrder.put("popular", false);
        sortFieldsOrder.put("name", false);

        return sortFieldsOrder;
    }

    @Override
    public boolean supportsSortOrder() {
        return true;
    }

    @Override
    public boolean supportsMinecraftVersionFiltering() {
        return true;
    }

    @Override
    public List<VersionManifestVersionType> getSupportedMinecraftVersionTypesForFiltering() {
        List<VersionManifestVersionType> supportedTypes = new ArrayList<>();

        return supportedTypes;
    }

    @Override
    public List<VersionManifestVersion> getSupportedMinecraftVersionsForFiltering() {
        List<VersionManifestVersion> minecraftVersions = new ArrayList<>();

        PackManager.getPacks().forEach(p -> minecraftVersions
                .addAll(p.versions.stream().map(v -> v.minecraftVersion).distinct().collect(Collectors.toList())));

        return minecraftVersions
                .stream().distinct().sorted(Comparator.comparingLong((VersionManifestVersion mv) -> ISODateTimeFormat
                        .dateTimeParser().parseDateTime(mv.releaseTime).getMillis() / 1000).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public boolean supportsManualAdding() {
        return false;
    }

    @Override
    public void addById(String id) {}

    @Override
    public boolean hasPagination() {
        return true;
    }

    @Override
    public boolean hasMorePages() {
        // already loaded in all the cards possible, so don't navigate
        return this.packs.isEmpty() || this.cards.isEmpty() || this.packs.size() != this.cards.size();
    }

    @Override
    public String getPlatformMessage() {
        return null;
    }
}
