//merged accounts + about tab into a homepage
package com.atlauncher.gui.tabs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ItemEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.Contributor;
import com.atlauncher.data.MicrosoftAccount;
import com.atlauncher.gui.dialogs.LoginWithMicrosoftDialog;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.gui.panels.HierarchyPanel;
import com.atlauncher.gui.tabs.news.NewsTab;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.themes.ATLauncherLaf;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.SkinUtils;
import com.atlauncher.utils.Utils;
import com.atlauncher.viewmodel.base.IAboutTabViewModel;
import com.atlauncher.viewmodel.base.IAccountsViewModel;
import com.atlauncher.viewmodel.impl.AboutTabViewModel;
import com.atlauncher.viewmodel.impl.AccountsViewModel;

/**
 * Merged homepage tab. Left side: account login/skin/switcher (from AccountsTab).
 * Right side: launcher info/contributors (top box) + News/License/Third Party Libraries
 * tabs (bottom box), split 50/50, matching the left column's box styling.
 */
public class HomeTab extends HierarchyPanel implements Tab {

    // ---- shared spacing constants, used everywhere for consistency ----
    private static final int GAP = 8;   // space between boxes / window edges
    private static final int PAD = 8;   // internal padding inside each box

    // ---- contributors: simple two-column list of name links, no avatars/cards ----
    private static final int CONTRIBUTOR_COLUMNS = 2;
    private static final int CONTRIBUTOR_COLUMN_GAP = 24;
    private static final int CONTRIBUTOR_ROW_GAP = 4;

    // shared box border style: faint line + internal padding
    private static final Border BOX_BORDER = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
        BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

    // ---- accounts (left side) ----
    private IAccountsViewModel accountsViewModel;
    private JEditorPane infoTextPane;
    private JLabel userSkin;
    private JComboBox<ComboItem<String>> accountsComboBox;
    private JButton deleteButton;
    private CardLayout deleteButtonCardLayout;
    private JPanel deleteButtonHolder;
    private JButton loginWithMicrosoftButton;
    private JMenuItem refreshAccessTokenMenuItem;
    private JPopupMenu contextMenu;

    // ---- about (right side) ----
    private IAboutTabViewModel aboutViewModel;
    private JPanel authorsList;

    public HomeTab() {
        super(new BorderLayout());
    }

    @Override
    protected void createViewModel() {
        accountsViewModel = new AccountsViewModel();
        aboutViewModel = new AboutTabViewModel();
    }

    @Override
    protected void onShow() {
        JPanel container = new JPanel(new GridBagLayout());
        container.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.weighty = 1;

        c.gridx = 0;
        c.weightx = 0.18;
        c.insets = new Insets(0, 0, 0, GAP / 2);
        container.add(buildAccountsPanel(), c);

        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 0);
        container.add(new JSeparator(SwingConstants.VERTICAL), c);

        c.gridx = 2;
        c.weightx = 0.82;
        c.insets = new Insets(0, GAP / 2, 0, 0);
        container.add(buildAboutColumn(), c);

        add(container, BorderLayout.CENTER);

        observeAccounts();
        // restore whichever account is currently active, rather than always
        // defaulting to "Add An Account" on tab load
        selectPersistedAccount();
    }

    // ================= shared helpers =================

    /** A JPanel using the standard box border (faint line + PAD internal padding). */
    private JPanel boxedPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BOX_BORDER);
        return panel;
    }

    /** A thin horizontal rule, capped at 3px tall, spanning the full width. */
    private JSeparator thinSeparator() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        return sep;
    }

    /** Appends a "title + glue + thin separator" header row to a PAGE_AXIS box. */
    private void addHeader(JPanel target, JComponent title) {
        Box row = Box.createHorizontalBox();
        row.add(title);
        row.add(Box.createHorizontalGlue());
        target.add(row);
        target.add(thinSeparator());
    }

    /**
     * Wraps a component in a BorderLayout.WEST anchor so it's reliably pinned to the left
     * edge of its parent, regardless of BoxLayout cross-axis alignment quirks.
     */
    private JPanel leftAnchored(Component content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, BorderLayout.WEST);
        return wrapper;
    }

    /** Centered HTML wrapper, used for both info-box states so formatting stays identical. */
    private String centeredHtml(String text) {
        return new HTMLBuilder().center().text(text).build();
    }

    // ================= ACCOUNTS (left) =================

    private JPanel buildAccountsPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));

        // ---- box 1: info text ----
        JPanel infoBox = boxedPanel(new BorderLayout());
        infoBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        infoTextPane = new JEditorPane("text/html",
            centeredHtml(GetText.tr("Login with your Minecraft account to get started.")));
        infoTextPane.setEditable(false);
        infoTextPane.setFocusable(false);
        infoTextPane.setOpaque(false);
        infoTextPane.addHyperlinkListener(this::openLink);
        infoBox.add(infoTextPane, BorderLayout.CENTER);
        // fixed height, based on the default text, so later text swaps don't resize it
        infoBox.setMaximumSize(new Dimension(Integer.MAX_VALUE,
            infoTextPane.getPreferredSize().height + (PAD * 2) + 4));
        leftPanel.add(infoBox);
        leftPanel.add(Box.createVerticalStrut(GAP));

        // ---- box 2: skin preview + account dropdown (centered, stable) + delete ----
        JPanel skinBox = boxedPanel(new BorderLayout());
        skinBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel skinCenterPanel = new JPanel(new GridBagLayout());
        skinCenterPanel.setOpaque(false);
        userSkin = new JLabel(SkinUtils.getDefaultSkin());
        skinCenterPanel.add(userSkin);
        skinBox.add(skinCenterPanel, BorderLayout.CENTER);

        setupSkinContextMenu();
        skinBox.add(buildAccountRow(), BorderLayout.SOUTH);

        leftPanel.add(skinBox);
        leftPanel.add(Box.createVerticalStrut(GAP));

        // ---- box 3: sign in with microsoft ----
        JPanel buttonBox = boxedPanel(new GridBagLayout());
        buttonBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttonBox.getPreferredSize().height));

        loginWithMicrosoftButton = new JButton();
        loginWithMicrosoftButton.setBorderPainted(false);
        loginWithMicrosoftButton.setToolTipText(GetText.tr("Sign In with Microsoft"));
        loginWithMicrosoftButton.setIcon(Utils.getIconImage(
            App.THEME.getResourcePath("image/providers", "sign-in-with-microsoft")));
        loginWithMicrosoftButton.addActionListener(e -> onSignInWithMicrosoft());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttons.setOpaque(false);
        buttons.add(loginWithMicrosoftButton);
        buttonBox.add(buttons);

        leftPanel.add(buttonBox);

        return leftPanel;
    }

    /** Right-click menu on the skin preview (change/reload skin, update username, refresh token). */
    private void setupSkinContextMenu() {
        contextMenu = new JPopupMenu();

        JMenuItem changeSkin = new JMenuItem(GetText.tr("Change Skin"));
        changeSkin.addActionListener(e -> {
            accountsViewModel.changeSkin();
            refreshSkinIcon();
        });
        contextMenu.add(changeSkin);

        JMenuItem updateSkin = new JMenuItem(GetText.tr("Reload Skin"));
        updateSkin.addActionListener(e -> {
            accountsViewModel.updateSkin();
            refreshSkinIcon();
        });
        contextMenu.add(updateSkin);

        JMenuItem updateUsername = new JMenuItem(GetText.tr("Update Username"));
        updateUsername.addActionListener(e -> accountsViewModel.updateUsername());
        contextMenu.add(updateUsername);

        refreshAccessTokenMenuItem = new JMenuItem(GetText.tr("Refresh Access Token"));
        refreshAccessTokenMenuItem.setVisible(false);
        refreshAccessTokenMenuItem.addActionListener(e -> refreshAccessToken());
        contextMenu.add(refreshAccessTokenMenuItem);
    }

    private void refreshSkinIcon() {
        MicrosoftAccount account = accountsViewModel.getSelectedAccount();
        if (account != null) {
            userSkin.setIcon(account.getMinecraftSkin());
        }
    }

    /**
     * Dropdown row: combo box stays truly centered via a spacer that always matches
     * deleteButtonHolder's width (blank card == button width), so Delete appearing/
     * disappearing never shifts the dropdown.
     */
    private JPanel buildAccountRow() {
        deleteButton = new JButton(GetText.tr("Delete"));
        deleteButton.addActionListener(e -> onDeleteAccount());

        JPanel blankCard = new JPanel();
        blankCard.setOpaque(false);
        blankCard.setPreferredSize(deleteButton.getPreferredSize());

        deleteButtonCardLayout = new CardLayout();
        deleteButtonHolder = new JPanel(deleteButtonCardLayout);
        deleteButtonHolder.setOpaque(false);
        deleteButtonHolder.add(blankCard, "blank");
        deleteButtonHolder.add(deleteButton, "button");
        deleteButtonCardLayout.show(deleteButtonHolder, "blank");

        accountsComboBox = new JComboBox<>();
        accountsComboBox.setName("accountsTabAccountsComboBox");
        accountsComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                accountsViewModel.setSelectedAccount(accountsComboBox.getSelectedIndex());
            }
        });

        JPanel leftSpacer = new JPanel();
        leftSpacer.setOpaque(false);
        leftSpacer.setPreferredSize(deleteButton.getPreferredSize());

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(PAD, 0, 0, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.anchor = GridBagConstraints.CENTER;

        g.gridx = 0;
        g.insets = new Insets(0, 0, 0, 8);
        row.add(leftSpacer, g);

        g.gridx = 1;
        g.insets = new Insets(0, 0, 0, 0);
        row.add(accountsComboBox, g);

        g.gridx = 2;
        g.insets = new Insets(0, 8, 0, 0);
        row.add(deleteButtonHolder, g);

        return row;
    }

    private void onDeleteAccount() {
        int ret = DialogManager
            .yesNoDialog()
            .setTitle(GetText.tr("Delete"))
            .setContent(GetText.tr("Are you sure you want to delete this account?"))
            .setType(DialogManager.WARNING).show();
        if (ret == DialogManager.YES_OPTION) {
            accountsViewModel.deleteAccount();
        }
    }

    private void onSignInWithMicrosoft() {
        int numberOfAccountsBefore = accountsViewModel.accountCount();

        LoginWithMicrosoftDialog loginWithMicrosoftDialog = new LoginWithMicrosoftDialog();
        loginWithMicrosoftDialog.setVisible(true);

        if (numberOfAccountsBefore != accountsViewModel.accountCount()) {
            if (loginWithMicrosoftDialog.account != null) {
                loginWithMicrosoftDialog.account.updateSkin();
            }
            accountsViewModel.pushNewAccounts();
            accountsComboBox.setSelectedItem(AccountManager.getSelectedAccount());
        }
    }

    private void refreshAccessToken() {
        MicrosoftAccount account = accountsViewModel.getSelectedAccount();
        if (account == null) {
            return;
        }

        final ProgressDialog<Boolean> dialog = new ProgressDialog<>(
            GetText.tr("Refreshing Access Token For {0}", account.minecraftUsername),
            0,
            GetText.tr("Refreshing Access Token For {0}", account.minecraftUsername),
            "Aborting refreshing access token for " + account.minecraftUsername);

        dialog.addThread(new Thread(() -> {
            boolean success = accountsViewModel.refreshAccessToken();
            dialog.setReturnValue(success);
            dialog.close();
        }));
        dialog.start();

        if (Boolean.TRUE.equals(dialog.getReturnValue())) {
            DialogManager.okDialog().setTitle(GetText.tr("Access Token Refreshed"))
                .setContent(GetText.tr("Access token refreshed successfully"))
                .setType(DialogManager.INFO).show();
        } else {
            DialogManager.okDialog().setTitle(GetText.tr("Failed To Refresh Access Token"))
                .setContent(GetText.tr("Failed to refresh accessToken. Please login again."))
                .setType(DialogManager.ERROR).show();

            new LoginWithMicrosoftDialog(account).setVisible(true);
        }
    }

    private void observeAccounts() {
        accountsViewModel.onAccountSelected(account -> {
            if (account == null) {
                deleteButtonCardLayout.show(deleteButtonHolder, "blank");
                userSkin.setIcon(SkinUtils.getDefaultSkin());
                loginWithMicrosoftButton.setVisible(true);
                refreshAccessTokenMenuItem.setVisible(false);
                infoTextPane.setText(centeredHtml(GetText.tr(
                    "Login with your Minecraft account to get started.")));
            } else {
                deleteButtonCardLayout.show(deleteButtonHolder, "button");
                loginWithMicrosoftButton.setVisible(true);
                refreshAccessTokenMenuItem.setVisible(true);
                deleteButton.setText(GetText.tr("Delete"));
                userSkin.setIcon(account.getMinecraftSkin());
                infoTextPane.setText(centeredHtml(GetText.tr("Welcome back, {0}.",
                    account.minecraftUsername)));
            }
        });
        accountsViewModel.onAccountsNamesChanged(accounts -> {
            accountsComboBox.removeAllItems();
            accountsComboBox.addItem(new ComboItem<>(null, GetText.tr("Add An Account")));
            for (String account : accounts) {
                accountsComboBox.addItem(new ComboItem<>(null, account));
            }
        });
    }

    /**
     * Restores whichever account AccountManager currently has active, instead of always
     * defaulting to "Add An Account" on tab load. Falls back to index 0 if no account is
     * currently selected (fresh install, or the selected account was deleted).
     */
    private void selectPersistedAccount() {
        MicrosoftAccount selected = AccountManager.getSelectedAccount();
        int indexToSelect = 0;

        if (selected != null) {
            for (int i = 0; i < accountsComboBox.getItemCount(); i++) {
                if (accountsComboBox.getItemAt(i).toString().equals(selected.minecraftUsername)) {
                    indexToSelect = i;
                    break;
                }
            }
        }

        accountsComboBox.setSelectedIndex(indexToSelect);
    }

    // ================= ABOUT (right) =================

    /** Right-hand column: two boxes split 50/50, same GAP between them as the left column. */
    private JPanel buildAboutColumn() {
        JPanel column = new JPanel(new GridLayout(2, 1, 0, GAP));
        column.add(buildAboutInfoBox());
        column.add(buildAboutLicenseBox());
        return column;
    }

    /**
     * Top box: launcher title, version/OS/Java info, and contributors list. Title and version
     * info are separate, independently left-anchored blocks so they can't interfere with
     * each other's alignment.
     */
    private JPanel buildAboutInfoBox() {
        JPanel box = new JPanel();
        box.setBorder(BOX_BORDER);
        box.setLayout(new BoxLayout(box, BoxLayout.PAGE_AXIS));

        // --- title row ---
        JLabel titleLabel = new JLabel(Constants.LAUNCHER_NAME);
        titleLabel.setFont(ATLauncherLaf.getInstance().getTitleFont());
        addHeader(box, titleLabel);

        // --- version/OS/Java info, left-anchored ---
        JTextPane textInfo = new JTextPane();
        textInfo.setText(aboutViewModel.getInfo());
        textInfo.setEditable(false);
        textInfo.setFocusable(false);
        textInfo.setOpaque(false);

        JPanel textInfoWrapper = leftAnchored(textInfo);
        textInfoWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));
        box.add(textInfoWrapper);
        box.add(Box.createVerticalStrut(PAD / 2));

        // --- contributors: simple two-column list of name links ---
        JLabel contributorsLabel = new JLabel(GetText.tr("Contributors"));
        contributorsLabel.setFont(ATLauncherLaf.getInstance().getTitleFont());
        addHeader(box, contributorsLabel);
        box.add(Box.createVerticalStrut(PAD / 2));

        authorsList = new JPanel(new GridLayout(0, CONTRIBUTOR_COLUMNS, CONTRIBUTOR_COLUMN_GAP,
            CONTRIBUTOR_ROW_GAP));
        authorsList.setOpaque(false);
        addDisposable(aboutViewModel.getContributors().subscribe(this::renderAuthors));

        JScrollPane contributorsScrollPane = new JScrollPane(authorsList,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contributorsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        contributorsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        contributorsScrollPane.setOpaque(false);
        contributorsScrollPane.getViewport().setOpaque(false);
        box.add(contributorsScrollPane);

        return box;
    }

    /** Bottom box: News / License / Third Party Libraries tabs. */
    private JPanel buildAboutLicenseBox() {
        JPanel box = boxedPanel(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab(GetText.tr("News"), new NewsTab());
        tabbedPane.addTab("License", buildResourceTabPanel("/LICENSE"));
        tabbedPane.addTab("Third Party Libraries", buildResourceTabPanel("/THIRDPARTYLIBRARIES"));
        box.add(tabbedPane, BorderLayout.CENTER);

        return box;
    }

    /** Loads a text resource (LICENSE / THIRDPARTYLIBRARIES) into a scrollable HTML panel. */
    private JPanel buildResourceTabPanel(String resourcePath) {
        JEditorPane textPane = new JEditorPane("text/html", "");
        textPane.setEditable(false);
        textPane.setFocusable(false);
        textPane.addHyperlinkListener(this::openLink);

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(App.class.getResourceAsStream(resourcePath), StandardCharsets.UTF_8))) {
            textPane.setText(new HTMLBuilder()
                .text(reader.lines().collect(Collectors.joining("<br/>"))
                    .replace("%YEAR%", new SimpleDateFormat("yyyy").format(new Date())))
                .build());
        } catch (Exception e) {
            LogManager.logStackTrace(e);
        }

        JScrollPane scrollPane = new JScrollPane(textPane);
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void openLink(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            OS.openWebBrowser(e.getURL());
        }
    }

    /**
     * Renders contributors as a simple two-column grid of clickable name links - no avatars,
     * no bordered cards. GridLayout wraps to a new row automatically after CONTRIBUTOR_COLUMNS
     * entries, and the whole thing scrolls vertically if it doesn't fit.
     */
    private void renderAuthors(List<Contributor> contributors) {
        for (Contributor contributor : contributors) {
            authorsList.add(buildContributorLink(contributor));
        }

        SwingUtilities.invokeLater(() -> {
            authorsList.revalidate();
            authorsList.repaint();
        });
    }

    /** A single contributor's name as a plain, left-aligned clickable link. */
    private JEditorPane buildContributorLink(Contributor contributor) {
        JEditorPane link = new JEditorPane("text/html",
            "<html><a href=\"" + contributor.url + "\">" + contributor.name + "</a></html>");
        link.setEditable(false);
        link.setFocusable(false);
        link.setOpaque(false);
        link.addHyperlinkListener(this::openLink);
        return link;
    }

    // ================= Tab interface =================

    @Override
    public String getTitle() {
        return GetText.tr("Home");
    }

    @Override
    public String getAnalyticsScreenViewName() {
        return "Home";
    }

    @Override
    protected void onDestroy() {
        removeAll();
        infoTextPane = null;
        userSkin = null;
        accountsComboBox = null;
        deleteButton = null;
        deleteButtonCardLayout = null;
        deleteButtonHolder = null;
        loginWithMicrosoftButton = null;
        refreshAccessTokenMenuItem = null;
        contextMenu = null;
        authorsList = null;
    }
}
