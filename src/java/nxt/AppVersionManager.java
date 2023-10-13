package nxt;

import java.util.Date;

import nxt.util.Convert;
import nxt.util.Logger;
import nxt.util.ThreadPool;

/**
 * The version manager reads application version data from the blockchain.
 * Peers who chose to do so can have their FIMK software periodically check the
 * blockchain and be notified when their current version is out of date.
 *
 * There are three values on the blockchain that are involved in this process.
 *
 * Alias LATESTVERSION (9266582752086146948)
 *
 *    This alias holds the most recent available version and a SHA256 checksum
 *    of the downloadable package.
 *    The version notifcations use this version and checksum to generate a
 *    command you should run to update your current version to LATESTVERSION.
 *
 *    The expected format for this alias:
 *
 *        [0-9]+\.[0-9]+\.[0-9]+(-.+)?\s+[a-z0-9]{64}
 *
 * Alias MINVERSIONWARN (17359617168004080578)
 *
 *    This alias holds the version number and blockheight, only after the
 *    blockheight has passed will the version manager act upon this.
 *    The version number is the minimal version you should run before we start
 *    issuing notifications that your version can be updated.
 *
 *    You can disable this functionality in your fimk.properties config file.
 *    Set `fimk.warnNotLatestVersion=false` to disable this functionality.
 *
 *    The expected format for this alias:
 *
 *        [0-9]+\.[0-9]+\.[0-9]+(-.+)?\s+[0-9]+
 *
 * Alias MINVERSIONBLACKLIST (9364249966090852339)
 *
 *    This alias holds the version number and blockheight, only after the
 *    blockheight has passed will the version manager act upon this.
 *    The version number is the minimal version you should run before we issue
 *    a notification that your version can be updated.
 *
 *    When the version manager detects you run a version up or below this
 *    version your server WILL BE SHUTDOWN. To start the server again either
 *    update (recommended) or disable this feature in fimk.properties.
 *
 *    Peers who enable this feature will start to blacklist all nodes on the
 *    network that are running a version on or below this version.
 *
 *    You can disable this functionality in your fimk.properties config file.
 *    Set `fimk.shutdownWhenOutdated=false` to disable this functionality.
 *
 *    The expected format for this alias:
 *
 *        [0-9]+\.[0-9]+\.[0-9]+(-.+)?\s+[0-9]+
 *
 * As mentioned the default behavior for clients who do not disable this feature
 * is for their servers to SHUTDOWN when we detect the server version is up or
 * below MINVERSIONBLACKLIST.
 *
 * The rational behind this is to protect users from accidentally landing on a
 * network fork because of a required update they've missed.
 *
 * Running an unsupported version is dangerous and should be avoided for several
 * reasons:
 *
 *    1. Forgers running an unsupported version will loose their FIMK forged
 *       on that fork. Electricity is wasted running your server and damage is
 *       caused to the rest of the network when you send blocks or transactions
 *       that are incompatible.
 *    2. Exchange operators running an unsupported version run the risk of
 *       loosing money when on a fork. The exchange software will accept
 *       FIMK deposits and credit BTC or other internal tokens to the depositor
 *       if these deposits where made while on a fork the exchange will loose
 *       the deposited FIMK.
 *    3. Merchants very similar to exchange operators risk the loss of funds
 *       when they accept FIMK payments while their server is on a fork.
 *    4. Ordinary users who accept payments or asset transfers risk loosing
 *       those funds since they are on a fork.
 *
 */

public class AppVersionManager {

    private static final boolean SHUTDOWN_WHEN_OUTDATED = Nxt.getBooleanProperty("fimk.shutdownWhenOutdated");
    private static final boolean WARN_NOT_LATEST_VERSION = Nxt.getBooleanProperty("fimk.warnNotLatestVersion");
    private static final int NEW_VERSION_CHECK_INTERVAL = Nxt.getIntProperty("fimk.newVersionCheckInterval", 600);

    private static long LATEST_VERSION_ID = Constants.isTestnet ? 0 : Long.parseUnsignedLong("9266582752086146948");
    private static long BLACKLIST_VERSION_ID = Constants.isTestnet ? 0 : Long.parseUnsignedLong("9364249966090852339");
    private static long WARN_VERSION_ID = Constants.isTestnet ? 0 : Long.parseUnsignedLong("17359617168004080578");

    public AppVersion MIN_VERSION = new AppVersion(Nxt.MIN_VERSION);
    public AppVersion VERSION = new AppVersion(Nxt.VERSION);

    /* Expects format [0-9]+\.[0-9]+\.[0-9]+(-.+)?\s+[a-z0-9]{64} */
    static class LatestVersion {
        private AppVersion version;
        private String checksum;
        private Date date;

        public static LatestVersion create(String aliasURI, int timestamp) {
            String[] parts = aliasURI.trim().split("\\s+");
            if (parts.length != 2) {
                throw new RuntimeException("Expected format: 0.0.0 [sha245 hash] [" + aliasURI.trim() + "]");
            }
            AppVersion version = new AppVersion(parts[0]);
            if (!version.getIsValid()) {
                throw new RuntimeException("Invalid version format [" + aliasURI.trim() + "]");
            }
            Date date = new Date(Convert.fromEpochTime(timestamp));
            if (parts[1].length() != 64) {
                throw new RuntimeException("Invalid checksum length [" + aliasURI.trim() + "]");
            }
            return new LatestVersion(version, parts[1], date);
        }

        LatestVersion(AppVersion version, String checksum, Date date) {
            this.version = version;
            this.checksum = checksum;
            this.date = date;
        }

        public AppVersion getVersion() {
            return version;
        }

        public String getChecksum() {
            return checksum;
        }

        public Date getDate() {
            return date;
        }
    }

    /* Expects format [0-9]+\.[0-9]+\.[0-9]+(-.+)?\s+[0-9]+ */
    static class MinVersion {
        private AppVersion version;
        private int height;
        private Date date;

        public static MinVersion create(String aliasURI, int timestamp) {
            String[] parts = aliasURI.trim().split("\\s+");
            if (parts.length != 2) {
                throw new RuntimeException("Expected format: 0.0.0 123456789 [" + aliasURI.trim() + "]");
            }
            AppVersion version = new AppVersion(parts[0]);
            if (!version.getIsValid()) {
                throw new RuntimeException("Invalid version format [" + aliasURI.trim() + "]");
            }
            int height = Integer.parseInt(parts[1]);
            Date date = new Date(Convert.fromEpochTime(timestamp));
            return new MinVersion(version, height, date);
        }

        MinVersion(AppVersion version, int height, Date date) {
            this.version = version;
            this.height = height;
            this.date = date;
        }

        public AppVersion getVersion() {
            return version;
        }

        public int getHeight() {
            return height;
        }

        public Date getDate() {
            return date;
        }
    }

    static AppVersionManager instance = new AppVersionManager();
    public static AppVersionManager getInstance() {
        return instance;
    }

    private AppVersionManager() {
        if (SHUTDOWN_WHEN_OUTDATED ||
            WARN_NOT_LATEST_VERSION) {
            ThreadPool.scheduleThread("GetAppVersionUpdates", getAppVersionUpdates,
                                      NEW_VERSION_CHECK_INTERVAL); // once every 10 minutes
        }
    }

    private final Runnable getAppVersionUpdates = new Runnable() {
        public void run() {
            if ( ! Nxt.getBlockchainProcessor().isScanning()) {

                Alias latestAlias = Alias.getAlias(LATEST_VERSION_ID);
                Alias blacklistAlias = Alias.getAlias(BLACKLIST_VERSION_ID);
                Alias warnAlias = Alias.getAlias(WARN_VERSION_ID);

                if (latestAlias == null) {
                    Logger.logInfoMessage("Could not find LATESTVERSION");
                    return;
                }
                if (blacklistAlias == null) {
                    Logger.logInfoMessage("Could not find MINVERSIONBLACKLIST");
                    return;
                }
                if (warnAlias == null) {
                    Logger.logInfoMessage("Could not find MINVERSIONWARN");
                    return;
                }

                LatestVersion latestVersion;
                MinVersion blacklistVersion;
                MinVersion warnVersion;

                try {
                    latestVersion = LatestVersion.create(latestAlias.getAliasURI(), latestAlias.getTimestamp());
                } catch (RuntimeException e) {
                    Logger.logInfoMessage("Could not interpret LATESTVERSION", e);
                    return;
                }

                try {
                    blacklistVersion = MinVersion.create(blacklistAlias.getAliasURI(), blacklistAlias.getTimestamp());
                } catch (RuntimeException e) {
                    Logger.logInfoMessage("Could not interpret MINVERSIONBLACKLIST", e);
                    return;
                }

                try {
                    warnVersion = MinVersion.create(warnAlias.getAliasURI(), warnAlias.getTimestamp());
                } catch (RuntimeException e) {
                    Logger.logInfoMessage("Could not interpret MINVERSIONWARN", e);
                    return;
                }

                /* Sanity check .. Latest version MUST ALWAYS be greater than BLACKLIST and WARN versions */
                if (warnVersion.version.compareTo(latestVersion.version) >= 0) {
                    Logger.logInfoMessage("Blockchain reports MIN_WARN_VERSION greater than LATEST_VERSION");
                    return;
                }
                if (blacklistVersion.version.compareTo(latestVersion.version) >= 0) {
                    Logger.logInfoMessage("Blockchain reports MIN_BLACKLIST_VERSION greater than LATEST_VERSION");
                    return;
                }

                /* Update global MIN_VERSION for use in peer blacklisting */
                if (Nxt.getBlockchain().getHeight() >= blacklistVersion.getHeight()) {
                    if (blacklistVersion.version.compareTo(MIN_VERSION) >= 0) { // only change MIN_VERSION when it's greater
                        MIN_VERSION = blacklistVersion.version;
                    }
                }

                if (SHUTDOWN_WHEN_OUTDATED) {
                    if (Nxt.getBlockchain().getHeight() >= blacklistVersion.getHeight() &&
                        blacklistVersion.version.compareTo(VERSION) >= 0) {
                        shutdownServer(latestVersion);
                        return;
                    }
                }

                if (WARN_NOT_LATEST_VERSION) {
                    if (Nxt.getBlockchain().getHeight() >= warnVersion.getHeight()  &&
                        warnVersion.version.compareTo(VERSION) >= 0) {
                        printVersionWarning(latestVersion);
                    }
                }
            }
        }
    };

    private void printVersionWarning(LatestVersion latestVersion) {
        final String message = String.format(
              "%n##############################################################################%n"
            + "##%n"
            + "## WARNING!%n"
            + "##%n"
            + "## YOU ARE RUNNING OUTDATED SOFTWARE%n"
            + "## FOLLOW INSTRUCTIONS BELOW TO UPDATE%n"
            + "##%n"
            + "## YOUR CURRENT VERSION IS %s %n"
            + "## THE LATEST VERSION IS %s [ %s ] %n"
            + "##%n"
            + "## IMPORTANT!%n"
            + "##%n"
            + "## In case you are still downloading the blockchain it could be a more recent%n"
            + "## update was released but was not yet seen on your blockchain.%n"
            + "## If this is the case we advise you to wait until all blocks are downloaded.%n"
            + "##%n"
            + "################################################################################%n"
            + "##%n"
            + "## FOR LINUX USERS RUN THE FOLLOWING COMMAND (requires curl, unzip, sha256_sum)%n"
            + "##%n"
            + "## $ sh update_fimk.sh %s %s%n"
            + "##%n"
            + "## USERS ON OTHER PLATFORMS SHOULD DOWNLOAD THEIR UPDATE HERE..%n"
            + "## https://github.com/fimkrypto/fimk/releases/download/v%s/fim-%s.zip%n"
            + "##%n"
            + "################################################################################",
            Nxt.VERSION,
            latestVersion.getVersion().toString(),
            latestVersion.getDate().toString(),
            latestVersion.getVersion().toString(),
            latestVersion.getChecksum(),
            latestVersion.getVersion().toString(),
            latestVersion.getVersion().toString(),
            latestVersion.getVersion().toString());

        Logger.logInfoMessage(message);
    }

    private void shutdownServer(LatestVersion latestVersion) {
        final String message = String.format(
              "%n##############################################################################%n"
            + "##%n"
            + "## WARNING!%n"
            + "##%n"
            + "## YOU ARE RUNNING UNSUPPORTED SOFTWARE%n"
            + "## FOLLOW INSTRUCTIONS BELOW TO UPDATE%n"
            + "##%n"
            + "## YOUR CURRENT VERSION IS %s %n"
            + "## THE LATEST VERSION IS %s [ %s ] %n"
            + "##%n"
            + "################################################################################%n"
            + "##%n"
            + "## FOR LINUX USERS RUN THE FOLLOWING COMMAND (requires curl, unzip, sha256_sum)%n"
            + "##%n"
            + "## $ sh update_fimk.sh %s %s%n"
            + "##%n"
            + "## USERS ON OTHER PLATFORMS SHOULD DOWNLOAD THEIR UPDATE HERE..%n"
            + "## https://github.com/fimkrypto/fimk/releases/download/v%s/fim-%s.zip%n"
            + "##%n"
            + "################################################################################",
            Nxt.VERSION,
            latestVersion.getVersion().toString(),
            latestVersion.getDate().toString(),
            latestVersion.getVersion().toString(),
            latestVersion.getChecksum(),
            latestVersion.getVersion().toString(),
            latestVersion.getVersion().toString(),
            latestVersion.getVersion().toString());

        Logger.logInfoMessage(message);

        Nxt.shutdown();
    }
}
