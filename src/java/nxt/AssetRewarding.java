package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;
import nxt.txn.AssetRewardingAttachment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AssetRewarding {

    private static final DbKey.LongKeyFactory<AssetRewarding> dbKeyFactory = new DbKey.LongKeyFactory<AssetRewarding>("id") {

        @Override
        public DbKey newKey(AssetRewarding assetRewarding) {
            return assetRewarding.dbKey;
        }

    };

    private static final EntityDbTable<AssetRewarding> assetRewardingTable = new EntityDbTable<AssetRewarding>("asset_rewarding", dbKeyFactory) {

        @Override
        protected AssetRewarding load(Connection con, ResultSet rs) throws SQLException {
            return new AssetRewarding(rs);
        }

        @Override
        protected void save(Connection con, AssetRewarding assetRewarding) throws SQLException {
            assetRewarding.save(con);
        }

    };

    public static void save(Transaction transaction, AssetRewardingAttachment attachment) {
        assetRewardingTable.insert(new AssetRewarding(transaction, attachment));
    }

    public static DbIterator<AssetRewarding> getAll(int from, int to) {
        return assetRewardingTable.getAll(from, to);
    }

    public static int getCount() {
        return assetRewardingTable.getCount();
    }

    public static DbIterator<AssetRewarding> getAssetRewardings(long asset, int from, int to) {
        return assetRewardingTable.getManyBy(new DbClause.LongClause("asset_id", asset), from, to);
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

    public static int getTransferCount(long assetId) {
        return assetRewardingTable.getCount(new DbClause.LongClause("asset_id", assetId));
    }

//    public static AssetTransfer addAssetTransfer(Transaction transaction, AssetTransferAttachment attachment) {
//        AssetTransfer assetTransfer = new AssetTransfer(transaction, attachment);
//        assetRewardingTable.insert(assetTransfer);
//        listeners.notify(assetTransfer, AssetTransfer.Event.ASSET_TRANSFER);
//        return assetTransfer;
//    }

    static void init() {}


    private final long id;
    private final DbKey dbKey;
    private final int height;
    private final long asset;
    private final int frequency;
    private final byte target;
    private final byte lotteryType;
    private final long baseAmount;
    private final long balanceDivider;
    private final long a;

    private AssetRewarding(Transaction transaction, AssetRewardingAttachment attachment) {
        this.id = transaction.getId();
        this.dbKey = dbKeyFactory.newKey(this.id);
        this.height = transaction.getHeight();
        this.asset = attachment.getAsset();
        this.frequency = attachment.getFrequency();
        this.target = attachment.getTarget();
        this.lotteryType = attachment.getLotteryType();
        this.baseAmount = attachment.getBaseAmount();
        this.balanceDivider = attachment.getBalanceDivider();
        this.a = attachment.getTargetInfo();
    }

    private AssetRewarding(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKeyFactory.newKey(this.id);
        this.height = rs.getInt("height");
        this.asset = rs.getLong("asset_id");
        this.frequency = rs.getInt("frequency");
        this.target = rs.getByte("target");
        this.lotteryType = rs.getByte("lotteryType");
        this.baseAmount = rs.getLong("baseAmount");
        this.balanceDivider = rs.getLong("balanceDivider");
        this.a = rs.getLong("a");
    }

    private void save(Connection con) throws SQLException {
        // overwrite previous transaction on same asset
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO asset_rewarding " +
                "(id, height, asset_id, frequency, target, lotteryType, baseAmount, balanceDivider, a) "
                + "KEY (asset_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setInt(++i, this.height);
            pstmt.setLong(++i, this.asset);
            pstmt.setInt(++i, this.frequency);
            pstmt.setByte(++i, this.target);
            pstmt.setByte(++i, this.lotteryType);
            pstmt.setLong(++i, this.baseAmount);
            pstmt.setLong(++i, this.balanceDivider);
            pstmt.setLong(++i, this.a);
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

    public int getFrequency() {
        return frequency;
    }

    public byte getTarget() {
        return target;
    }

    public byte getLotteryType() {
        return lotteryType;
    }

    public long getBaseAmount() {
        return baseAmount;
    }

    public long getBalanceDivider() {
        return balanceDivider;
    }

    public long getA() {
        return a;
    }

}
