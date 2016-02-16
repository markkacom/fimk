package nxt.replicate.mysql;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import nxt.Appendix;
import nxt.Block;
import nxt.Nxt;
import nxt.Transaction;
import nxt.TransactionImpl;
import nxt.db.DbUtils;
import nxt.replicate.IReplicator;
import nxt.replicate.ReplicateDB;
import nxt.util.Logger;

public class MySQLReplicator implements IReplicator {

    private ReplicateDB db;

    public MySQLReplicator(ReplicateDB db) {
        this.db = db;
    }

    @Override
    public boolean rescanBegin(int height) {
        Logger.logInfoMessage("MySQLReplicator rescanBegin at height " + height);
        try (Connection con = db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM block WHERE height >= ?")) {
              pstmtDelete.setInt(1, height);
              pstmtDelete.executeUpdate();
              return true;
          }
          catch (SQLException e) {
              Logger.logErrorMessage("MySQLReplicator blockPopped outer", e);
              return false;
          }
    }

    @Override
    public boolean addedUnconfirmedTransactions(List<? extends Transaction> transactions) {
        try {
            try (Connection con = db.getConnection()) {
                short index = 0;
                for (Transaction transaction : transactions) {
                    try (PreparedStatement pstmt = con.prepareStatement(
                            "INSERT INTO unconfirmed_transaction ("
                          + "id, deadline, recipient_id, sender_id, amount, fee, type, subtype, "
                          + "timestamp, attachment_bytes, transaction_version, has_message, "
                          + "has_encrypted_message, has_encrypttoself_message, "
                          + "expiration, height, transaction_index) "
                          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        int i = 0;
                        pstmt.setLong(++i, transaction.getId());
                        pstmt.setShort(++i, transaction.getDeadline());
                        DbUtils.setLongZeroToNull(pstmt, ++i, transaction.getRecipientId());
                        pstmt.setLong(++i, transaction.getSenderId());
                        pstmt.setLong(++i, transaction.getAmountNQT());
                        pstmt.setLong(++i, transaction.getFeeNQT());
                        pstmt.setByte(++i, transaction.getType().getType());
                        pstmt.setByte(++i, transaction.getType().getSubtype());
                        pstmt.setInt(++i, transaction.getTimestamp());
                        int bytesLength = 0;
                        for (Appendix appendage : transaction.getAppendages()) {
                            bytesLength += appendage.getSize();
                        }
                        if (bytesLength == 0) {
                            pstmt.setNull(++i, Types.VARBINARY);
                        } else {
                            ByteBuffer buffer = ByteBuffer.allocate(bytesLength);
                            buffer.order(ByteOrder.LITTLE_ENDIAN);
                            for (Appendix appendage : transaction.getAppendages()) {
                                appendage.putBytes(buffer);
                            }
                            pstmt.setBytes(++i, buffer.array());
                            pstmt.setByte(++i, transaction.getVersion());
                        }
                        pstmt.setBoolean(++i, transaction.getMessage() != null);
                        pstmt.setBoolean(++i, transaction.getEncryptedMessage() != null);
                        pstmt.setBoolean(++i, transaction.getEncryptToSelfMessage() != null);
                        pstmt.setInt(++i, transaction.getExpiration());
                        pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                        pstmt.setInt(++i, index++);
                        pstmt.executeUpdate();
                    }
                }
                return true;
            }
        }
        catch (SQLException e) {
            Logger.logErrorMessage("MySQLReplicator blockPopped outer", e);
            return false;
        }
    }

    @Override
    public boolean removedUnconfirmedTransactions(List<? extends Transaction> transactions) {
        try {
            try (Connection con = db.getConnection()) {
                for (Transaction transaction : transactions) {
                    try (PreparedStatement pstmt = con.prepareStatement(
                            "DELETE FROM unconfirmed_transaction WHERE id = ?")) {
                        int i = 0;
                        pstmt.setLong(++i, transaction.getId());
                        pstmt.executeUpdate();
                    }
                }
                return true;
            }
        }
        catch (SQLException e) {
            Logger.logErrorMessage("MySQLReplicator blockPopped outer", e);
            return false;
        }
    }

    @Override
    public boolean addedConfirmedTransactions(List<? extends Transaction> transactions) {
        try {
            try (Connection con = db.getConnection()) {
                short index = 0;
                for (Transaction transaction : transactions) {
                    try (PreparedStatement pstmt = con.prepareStatement(
                            "INSERT INTO transaction (id, deadline, recipient_id, amount, "
                          + "fee, height, block_id, signature, timestamp, type, subtype, sender_id, "
                          + "block_timestamp, referenced_transaction_full_hash, attachment_bytes, "
                          + "transaction_version, has_message, has_encrypted_message, has_encrypttoself_message, transaction_index) "
                          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        int i = 0;
                        pstmt.setLong(++i, transaction.getId());
                        pstmt.setShort(++i, transaction.getDeadline());
                        DbUtils.setLongZeroToNull(pstmt, ++i, transaction.getRecipientId());
                        pstmt.setLong(++i, transaction.getAmountNQT());
                        pstmt.setLong(++i, transaction.getFeeNQT());
                        pstmt.setInt(++i, transaction.getHeight());
                        pstmt.setLong(++i, transaction.getBlockId());
                        pstmt.setBytes(++i, transaction.getSignature());
                        pstmt.setInt(++i, transaction.getTimestamp());
                        pstmt.setByte(++i, transaction.getType().getType());
                        pstmt.setByte(++i, transaction.getType().getSubtype());
                        pstmt.setLong(++i, transaction.getSenderId());
                        pstmt.setInt(++i, transaction.getBlockTimestamp());
                        DbUtils.setBytes(pstmt, ++i, ((TransactionImpl)transaction).referencedTransactionFullHash());
                        int bytesLength = 0;
                        for (Appendix appendage : transaction.getAppendages()) {
                            bytesLength += appendage.getSize();
                        }
                        if (bytesLength == 0) {
                            pstmt.setNull(++i, Types.VARBINARY);
                        } else {
                            ByteBuffer buffer = ByteBuffer.allocate(bytesLength);
                            buffer.order(ByteOrder.LITTLE_ENDIAN);
                            for (Appendix appendage : transaction.getAppendages()) {
                                appendage.putBytes(buffer);
                            }
                            pstmt.setBytes(++i, buffer.array());
                        }
                        pstmt.setByte(++i, transaction.getVersion());
                        pstmt.setBoolean(++i, transaction.getMessage() != null);
                        pstmt.setBoolean(++i, transaction.getEncryptedMessage() != null);
                        pstmt.setBoolean(++i, transaction.getEncryptToSelfMessage() != null);
                        pstmt.setShort(++i, index++);
                        pstmt.executeUpdate();
                    }
                }
                return true;
            }
        }
        catch (SQLException e) {
            Logger.logErrorMessage("MySQLReplicator blockPopped outer", e);
            return false;
        }
    }

    @Override
    // relying on cascade triggers in the database to delete the transactions and public keys for all deleted blocks
    public boolean blockPopped(Block block) {
        try (Connection con = db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT id FROM block WHERE timestamp >= "
                        + "(SELECT timestamp FROM block WHERE id = ?) ORDER BY timestamp DESC");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM block WHERE id = ?")) {
           try {
               pstmtSelect.setLong(1, block.getId());
               try (ResultSet rs = pstmtSelect.executeQuery()) {
                   while (rs.next()) {
                       pstmtDelete.setLong(1, rs.getLong("db_id"));
                       pstmtDelete.executeUpdate();
                   }
                   return true;
               }
           }
           catch (SQLException e) {
               Logger.logErrorMessage("MySQLReplicator blockPopped inner", e);
               return false;
           }
       }
       catch (SQLException e) {
           Logger.logErrorMessage("MySQLReplicator blockPopped outer", e);
           return false;
       }
    }

    @Override
    public boolean beforeBlockApply(Block block) {
        try {
            try (Connection con = db.getConnection()) {
                try (PreparedStatement pstmt = con.prepareStatement(
                        "INSERT INTO block (id, timestamp, "
                      + "total_amount, total_fee, height, generator_id) "
                      + "VALUES (?, ?, ?, ?, ?, ?)")) {
                   int i = 0;
                   pstmt.setLong(++i, block.getId());
                   pstmt.setInt(++i, block.getTimestamp());
                   pstmt.setLong(++i, block.getTotalAmountNQT());
                   pstmt.setLong(++i, block.getTotalFeeNQT());
                   pstmt.setInt(++i, block.getHeight());
                   pstmt.setLong(++i, block.getGeneratorId());
                   pstmt.executeUpdate();
               }
               return true;
            }
        }
        catch (SQLException e) {
            Logger.logErrorMessage("MySQLReplicator beforeBlockApply", e);
            return false;
        }
    }



    @Override
    public boolean updateAccountBalance(long accountId, long balanceNQT, int height) {
        return true;
    }

    @Override
    public boolean updateAccountUnconfirmedBalance(long accountId, long unconfirmedBalanceNQT, int height) {
        return true;
    }

    @Override
    public boolean updateAccountPublicKey(long accountId, byte[] publicKey, int height) {
        return true;
    }

    @Override
    public boolean updateAccountAssetBalance(long accountId, long assetId, long quantityQNT, int height) {
        return true;
    }

    @Override
    public boolean updateAccountAssetUnconfirmedBalance(long accountId, long assetId, long unconfirmedQuantityQNT, int height) {
        return true;
    }

}

//PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT " + dbKeyFactory.getPKColumns()
//+ " FROM " + table + " WHERE height > ?");
//PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table
//+ " WHERE height > ?");
//PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table
//+ " SET latest = TRUE " + dbKeyFactory.getPKClause() + " AND height ="
//+ " (SELECT MAX(height) FROM " + table + dbKeyFactory.getPKClause() + ")")) {
