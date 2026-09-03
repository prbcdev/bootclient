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
package com.atlauncher.gui.card.packbrowser;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Utils;

/**
 * A single universal pack card used by every source (ATLauncher, CurseForge, FTB, Modrinth,
 * Technic, unified search). Fixed size, single vertical stack: title (with platform icon),
 * image, description, buttons - each separated by an identical GAP strut, with image/
 * description/buttons given explicit heights (50%/40%/10% of the space remaining after the
 * title and padding) so there's never any leftover unallocated space between elements.
 */
public class PackCard extends JPanel implements RelocalizationListener {

    public static final int CARD_WIDTH = 260;
    public static final int CARD_HEIGHT = 340;

    private static final int PAD = 8;
    private static final int GAP = 8;
    private static final int TITLE_HEIGHT = 24;
    private static final int TITLE_ICON_SIZE = 16;
    private static final int TITLE_MAX_CHARS = 24;
    private static final int CONTENT_WIDTH = CARD_WIDTH - (PAD * 2);

    // remaining vertical space after the title, outer padding, and the 3 gaps between
    // title/image, image/description and description/buttons
    private static final int INNER_HEIGHT = CARD_HEIGHT - (PAD * 2) - TITLE_HEIGHT - (GAP * 3);

    public static final int IMAGE_HEIGHT = Math.round(INNER_HEIGHT * 0.50f);
    private static final int DESCRIPTION_HEIGHT = Math.round(INNER_HEIGHT * 0.40f);
    // remainder (not a straight 10% calc) so the three heights always sum exactly to INNER_HEIGHT
    private static final int BUTTONS_HEIGHT = INNER_HEIGHT - IMAGE_HEIGHT - DESCRIPTION_HEIGHT;

    private static final Border BOX_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
            BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

    private final JButton installButton = new JButton(GetText.tr("Install"));
    private final JButton websiteButton = new JButton(GetText.tr("Website"));

    /**
     * @param title            the pack's name, shown at the top of the card (truncated with an
     *                         ellipsis if too long - the full name is available as a tooltip)
     * @param image            a pre-built image component that properly scales its drawing to
     *                         its own actual getWidth()/getHeight() (e.g. BackgroundImageLabel,
     *                         or a custom component - NOT PackImagePanel, which always draws at
     *                         a fixed 300x150 regardless of its given bounds and gets clipped)
     * @param descriptionHtml  the description, already rendered as an HTML string
     * @param platformIconKey  key passed to App.THEME.getResourcePath("image/modpack-platform", ...),
     *                         shown next to the title (scaled to match the title text size),
     *                         or null to skip it
     * @param onInstall        called after confirming an account is selected
     * @param websiteUrl       opened by the Website button, or null to hide that button
     */
    public PackCard(String title, JComponent image, String descriptionHtml, String platformIconKey,
            Runnable onInstall, String websiteUrl) {
        super();

        Dimension size = new Dimension(CARD_WIDTH, CARD_HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setBorder(BOX_BORDER);
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        RelocalizationManager.addListener(this);

        add(buildTitle(title, platformIconKey));
        add(Box.createVerticalStrut(GAP));
        add(buildImage(image));
        add(Box.createVerticalStrut(GAP));
        add(buildDescription(descriptionHtml));
        add(Box.createVerticalStrut(GAP));
        add(buildButtons(onInstall, websiteUrl));
    }

    /** Truncates long titles with an ellipsis so the title row's width never varies. */
    private static String truncateTitle(String title) {
        if (title != null && title.length() > TITLE_MAX_CHARS) {
            return title.substring(0, TITLE_MAX_CHARS - 1).trim() + "…";
        }
        return title;
    }

    /** Title row: platform icon (scaled to match the title font size) + title text, centered. */
    private JComponent buildTitle(String title, String platformIconKey) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension titleSize = new Dimension(CONTENT_WIDTH, TITLE_HEIGHT);
        row.setPreferredSize(titleSize);
        row.setMaximumSize(titleSize);

        if (platformIconKey != null) {
            ImageIcon rawIcon = Utils.getIconImage(App.THEME.getResourcePath("image/modpack-platform",
                    platformIconKey));
            ImageIcon scaledIcon = scaleIcon(rawIcon, TITLE_ICON_SIZE);
            if (scaledIcon != null) {
                row.add(new JLabel(scaledIcon));
            }
        }

        JLabel titleLabel = new JLabel(truncateTitle(title));
        titleLabel.setFont(App.THEME.getBoldFont().deriveFont(14f));
        titleLabel.setToolTipText(title);
        row.add(titleLabel);

        return row;
    }

    /**
     * Explicitly scales an icon's underlying image to the target size - JLabel doesn't scale
     * icons to fit on its own, it just paints them at native resolution and clips whatever
     * doesn't fit, which is what caused some platform icons (larger source images than others)
     * to look zoomed-in/cropped before.
     */
    private static ImageIcon scaleIcon(ImageIcon icon, int size) {
        if (icon == null) {
            return null;
        }
        Image scaled = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /**
     * Image, stretched to fill a fixed-size slot (50% of the card's inner content height).
     * Requires that `image` actually scales its own painting to whatever bounds it's given
     * (BackgroundImageLabel does this correctly) - components that draw at a fixed internal
     * size regardless of their bounds (like PackImagePanel) will get clipped instead of scaled.
     */
    private JComponent buildImage(JComponent image) {
        Dimension imgSize = new Dimension(CONTENT_WIDTH, IMAGE_HEIGHT);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(imgSize);
        wrapper.setMaximumSize(imgSize);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        wrapper.add(image, gbc);

        return wrapper;
    }

    /** Fixed-height scrollable description area (40% of the card's inner content height). */
    private JComponent buildDescription(String descriptionHtml) {
        JEditorPane descArea = new JEditorPane("text/html", descriptionHtml == null ? "" : descriptionHtml);
        descArea.setEditable(false);
        descArea.setFocusable(false);
        descArea.setHighlighter(null);
        descArea.setOpaque(false);
        descArea.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                OS.openWebBrowser(e.getURL());
            }
        });

        JScrollPane scroll = new JScrollPane(descArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        Dimension descSize = new Dimension(CONTENT_WIDTH, DESCRIPTION_HEIGHT);
        scroll.setPreferredSize(descSize);
        scroll.setMaximumSize(descSize);
        return scroll;
    }

    /** Fixed-height Install/Website row (10% of the card's inner content height). */
    private JComponent buildButtons(Runnable onInstall, String websiteUrl) {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP, 0));
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.setOpaque(false);

        Dimension buttonsSize = new Dimension(CONTENT_WIDTH, BUTTONS_HEIGHT);
        buttons.setPreferredSize(buttonsSize);
        buttons.setMaximumSize(buttonsSize);

        buttons.add(installButton);
        if (websiteUrl != null) {
            buttons.add(websiteButton);
            websiteButton.addActionListener(e -> OS.openWebBrowser(websiteUrl));
        }

        installButton.addActionListener(e -> {
            if (AccountManager.getSelectedAccount() == null) {
                DialogManager.okDialog().setTitle(GetText.tr("No Account Selected"))
                        .setContent(GetText.tr("Cannot create instance as you have no account selected."))
                        .setType(DialogManager.ERROR).show();

                if (AccountManager.getAccounts().isEmpty()) {
                    App.navigate(UIConstants.LAUNCHER_ACCOUNTS_TAB);
                }
            } else {
                onInstall.run();
            }
        });

        return buttons;
    }

    @Override
    public void onRelocalization() {
        installButton.setText(GetText.tr("Install"));
        websiteButton.setText(GetText.tr("Website"));
    }
}
