package nxt.replicate;

import java.util.List;

import nxt.Account;
import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Constants;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.replicate.ReplicateDB.DbProperties;
import nxt.replicate.mysql.MySQLReplicateDB;
import nxt.replicate.mysql.MySQLReplicateDBVersion;
import nxt.util.Logger;

public class Replicate {

    public static ReplicateDB db;

    public static void init() {
        if (Nxt.getBooleanProperty("nxt.replicationEnabled")) {
            setupReplication();
            setupListeners();
        }
    }

    private static void setupReplication() {
        final String PREFIX = Constants.isTestnet ? "nxt.testReplication" : "nxt.replication";
        DbProperties properties = new ReplicateDB.DbProperties();
        properties.jdbcUrl(Nxt.getStringProperty(PREFIX + "JdbcUrl"))
                  .username(Nxt.getStringProperty(PREFIX + "Username"))
                  .password(Nxt.getStringProperty(PREFIX + "Password"))
                  .connectionTimeout(Nxt.getIntProperty(PREFIX + "ConnectionTimeout"))
                  .idleTimeout(Nxt.getIntProperty(PREFIX + "IdleTimeout"))
                  .maxLifetime(Nxt.getIntProperty(PREFIX + "MaxLifetime"))
                  .minimumIdle(Nxt.getIntProperty(PREFIX + "MinimumIdle"))
                  .maximumPoolSize(Nxt.getIntProperty(PREFIX + "MaximumPoolSize"));

        String vendorType = Nxt.getStringProperty("nxt.replicationVendorType");
        if (MySQLReplicateDB.VENDOR_TYPE.equals(vendorType)) {
            db = new MySQLReplicateDB(properties);

            ReplicateDBVersion version = new MySQLReplicateDBVersion();
            db.init(version);
            version.init(db);
        }
        else {
            throw new RuntimeException("Unsupported \"nxt.replicationVendorType\" " + vendorType);
        }
    }

    private static void setupListeners() {
//        Account.addListener(account -> {
//            updateAccountBalance(account.getId(), account.getBalanceNQT());
//        }, Account.Event.BALANCE);
//
//        Account.addListener(account -> {
//            updateAccountUnconfirmedBalance(account.getId(), account.getUnconfirmedBalanceNQT());
//        }, Account.Event.UNCONFIRMED_BALANCE);

        Account.addListener(account -> {
            updateAccountPublicKey(account.getId(), account.getPublicKey());
        }, Account.Event.PUBLIC_KEY);

//        Account.addAssetListener(accountAsset -> {
//            updateAccountAssetBalance(accountAsset.getAccountId(), accountAsset.getAssetId(), accountAsset.getQuantityQNT());
//        }, Account.Event.ASSET_BALANCE);
//
//        Account.addAssetListener(accountAsset -> {
//            updateAccountAssetUnconfirmedBalance(accountAsset.getAccountId(), accountAsset.getAssetId(), accountAsset.getUnconfirmedQuantityQNT());
//        }, Account.Event.UNCONFIRMED_ASSET_BALANCE);

        Nxt.getBlockchainProcessor().addListener(block -> {
            blockPopped(block);
        }, BlockchainProcessor.Event.BLOCK_POPPED);

        Nxt.getBlockchainProcessor().addListener(block -> {
            beforeBlockApply(block);
        }, BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);

        Nxt.getBlockchainProcessor().addListener(block -> {
            rescanBegin(block);
        }, BlockchainProcessor.Event.RESCAN_BEGIN);

        Nxt.getTransactionProcessor().addListener(transactions -> {
            addConfirmedTransaction(transactions);
        }, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);

        Nxt.getTransactionProcessor().addListener(transactions -> {
            addUnconfirmedTransaction(transactions);
        }, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);

        Nxt.getTransactionProcessor().addListener(transactions -> {
            removeUnconfirmedTransaction(transactions);
        },TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
    }

    private static void updateAccountBalance(long accountId, long balanceNQT) {
        if (!db.getReplicator().updateAccountBalance(accountId, balanceNQT, Nxt.getBlockchain().getHeight())) {
            throw new RuntimeException("Replicator updateAccountBalance failed");
        }
    }

    private static void updateAccountUnconfirmedBalance(long accountId, long unconfirmedBalanceNQT) {
        if (!db.getReplicator().updateAccountUnconfirmedBalance(accountId, unconfirmedBalanceNQT, Nxt.getBlockchain().getHeight())) {
            throw new RuntimeException("Replicator updateAccountUnconfirmedBalance failed");
        }
    }

    private static void updateAccountPublicKey(long accountId, byte[] publicKey) {
        if (!db.getReplicator().updateAccountPublicKey(accountId, publicKey, Nxt.getBlockchain().getHeight())) {
            throw new RuntimeException("Replicator updateAccountPublicKey failed");
        }
    }

    private static void updateAccountAssetBalance(long accountId, long assetId, long quantityQNT) {
    }

    private static void updateAccountAssetUnconfirmedBalance(long accountId, long assetId, long unconfirmedQuantityQNT) {
    }

    private static void blockPopped(Block block) {
        if (!db.getReplicator().blockPopped(block)) {
            throw new RuntimeException("Replicator blockPopped failed");
        }
    }

    private static void beforeBlockApply(Block block) {
        if (!db.getReplicator().beforeBlockApply(block)) {
            throw new RuntimeException("Replicator beforeBlockApply failed");
        }
    }

    private static void rescanBegin(Block block) {
        if (!db.getReplicator().rescanBegin(block.getHeight())) {
            throw new RuntimeException("Replicator rescanBegin failed");
        }
    }

    private static void addUnconfirmedTransaction(List<? extends Transaction> transactions) {
        if (!db.getReplicator().addedUnconfirmedTransactions(transactions)) {
            throw new RuntimeException("Replicator addUnconfirmedTransaction failed");
        }
    }

    private static void removeUnconfirmedTransaction(List<? extends Transaction> transactions) {
        if (!db.getReplicator().removedUnconfirmedTransactions(transactions)) {
            throw new RuntimeException("Replicator removeUnconfirmedTransaction failed");
        }
    }

    private static void addConfirmedTransaction(List<? extends Transaction> transactions) {
        if (!db.getReplicator().addedConfirmedTransactions(transactions)) {
            throw new RuntimeException("Replicator addConfirmedTransaction failed");
        }
    }

}