package nxt.reward;

import nxt.Transaction;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Register candidate for asset rewarding
 */
public final class RewardCandidate {

    private static final DbKey.LongKeyFactory<RewardCandidate> rewardCandidateDbKeyFactory = new DbKey.LongKeyFactory<RewardCandidate>("id") {
        @Override
        public DbKey newKey(RewardCandidate rewardCandidate) {
            return rewardCandidate.dbKey;
        }
    };

    private static final EntityDbTable<RewardCandidate> rewardCandidateTable =
            new EntityDbTable<RewardCandidate>("reward_candidate", rewardCandidateDbKeyFactory) {
        @Override
        protected RewardCandidate load(Connection con, ResultSet rs) throws SQLException {
            return new RewardCandidate(rs);
        }
        @Override
        protected void save(Connection con, RewardCandidate assetRewarding) throws SQLException {
            assetRewarding.save(con);
        }
    };

    public static void save(Transaction transaction, long assetId) {
        rewardCandidateTable.insert(new RewardCandidate(transaction, assetId));
    }

    public static DbIterator<RewardCandidate> getAll(int from, int to) {
        return rewardCandidateTable.getAll(from, to);
    }

    public static int getCount() {
        return rewardCandidateTable.getCount();
    }

    /**
     * "sorted by id" is important because is used in the consensus.
     */
    public static DbIterator<RewardCandidate> getRewardCandidatesSorted(long asset, int from, int to) {
        return rewardCandidateTable.getManyBy(new DbClause.LongClause("asset_id", asset), from, to, "ORDER BY id");
    }

//    public static DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, int from, int to) {
//        Connection con = null;
//        try {
//            con = Db.db.getConnection();
//            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM asset_transfer WHERE sender_id = ?"
//                    + " UNION ALL SELECT * FROM asset_transfer WHERE recipient_id = ? AND sender_id <> ? ORDER BY height DESC, db_id DESC"
//                    + DbUtils.limitsClause(from, to));
//            int i = 0;
//            pstmt.setLong(++i, accountId);
//            pstmt.setLong(++i, accountId);
//            pstmt.setLong(++i, accountId);
//            DbUtils.setLimits(++i, pstmt, from, to);
//            return assetRewardingTable.getManyBy(con, pstmt, false);
//        } catch (SQLException e) {
//            DbUtils.close(con);
//            throw new RuntimeException(e.toString(), e);
//        }
//    }

//    public static DbIterator<AssetTransfer> getAccountAssetTransfers(long accountId, long assetId, int from, int to) {
//        Connection con = null;
//        try {
//            con = Db.db.getConnection();
//            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM asset_transfer WHERE sender_id = ? AND asset_id = ?"
//                    + " UNION ALL SELECT * FROM asset_transfer WHERE recipient_id = ? AND sender_id <> ? AND asset_id = ? ORDER BY height DESC, db_id DESC"
//                    + DbUtils.limitsClause(from, to));
//            int i = 0;
//            pstmt.setLong(++i, accountId);
//            pstmt.setLong(++i, assetId);
//            pstmt.setLong(++i, accountId);
//            pstmt.setLong(++i, accountId);
//            pstmt.setLong(++i, assetId);
//            DbUtils.setLimits(++i, pstmt, from, to);
//            return assetRewardingTable.getManyBy(con, pstmt, false);
//        } catch (SQLException e) {
//            DbUtils.close(con);
//            throw new RuntimeException(e.toString(), e);
//        }
//    }

/*
CREATE TABLE IF NOT EXISTS reward_candidate (id BIGINT NOT NULL,
height INT NOT NULL, asset_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE)
* */

    public static void init() {}

    private final long id;
    private final DbKey dbKey;
    private final int height;
    private final long asset;
    private final long account;
    long balance = -1;  // -1 means not filled

    private RewardCandidate(Transaction transaction, long assetId) {
        this.id = transaction.getId();
        this.dbKey = rewardCandidateDbKeyFactory.newKey(this.id);
        this.height = transaction.getHeight();
        this.account = transaction.getSenderId();
        this.asset = assetId;
    }

    private RewardCandidate(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = rewardCandidateDbKeyFactory.newKey(this.id);
        this.height = rs.getInt("height");
        this.account = rs.getLong("account_id");
        this.asset = rs.getLong("asset_id");
    }

    private void save(Connection con) throws SQLException {
        // overwrite previous transaction on same asset and candidate
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO reward_candidate " +
                "(id, height, account_id, asset_id) "
                + "KEY (asset_id, account_id) VALUES (?, ?, ?, ?) ")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setInt(++i, this.height);
            pstmt.setLong(++i, this.account);
            pstmt.setLong(++i, this.asset);
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return id;
    }

    public int getHeight() {
        return height;
    }

    public long getAsset() {
        return asset;
    }

    public long getAccount() {
        return account;
    }
}