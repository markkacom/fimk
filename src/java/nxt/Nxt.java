package nxt;

import nxt.http.API;
import nxt.http.websocket.WebsocketServer;
import nxt.peer.Peers;
import nxt.user.Users;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import nxt.util.Time;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public final class Nxt {

    public static final String NXT_VERSION = "1.4.13";
    public static final String APPLICATION = "FIMK";
    
    /* XXX - This tracks the FIM version */
    public static final String VERSION = "0.4.0";

    private static volatile Time time = new Time.EpochTime();

    private static final Properties defaultProperties = new Properties();
    static {
        System.out.println("Initializing FIM server version " + Nxt.VERSION + " (based on NXT " + NXT_VERSION + ")");
        try (InputStream is = ClassLoader.getSystemResourceAsStream("nxt-default.properties")) {
            if (is != null) {
                Nxt.defaultProperties.load(is);
            } else {
                String configFile = System.getProperty("nxt-default.properties");
                if (configFile != null) {
                    try (InputStream fis = new FileInputStream(configFile)) {
                        Nxt.defaultProperties.load(fis);
                    } catch (IOException e) {
                        throw new RuntimeException("Error loading nxt-default.properties from " + configFile);
                    }
                } else {
                    throw new RuntimeException("nxt-default.properties not in classpath and system property nxt-default.properties not defined either");
                }
            }
            if (!VERSION.equals(Nxt.defaultProperties.getProperty("nxt.version"))) {
                throw new RuntimeException("Using an nxt-default.properties file from a version other than " + VERSION + " is not supported!!!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading nxt-default.properties", e);
        }
    }
    private static final Properties properties = new Properties(defaultProperties);
    static {
        try (InputStream is = ClassLoader.getSystemResourceAsStream("nxt.properties")) {
            if (is != null) {
                Nxt.properties.load(is);
            } // ignore if missing
        } catch (IOException e) {
            throw new RuntimeException("Error loading nxt.properties", e);
        }
    }

    private static int displayProperties = 0;
    private static boolean getDisplayProperties() {
      if (displayProperties == 0) {
        String value = properties.getProperty("nxt.debug");
        displayProperties = Boolean.TRUE.toString().equals(value) ? 1 : 2;
      }
      return displayProperties == 1;
    }
    
    public static int getIntProperty(String name) {
        try {
            int result = Integer.parseInt(properties.getProperty(name));
            if (getDisplayProperties()) Logger.logMessage(name + " = \"" + result + "\"");
            return result;
        } catch (NumberFormatException e) {
            if (getDisplayProperties()) Logger.logMessage(name + " not defined, assuming 0");
            return 0;
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

    public static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountNQT, long feeNQT, short deadline, Attachment attachment) {
        return new TransactionImpl.BuilderImpl((byte)1, senderPublicKey, amountNQT, feeNQT, deadline, (Attachment.AbstractAttachment)attachment);
    }

    public static int getEpochTime() {
        return time.getTime();
    }

    static void setTime(Time time) {
        Nxt.time = time;
    }

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    Nxt.shutdown();
                }
            }));
            init();
        } catch (Throwable t) {
            System.out.println("Fatal error: " + t.toString());
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
        Peers.shutdown();
        ThreadPool.shutdown();
        Db.shutdown();
        Logger.logShutdownMessage("FIM server " + VERSION + " (based on NXT "+ NXT_VERSION + ") stopped.");
        Logger.shutdown();
    }

    private static class Init {

        static {
            try {
                long startTime = System.currentTimeMillis();
                Logger.init();
                logSystemProperties();
                Db.init();
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
                Trade.init();
                AssetTransfer.init();
                Vote.init();
                Currency.init();
                CurrencyBuyOffer.init();
                CurrencySellOffer.init();
                CurrencyFounder.init();
                CurrencyMint.init();
                CurrencyTransfer.init();
                Exchange.init();
                Peers.init();
                Generator.init();
                API.init();
                WebsocketServer.init();
                Users.init();
                DebugTrace.init();
                MofoChart.init();
                MofoMessaging.init();
                int timeMultiplier = (Constants.isTestnet && Constants.isOffline) ? Math.max(Nxt.getIntProperty("nxt.timeMultiplier"), 1) : 1;
                ThreadPool.start(timeMultiplier);
                if (timeMultiplier > 1) {
                    setTime(new Time.FasterTime(Math.max(getEpochTime(), Nxt.getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                    Logger.logMessage("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
                }

                long currentTime = System.currentTimeMillis();
                Logger.logMessage("Initialization took " + (currentTime - startTime) / 1000 + " seconds");
                Logger.logMessage("FIM server " + VERSION + " (based on NXT "+ NXT_VERSION + ") started successfully.");
                if (Constants.isTestnet) {
                    Logger.logMessage("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
                }
            } catch (Exception e) {
                Logger.logErrorMessage(e.getMessage(), e);
                System.exit(1);
            }
        }

        private static void init() {}

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
                "file.encoding"
        };
        for (String property : loggedProperties) {
            Logger.logDebugMessage(String.format("%s = %s", property, System.getProperty(property)));
        }
        Logger.logDebugMessage(String.format("availableProcessors = %s", Runtime.getRuntime().availableProcessors()));
        Logger.logDebugMessage(String.format("maxMemory = %s", Runtime.getRuntime().maxMemory()));
    }

    private Nxt() {} // never

}
