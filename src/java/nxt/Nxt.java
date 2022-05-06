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

package nxt;

import nxt.env.DirProvider;
import nxt.env.RuntimeEnvironment;
import nxt.env.RuntimeMode;
import nxt.gossip.GossipProcessor;
import nxt.gossip.GossipProcessorImpl;
import nxt.http.API;
import nxt.http.websocket.BlockchainEvents;
import nxt.http.websocket.WebsocketServer;
import nxt.peer.Peers;
import nxt.user.Users;
import nxt.util.Convert;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import nxt.util.Time;
import nxt.virtualexchange.ExchangeObserver;
import org.json.simple.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class Nxt {

    public static final String NXT_VERSION = "1.5.10";
    public static final String APPLICATION = "FIMK";
    public static final String VERSION = "0.7.2-b1"; /* FIM Version. Note the Peers.MAX_VERSION_LENGTH*/
    public static final String MIN_VERSION = "0.6.1"; /* Blacklist everything up or below this version */

    private static volatile Time time = new Time.EpochTime();

    public static final String NXT_DEFAULT_PROPERTIES = "fimk-default.properties";
    public static final String NXT_PROPERTIES = "fimk.properties";
    public static final String CONFIG_DIR = "conf";

    private static final RuntimeMode runtimeMode;
    private static final DirProvider dirProvider;

    private static final Properties defaultProperties = new Properties();
    static {
        redirectSystemStreams("out");
        redirectSystemStreams("err");
        System.out.println("Initializing FIM server version " + Nxt.VERSION);
        printCommandLineArguments();
        runtimeMode = RuntimeEnvironment.getRuntimeMode();
        dirProvider = RuntimeEnvironment.getDirProvider();
        loadProperties(defaultProperties, NXT_DEFAULT_PROPERTIES, true);
        if (!VERSION.equals(Nxt.defaultProperties.getProperty("fimk.version"))) {
            throw new RuntimeException("Using an fimk-default.properties file from a version other than " + VERSION + " is not supported!!!");
        }
    }

    private static void redirectSystemStreams(String streamName) {
        String isStandardRedirect = System.getProperty("nxt.redirect.system." + streamName);
        Path path = null;
        if (isStandardRedirect != null) {
            try {
                path = Files.createTempFile("nxt.system." + streamName + ".", ".log");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } else {
            String explicitFileName = System.getProperty("nxt.system." + streamName);
            if (explicitFileName != null) {
                Paths.get(explicitFileName);
            }
        }
        if (path != null) {
            try {
                PrintStream stream = new PrintStream(Files.newOutputStream(path));
                if (streamName.equals("out")) {
                    System.setOut(new PrintStream(stream));
                } else {
                    System.setErr(new PrintStream(stream));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final Properties properties = new Properties(defaultProperties) {
        @Override
        public String getProperty(String key) {
            String v = super.getProperty(key);
            if (v == null) {
                //backward compatibility
                if (key.startsWith("fimk.")) {
                    key = "nxt." + key.substring(5);
                    return super.getProperty(key);
                }
            }
            return v;
        }

        @Override
        public String getProperty(String key, String defaultValue) {
            String v = super.getProperty(key, defaultValue);
            if (v == null) {
                //backward compatibility
                if (key.startsWith("fimk.")) {
                    key = "nxt." + key.substring(5);
                    return super.getProperty(key, defaultValue);
                }
            }
            return v;
        }
    };

    static {
        loadProperties(properties, NXT_PROPERTIES, false);
    }

    public static Properties loadProperties(Properties properties, String propertiesFile, boolean isDefault) {
        try {
            // Load properties from location specified as command line parameter
            String configFile = System.getProperty(propertiesFile);
            if (configFile != null) {
                System.out.printf("Loading %s from %s\n", propertiesFile, configFile);
                try (InputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException(String.format("Error loading %s from %s", propertiesFile, configFile));
                }
            } else {
                try (InputStream is = ClassLoader.getSystemResourceAsStream(propertiesFile)) {
                    // When running nxt.exe from a Windows installation we always have fimk.properties in the classpath but this is not the nxt properties file
                    // Therefore we first load it from the classpath and then look for the real fimk.properties in the user folder.
                    if (is != null) {
                        System.out.printf("Loading %s from classpath\n", propertiesFile);
                        properties.load(is);
                        if (isDefault) {
                            return properties;
                        }
                    }
                    // load non-default properties files from the user folder
                    if (!dirProvider.isLoadPropertyFileFromUserDir()) {
                        return properties;
                    }
                    if (!Files.isReadable(Paths.get(dirProvider.getUserHomeDir()))) {
                        System.out.printf("Creating dir %s\n", dirProvider.getUserHomeDir());
                        Files.createDirectory(Paths.get(dirProvider.getUserHomeDir()));
                    }
                    Path confDir = Paths.get(dirProvider.getUserHomeDir(), CONFIG_DIR);
                    if (!Files.isReadable(confDir)) {
                        System.out.printf("Creating dir %s\n", confDir);
                        Files.createDirectory(confDir);
                    }
                    Path propPath = Paths.get(confDir.toString(), propertiesFile);
                    if (Files.isReadable(propPath)) {
                        System.out.printf("Loading %s from dir %s\n", propertiesFile, confDir);
                        properties.load(Files.newInputStream(propPath));
                    } else {
                        System.out.printf("Creating property file %s\n", propPath);
                        Files.createFile(propPath);
                        Files.write(propPath, Convert.toBytes("# use this file for workstation specific " + propertiesFile));
                    }
                    return properties;
                } catch (IOException e) {
                    throw new IllegalArgumentException("Error loading " + propertiesFile, e);
                }
            }
        } catch(IllegalArgumentException e) {
            e.printStackTrace(); // make sure we log this exception
            throw e;
        }
    }

    private static void printCommandLineArguments() {
        try {
            List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            if (inputArguments != null && inputArguments.size() > 0) {
                System.out.println("Command line arguments");
            } else {
                return;
            }
            inputArguments.forEach(System.out::println);
        } catch (AccessControlException e) {
            System.out.println("Cannot read input arguments " + e.getMessage());
        }
    }

    private static int displayProperties = 0;
    private static boolean getDisplayProperties() {
      if (displayProperties == 0) {
        String value = properties.getProperty("fimk.debug");
        displayProperties = Boolean.TRUE.toString().equals(value) ? 1 : 2;
      }
      return displayProperties == 1;
    }

    public static int getIntProperty(String name) {
        return getIntProperty(name, 0);
    }

    public static int getIntProperty(String name, int defaultValue) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            if (getDisplayProperties()) Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            Logger.logMessage(name + " not defined or not numeric, using default value " + defaultValue);
            return defaultValue;
        }
    }

    public static String getStringProperty(String name) {
        return getStringProperty(name, null, false);
    }

    public static String getStringProperty(String name, String defaultValue) {
        return getStringProperty(name, defaultValue, false);
    }

    public static String getStringProperty(String name, String defaultValue, boolean doNotLog) {
        String value = properties.getProperty(name);
        if (value != null && ! "".equals(value)) {
            if (getDisplayProperties()) Logger.logMessage(name + " = \"" + (doNotLog ? "{not logged}" : value) + "\"");
            return value;
        } else {
            if (getDisplayProperties()) Logger.logMessage(name + " not defined");
            return defaultValue;
        }
    }

    public static List<String> getStringListProperty(String name) {
        String value = getStringProperty(name);
        if (value == null || value.length() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String s : value.split(";")) {
            s = s.trim();
            if (s.length() > 0) {
                result.add(s);
            }
        }
        return result;
    }

    public static Boolean getBooleanProperty(String name) {
        String value = properties.getProperty(name);
        if (Boolean.TRUE.toString().equals(value)) {
            if (getDisplayProperties()) Logger.logMessage(name + " = \"true\"");
            return true;
        } else if (Boolean.FALSE.toString().equals(value)) {
            if (getDisplayProperties()) Logger.logMessage(name + " = \"false\"");
            return false;
        }
        if (getDisplayProperties()) Logger.logMessage(name + " not defined, assuming false");
        return false;
    }

    public static Blockchain getBlockchain() {
        return BlockchainImpl.getInstance();
    }

    public static BlockchainProcessor getBlockchainProcessor() {
        return BlockchainProcessorImpl.getInstance();
    }

    public static TransactionProcessor getTransactionProcessor() {
        return TransactionProcessorImpl.getInstance();
    }

    public static GossipProcessor getGossipProcessor() {
        return GossipProcessorImpl.getInstance();
    }

    public static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment) {
        return new TransactionImpl.BuilderImpl((byte)1, senderPublicKey, amountNQT, feeNQT, deadline, (Attachment.AbstractAttachment)attachment);
    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes) throws NxtException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes);
    }

    public static Transaction.Builder newTransactionBuilder(JSONObject transactionJSON) throws NxtException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionJSON);
    }

    public static Transaction.Builder newTransactionBuilder(byte[] transactionBytes, JSONObject prunableAttachments) throws NxtException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes, prunableAttachments);
    }

    public static int getEpochTime() {
        return time.getTime();
    }

    public static void setTime(Time time) {
        Nxt.time = time;
    }

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(Nxt::shutdown));
            init();
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
            t.printStackTrace();
        }
    }

    public static void init(Properties customProperties) {
        properties.putAll(customProperties);
        init();
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        Logger.logShutdownMessage("Shutting down...");
        API.shutdown();
        Users.shutdown();
        ThreadPool.shutdown();
        Peers.shutdown();
        Db.shutdown();
        Logger.logShutdownMessage("FIM server " + VERSION + " stopped.");
        Logger.shutdown();
        runtimeMode.shutdown();
    }

    private static class Init {

        private static volatile boolean initialized = false;

        static {
            try {
                long startTime = System.currentTimeMillis();
                Logger.init();
                logSystemProperties();
                runtimeMode.init();
                setServerStatus("FIM Server - Loading database", null);
                Db.init();
                setServerStatus("FIM Server - Loading resources", null);
                TransactionProcessorImpl.getInstance();
                BlockchainProcessorImpl.getInstance();
                Account.init();
                Alias.init();
                NamespacedAlias.init();
                Asset.init();
                DigitalGoodsStore.init();
                Hub.init();
                Order.init();
                Poll.init();
                PhasingPoll.init();
                Trade.init();
                AssetTransfer.init();
                Vote.init();
                PhasingVote.init();
                Currency.init();
                CurrencyBuyOffer.init();
                CurrencySellOffer.init();
                CurrencyFounder.init();
                CurrencyMint.init();
                CurrencyTransfer.init();
                Exchange.init();
                PrunableMessage.init();
                TaggedData.init();
                Peers.init();
                Generator.init();
                API.init();
                WebsocketServer.init();
                BlockchainEvents.getInstance();
                Users.init();
                DebugTrace.init();
                MofoMessaging.init();
                MofoAsset.init();
                ExchangeObserver.init();
                MofoVerificationAuthority.init();
                GossipProcessorImpl.getInstance();
                AccountColor.init();
                AppVersionManager.getInstance();

                TransactionTypeExtension.init();

                int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(Nxt.getIntProperty("fimk.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), Nxt.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    Logger.logMessage("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }

                long currentTime = System.currentTimeMillis();
                Logger.logMessage("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                Logger.logMessage("FIM server " + VERSION + " started successfully.");
                Logger.logMessage("Copyright (c) 2013-2015 The Nxt Core Developers.");
                Logger.logMessage("Copyright (c) 2014-2016 Krypto Fin ry and the FIMKrypto Developers.");
                Logger.logMessage("Copyright (c) 2017-2021 The FIMK Developers.");
                Logger.logMessage("Distributed under GPLv2, with ABSOLUTELY NO WARRANTY.");
                if (API.getBrowserUri() != null) {
                    Logger.logMessage("Client UI is at " + API.getBrowserUri());
                }
                setServerStatus("NXT Server - Online", API.getBrowserUri());
                if (Constants.isTestnet) {
                    Logger.logMessage("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
                }
            } catch (Exception e) {
                Logger.logErrorMessage(e.getMessage(), e);
                System.exit(1);
            }
        }

        //note it is invoked after scan()
        private static void init() {
            if (initialized) {
                throw new RuntimeException("Nxt.init has already been called");
            }

            initialized = true;
        }

        private Init() {} // never

    }

    private static void logSystemProperties() {
        String[] loggedProperties = new String[] {
                "java.version",
                "java.vm.version",
                "java.vm.name",
                "java.vendor",
                "java.vm.vendor",
                "java.home",
                "java.library.path",
                "java.class.path",
                "os.arch",
                "sun.arch.data.model",
                "os.name",
                "file.encoding",
                RuntimeMode.RUNTIME_MODE_ARG
        };
        for (String property : loggedProperties) {
            Logger.logDebugMessage(String.format("%s = %s", property, System.getProperty(property)));
        }
        Logger.logDebugMessage(String.format("availableProcessors = %s", Runtime.getRuntime().availableProcessors()));
        Logger.logDebugMessage(String.format("maxMemory = %s", Runtime.getRuntime().maxMemory()));
        Logger.logDebugMessage(String.format("processId = %s", getProcessId()));
    }

    public static String getProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        if (runtimeName == null) {
            return "";
        }
        String[] tokens = runtimeName.split("@");
        if (tokens.length == 2) {
            return tokens[0];
        }
        return "";
    }

    public static String getDbDir(String dbDir) {
        return dirProvider.getDbDir(dbDir);
    }

    public static void updateLogFileHandler(Properties loggingProperties) {
        dirProvider.updateLogFileHandler(loggingProperties);
    }

    public static String getUserHomeDir() {
        return dirProvider.getUserHomeDir();
    }

    public static void setServerStatus(String status, URI wallet) {
        runtimeMode.setServerStatus(status, wallet, dirProvider.getLogFileDir());
    }

    private Nxt() {} // never

}
