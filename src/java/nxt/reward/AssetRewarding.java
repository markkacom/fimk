package nxt.reward;

import nxt.Db;
import nxt.Transaction;
import nxt.db.*;
import nxt.txn.AssetRewardingAttachment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Rules described rewarding for private asset
 */
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

//    public static AssetTransfer addAssetTransfer(Transaction transaction, AssetTransferAttachment attachment) {
//        AssetTransfer assetTransfer = new AssetTransfer(transaction, attachment);
//        assetRewardingTable.insert(assetTransfer);
//        listeners.notify(assetTransfer, AssetTransfer.Event.ASSET_TRANSFER);
//        return assetTransfer;
//    }


    public static DbIterator<AssetRewarding> getAssetRewardings(int height) {
        // result must be strong ordered because it affects consensus
        Connection con = null;
        try {
            con = Db.db.getConnection();
            // todo select records where expiry > today and height > r.height
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM asset_rewarding WHERE height < ? ORDER BY height, id");
            pstmt.setInt(1, height);
            return assetRewardingTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<AssetRewarding> getApplicableRewardings(int height) {
        List<AssetRewarding> result = new ArrayList<>();
        DbIterator<AssetRewarding> rewardings = getAssetRewardings(height);
        for (AssetRewarding r : rewardings) {
            if ((height - r.height) % r.frequency == 0) result.add(r);
        }

        return result;
    }


    public static void init() {
    }


    private final long id;
    private final DbKey dbKey;
    private final int height;
    private final long asset;
    private final int frequency;
    private final byte target;
    private final byte lotteryType;
    private final long baseAmount;
    private final long balanceDivider;
    private final long targetInfo;
    private final int halvingBlocks;

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
        this.targetInfo = attachment.getTargetInfo();
        this.halvingBlocks = attachment.getHalvingBlocks();
    }

    private AssetRewarding(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = dbKeyFactory.newKey(this.id);
        this.height = rs.getInt("height");
        this.asset = rs.getLong("asset_id");
        this.frequency = rs.getInt("frequency");
        this.halvingBlocks = rs.getInt("halvingBlocks");
        this.target = rs.getByte("target");
        this.lotteryType = rs.getByte("lotteryType");
        this.baseAmount = rs.getLong("baseAmount");
        this.balanceDivider = rs.getLong("balanceDivider");
        this.targetInfo = rs.getLong("targetInfo");
    }

    private void save(Connection con) throws SQLException {
        // overwrite previous transaction on same asset. Only single one per asset
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO asset_rewarding " +
                "(id, height, asset_id, frequency, halvingBlocks, target, lotteryType, baseAmount, balanceDivider, targetInfo) "
                + "KEY (asset_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setInt(++i, this.height);
            pstmt.setLong(++i, this.asset);
            pstmt.setInt(++i, this.frequency);
            pstmt.setInt(++i, this.halvingBlocks);
            pstmt.setByte(++i, this.target);
            pstmt.setByte(++i, this.lotteryType);
            pstmt.setLong(++i, this.baseAmount);
            pstmt.setLong(++i, this.balanceDivider);
            pstmt.setLong(++i, this.targetInfo);
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

    public long getTargetInfo() {
        return targetInfo;
    }

    public int getHalvingBlocks() {
        return halvingBlocks;
    }

    @Override
    public String toString() {
        return "AssetRewarding{" +
                "id=" + id +
                ", dbKey=" + dbKey +
                ", height=" + height +
                ", asset=" + asset +
                ", frequency=" + frequency +
                ", halvingBlocks=" + halvingBlocks +
                ", target=" + target +
                ", lotteryType=" + lotteryType +
                ", baseAmount=" + baseAmount +
                ", balanceDivider=" + balanceDivider +
                ", targetInfo=" + targetInfo +
                '}';
    }
}
