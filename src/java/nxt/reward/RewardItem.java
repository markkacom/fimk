package nxt.reward;

import nxt.crypto.Crypto;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Reward amount for asset/money and account
 */
public class RewardItem {

    public static void init() {
    }

    public static void registerReward(RewardItem rewardItem) {
        rewardItemTable.insert(rewardItem);
        System.out.printf("reward %s \n", rewardItem);
    }

    private static final DbKey.LongKeyFactory<RewardItem> dbKeyFactory = new DbKey.LongKeyFactory<RewardItem>("id") {

        @Override
        public DbKey newKey(RewardItem assetRewarding) {
            return assetRewarding.dbKey;
        }

    };

    private static final EntityDbTable<RewardItem> rewardItemTable = new EntityDbTable<RewardItem>("reward_item", dbKeyFactory) {

        @Override
        protected RewardItem load(Connection con, ResultSet rs) throws SQLException {
            return new RewardItem(rs);
        }

        @Override
        protected void save(Connection con, RewardItem rewardItem) throws SQLException {
            rewardItem.save(con);
        }

    };

    private final DbKey dbKey;
    int height;
    long campaignId;
    String name;
    long accountId;
    long assetId;
    long amount;

    /**
     *
     * @param height
     * @param campaignId rewarding campaign id or one of constants: 0 FIM POS reward, -1 FIM POP reward
     * @param name
     * @param accountId
     * @param assetId
     * @param amount
     */
    public RewardItem(int height, long campaignId, String name, long accountId, long assetId, long amount) {
        this.height = height;
        this.dbKey = dbKeyFactory.newKey(hash(height, accountId, assetId, campaignId));
        this.campaignId = campaignId;
        this.name = name;
        this.accountId = accountId;
        this.assetId = assetId;
        this.amount = amount;
    }

    private RewardItem(ResultSet rs) throws SQLException {
        this.height = rs.getInt("height");
        this.campaignId = rs.getLong("campaign_id");
        this.name = rs.getString("name");;
        this.accountId = rs.getLong("account_id");
        this.assetId = rs.getLong("asset_id");
        this.amount = rs.getLong("amount");
        this.dbKey = dbKeyFactory.newKey(hash(height, accountId, assetId, campaignId));
    }

    @Override
    public String toString() {
        return "AssetReward{" +
                "name=" + name +
                ", campaignId=" + Long.toUnsignedString(campaignId) +
                ", accountId=" + Long.toUnsignedString(accountId) +
                ", assetId=" + Long.toUnsignedString(assetId) +
                ", amount=" + amount +
                '}';
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO reward_item " +
                "(height, campaign_id, name, account_id, asset_id, amount) VALUES (?, ?, ?, ?, ?, ?) "
        )) {
            int i = 0;
            pstmt.setInt(++i, this.height);
            pstmt.setLong(++i, this.campaignId);
            pstmt.setString(++i, this.name);
            pstmt.setLong(++i, this.accountId);
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.amount);
            pstmt.executeUpdate();
        }
    }

    private long hash(int height, long accountId, long assetId, long rewardingId) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 8 + 8 + 8);
        buffer.putInt(height).putLong(accountId).putLong(assetId).putLong(rewardingId);
        byte[] h = Crypto.sha256().digest(buffer.array());
        BigInteger bigInteger = new BigInteger(1, new byte[]{h[7], h[6], h[5], h[4], h[3], h[2], h[1], h[0]});
        return bigInteger.longValue();
    }

}
