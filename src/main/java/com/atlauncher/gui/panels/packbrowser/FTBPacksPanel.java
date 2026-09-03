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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.JPanel;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.ftb.FTBPackArt;
import com.atlauncher.data.ftb.FTBPackArtType;
import com.atlauncher.data.ftb.FTBPackManifest;
import com.atlauncher.data.minecraft.VersionManifestVersion;
import com.atlauncher.data.minecraft.VersionManifestVersionType;
import com.atlauncher.gui.card.NilCard;
import com.atlauncher.gui.card.packbrowser.PackCard;
import com.atlauncher.gui.components.BackgroundImageLabel;
import com.atlauncher.gui.dialogs.InstanceInstallerDialog;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.network.Analytics;
import com.atlauncher.network.analytics.AnalyticsEvent;
import com.atlauncher.utils.FTBApi;
import com.atlauncher.utils.Markdown;

public class FTBPacksPanel extends PackBrowserPlatformPanel {
    boolean hasMorePages = true;

    private PackCard buildCard(FTBPackManifest pack) {
        String imageUrl = null;
        if (pack.art != null) {
            Optional<FTBPackArt> art = pack.art.stream()
                    .filter(a -> a.type == FTBPackArtType.LOGO || a.type == FTBPackArtType.SQUARE)
                    .sorted(Comparator.comparingInt((FTBPackArt a) -> a.updated).reversed()).findFirst();
            if (art.isPresent()) {
                imageUrl = art.get().url;
            }
        }

        BackgroundImageLabel image = new BackgroundImageLabel(imageUrl, PackCard.CARD_WIDTH, PackCard.IMAGE_HEIGHT);

        String description = String.format("<html>%s</html>", Markdown.render(pack.description));

        // The Feed The Beast website only displays modpacks with the 'FTB' tag present,
        // so we should hide the Website button for packs without the tag.
        String websiteUrl = pack.hasTag("FTB") ? pack.getWebsiteUrl() : null;

        return new PackCard(pack.name, image, description, "ftb",
                () -> {
                    Analytics.trackEvent(AnalyticsEvent.forPackInstall(pack));
                    new InstanceInstallerDialog(pack).setVisible(true);
                },
                websiteUrl);
    }

    @Override
    protected void loadPacks(JPanel contentPanel, String minecraftVersion, String category, String sort,
            boolean sortDescending, String search, int page) {
        List<FTBPackManifest> packs;

        if (search == null || search.isEmpty()) {
            packs = FTBApi.getModPacks(page, sort);
        } else {
            packs = FTBApi.searchModPacks(search, page);
        }

        hasMorePages = packs != null && packs.size() == Constants.CURSEFORGE_PAGINATION_SIZE;

        contentPanel.removeAll();

        if (packs == null || packs.size() == 0) {
            contentPanel.add(
                    new NilCard(new HTMLBuilder().text(GetText
                            .tr("There are no packs to display.<br/><br/>Try removing your search query and try again."))
                            .build()));
            return;
        }

        for (FTBPackManifest pack : packs) {
            contentPanel.add(buildCard(pack));
        }
    }

    @Override
    public void loadMorePacks(JPanel contentPanel, String minecraftVersion, String category, String sort,
            boolean sortDescending, String search, int page) {
        List<FTBPackManifest> packs;

        if (search == null || search.isEmpty()) {
            packs = FTBApi.getModPacks(page, sort);
        } else {
            packs = FTBApi.searchModPacks(search, page);
        }

        hasMorePages = packs != null && packs.size() == Constants.FTB_PAGINATION_SIZE;

        if (packs != null) {
            for (FTBPackManifest pack : packs) {
                contentPanel.add(buildCard(pack));
            }
        }
    }

    @Override
    public String getPlatformName() {
        return "FTB";
    }

    @Override
    public String getAnalyticsCategory() {
        return "FTBPack";
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
    public boolean supportsSortOrder() {
        return false;
    }

    @Override
    public Map<String, String> getSortFields() {
        Map<String, String> sortFields = new LinkedHashMap<>();

        sortFields.put("popular/plays", GetText.tr("Most Popular"));
        sortFields.put("popular/installs", GetText.tr("Most Installed"));
        sortFields.put("updated", GetText.tr("Recently Updated"));
        sortFields.put("featured", GetText.tr("Featured"));

        return sortFields;
    }

    @Override
    public Map<String, Boolean> getSortFieldsDefaultOrder() {
        return new LinkedHashMap<>();
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
    public boolean hasPagination() {
        return true;
    }

    @Override
    public boolean hasMorePages() {
        return hasMorePages;
    }

    @Override
    public boolean supportsManualAdding() {
        return false;
    }

    @Override
    public void addById(String id) {}

    @Override
    public String getPlatformMessage() {
        return ConfigManager.getConfigItem("platforms.ftb.message", null);
    }
}
