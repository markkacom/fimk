/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.env;

import nxt.Block;
import nxt.Constants;
import nxt.Generator;
import nxt.Nxt;
import nxt.http.API;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;

public class DesktopSystemTray {

    private SystemTray tray;
    private JFrame wrapper = new JFrame();
    private JDialog statusDialog;
    private JPanel statusPanel;
    private ImageIcon imageIcon;
    private TrayIcon trayIcon;
    private MenuItem openWallet;
    private MenuItem viewLog;
    private SystemTrayDataProvider dataProvider;

    void createAndShowGUI() {
        if (!SystemTray.isSupported()) {
            Logger.logInfoMessage("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        imageIcon = new ImageIcon("html/ui/img/nxt-icon-32x32.png", "tray icon");
        trayIcon = new TrayIcon(imageIcon.getImage());
        trayIcon.setImageAutoSize(true);
        tray = SystemTray.getSystemTray();

        MenuItem shutdown = new MenuItem("Shutdown");
        openWallet = new MenuItem("Open Wallet");
        if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            openWallet.setEnabled(false);
        }
        viewLog = new MenuItem("View Log File");
        if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            viewLog.setEnabled(false);
        }
        MenuItem status = new MenuItem("Status");

        popup.add(status);
        popup.add(viewLog);
        popup.addSeparator();
        popup.add(openWallet);
        popup.addSeparator();
        popup.add(shutdown);
        trayIcon.setPopupMenu(popup);
        trayIcon.setToolTip("Initializing");
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            Logger.logInfoMessage("TrayIcon could not be added", e);
            return;
        }

        trayIcon.addActionListener(e -> displayStatus());

        openWallet.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(dataProvider.getWallet());
            } catch (IOException ex) {
                Logger.logInfoMessage("Cannot open wallet", ex);
            }
        });

        viewLog.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(dataProvider.getLogFile());
            } catch (IOException ex) {
                Logger.logInfoMessage("Cannot view log", ex);
            }
        });

        status.addActionListener(e -> displayStatus());

        shutdown.addActionListener(e -> {
            Logger.logInfoMessage("Shutdown requested by System Tray");
            System.exit(0); // Implicitly invokes shutdown using the shutdown hook
        });
    }

    private void displayStatus() {
        Block lastBlock = Nxt.getBlockchain().getLastBlock();
        Collection<Generator> allGenerators = Generator.getAllGenerators();

        StringBuilder generators = new StringBuilder();
        for (Generator generator : allGenerators) {
            generators.append('\n').append(Convert.rsAccount(generator.getAccountId()));
        }
        Object optionPaneBackground = UIManager.get("OptionPane.background");
        UIManager.put("OptionPane.background", Color.WHITE);
        Object panelBackground = UIManager.get("Panel.background");
        UIManager.put("Panel.background", Color.WHITE);
        Object textFieldBackground = UIManager.get("TextField.background");
        UIManager.put("TextField.background", Color.WHITE);
        if (statusDialog != null && statusPanel != null) {
            statusDialog.getContentPane().remove(statusPanel);
        }
        statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));

        addLabelRow(statusPanel, "Installation");
        addDataRow(statusPanel, "Application", Nxt.APPLICATION);
        addDataRow(statusPanel, "Version", Nxt.VERSION);
        addDataRow(statusPanel, "Network", (Constants.isTestnet) ? "test" : "main");
        addDataRow(statusPanel, "Working offline", "" + Constants.isOffline);
        addDataRow(statusPanel, "Wallet", String.valueOf(API.getBrowserUri()));
        addDataRow(statusPanel, "Peer port", String.valueOf(Peers.getDefaultPeerPort()));
        addDataRow(statusPanel, "Program folder", String.valueOf(Paths.get(".").toAbsolutePath().getParent()));
        addDataRow(statusPanel, "User folder", String.valueOf(Paths.get(Nxt.getUserHomeDir()).toAbsolutePath()));
        addEmptyRow(statusPanel);

        if (lastBlock != null) {
            addLabelRow(statusPanel, "Last Block");
            addDataRow(statusPanel, "Height", String.valueOf(lastBlock.getHeight()));
            addDataRow(statusPanel, "Timestamp", String.valueOf(lastBlock.getTimestamp()));
            addDataRow(statusPanel, "Time", String.valueOf(new Date(Convert.fromEpochTime(lastBlock.getTimestamp()))));
            addDataRow(statusPanel, "Seconds passed", String.valueOf(Nxt.getEpochTime() - lastBlock.getTimestamp()));
            addDataRow(statusPanel, "Forging", String.valueOf(allGenerators.size() > 0));
            if (allGenerators.size() > 0) {
                addDataRow(statusPanel, "Forging accounts", generators.toString());
            }
        }

        addEmptyRow(statusPanel);
        addLabelRow(statusPanel, "Environment");
        addDataRow(statusPanel, "Number of peers", String.valueOf(Peers.getAllPeers().size()));
        addDataRow(statusPanel, "Available processors", String.valueOf(Runtime.getRuntime().availableProcessors()));
        addDataRow(statusPanel, "Max memory", humanReadableByteCount(Runtime.getRuntime().maxMemory()));
        addDataRow(statusPanel, "Total memory", humanReadableByteCount(Runtime.getRuntime().totalMemory()));
        addDataRow(statusPanel, "Free memory", humanReadableByteCount(Runtime.getRuntime().freeMemory()));
        addDataRow(statusPanel, "Process id", Nxt.getProcessId());
        if (statusDialog == null || !statusDialog.isVisible()) {
            JOptionPane pane = new JOptionPane(statusPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, imageIcon);
            statusDialog = pane.createDialog(wrapper, "FIM Server Status");
            statusDialog.setVisible(true);
            statusDialog.dispose();
        } else {
            java.awt.EventQueue.invokeLater(statusDialog::toFront);
        }
        UIManager.put("OptionPane.background", optionPaneBackground);
        UIManager.put("Panel.background", panelBackground);
        UIManager.put("TextField.background", textFieldBackground);
    }

    private void addDataRow(JPanel parent, String text, String value) {
        JPanel rowPanel = new JPanel();
        if (!"".equals(value)) {
            rowPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        }
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        if (!"".equals(text) && !"".equals(value)) {
            text += ':';
        }
        JLabel textLabel = new JLabel(text);
        // textLabel.setFont(textLabel.getFont().deriveFont(Font.BOLD));
        rowPanel.add(textLabel);
        rowPanel.add(Box.createRigidArea(new Dimension(140 - textLabel.getPreferredSize().width, 0)));
        JTextField valueField = new JTextField(value);
        valueField.setEditable(false);
        valueField.setBorder(BorderFactory.createEmptyBorder());
        rowPanel.add(valueField);
        rowPanel.add(Box.createRigidArea(new Dimension(4, 0)));
        parent.add(rowPanel);
        parent.add(Box.createRigidArea(new Dimension(0, 4)));
    }

    private void addLabelRow(JPanel parent, String text) {
        addDataRow(parent, text, "");
    }

    private void addEmptyRow(JPanel parent) {
        addLabelRow(parent, "");
    }

    void setToolTip(final SystemTrayDataProvider dataProvider) {
        SwingUtilities.invokeLater(() -> {
            trayIcon.setToolTip(dataProvider.getToolTip());
            openWallet.setEnabled(dataProvider.getWallet() != null && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
            viewLog.setEnabled(dataProvider.getWallet() != null);
            DesktopSystemTray.this.dataProvider = dataProvider;
        });
    }

    void shutdown() {
        SwingUtilities.invokeLater(() -> tray.remove(trayIcon));
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1000;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "" + ("KMGTPE").charAt(exp-1);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
