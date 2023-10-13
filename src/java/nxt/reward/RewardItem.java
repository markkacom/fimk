package nxt.reward;

import nxt.Asset;
import nxt.Db;
import nxt.crypto.Crypto;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.EntityDbTable;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Reward amount for asset/money and account
 */
public class RewardItem {

    /**
     * This enum provides human-readable names for each registered reward
     */
    public enum NAME {
        // do not rename enum items since it is saved to db. Until it is refactored to use code instead name of enum item.
        POS_REWARD(1, "POS Block Reward"),
        POP_REWARD_MONEY(2, "POP Block Reward"),
        FORGER(3, "POP Forger Reward"),
        CONSTANT_ACCOUNT(4, "POP Constant Account Reward"),
        RANDOM_ACCOUNT(5, "POP Random Account Reward"),
        RANDOM_WEIGHTED_ACCOUNT(6, "POP Random Weighted Account Reward");

        private static final HashMap<Integer, NAME> map;

        static {
            map = new HashMap<>(NAME.values().length);
            Arrays.stream(NAME.values()).forEach(value -> map.put(value.code, value));
        }

        public final int code;
        public final String text;

        NAME(int code, String text) {
            this.code = code;
            this.text = text;
        }

        public static NAME resolve(int code) {
            return map.get(code);
        }
    }

    public static class TotalItem {
        public final String assetName;
        public final int decimals;
        public final int fromHeight;
        public final int toHeight;
        public long accountId;
        public final long assetId;
        public final NAME name;
        public long campaignId;
        public long amount;

        public TotalItem(int fromHeight, int toHeight, long accountId, long assetId, NAME name, long campaignId, long amount) {
            this.fromHeight = fromHeight;
            this.toHeight = toHeight;
            this.accountId = accountId;
            this.assetId = assetId;
            this.name = name;
            this.campaignId = campaignId;
            this.amount = amount;

            if (assetId == 0) {
                this.decimals = 8;
                this.assetName = "FIM";
            } else {
                Asset asset = Asset.getAsset(assetId);
                if (asset == null) {
                    this.decimals = 0;
                    this.assetName = null;
                } else {
                    this.decimals = asset.getDecimals();
                    this.assetName = asset.getName();
                }
            }
        }
    }

    public static void init() {
    }

    public static void registerReward(RewardItem rewardItem) {
        rewardItemTable.insert(rewardItem);
        //System.out.printf("reward %s \n", rewardItem);
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

    public static DbIterator<RewardItem> getRewardItems(long accountId, int fromHeight, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                    "SELECT * FROM reward_item WHERE account_id = ? AND height >= ? ORDER BY height DESC "
                            + DbUtils.limitsClause(from, to));
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setInt(++i, fromHeight);
            DbUtils.setLimits(++i, pstmt, from, to);
            return rewardItemTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * Total amounts grouped by reward kind in the height range.
     *
     * @param fromHeight inclusive min limit
     * @param toHeight   exclusive max limit
     * @return
     */
    public static List<TotalItem> getTotals(int fromHeight, int toHeight) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "select account_id, asset_id, name_code, campaign_id, sum(amount) from reward_item ri " +
                             "where height>= ? and height < ? " +
                             "group by account_id, asset_id, name_code, campaign_id " +
                             "order by name_code ")) {
            pstmt.setInt(1, fromHeight);
            pstmt.setInt(2, toHeight);
            List<TotalItem> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    NAME name = NAME.resolve(rs.getInt(3));
                    result.add(new TotalItem(
                            fromHeight,
                            toHeight,
                            rs.getLong(1),
                            rs.getLong(2),
                            name,
                            rs.getLong(4),
                            rs.getLong(5)
                    ));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * Total reward amounts for account.
     */
    public static List<TotalItem> getTotals(long accountId) {
        String sql = "select asset_id, name_code, sum(amount) from reward_item " +
                "where account_id = ? " +
                "group by asset_id, name_code ";
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {

            pstmt.setLong(1, accountId);
            List<TotalItem> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    NAME name = NAME.resolve(rs.getInt(2));
                    result.add(new TotalItem(
                            0,
                            Integer.MAX_VALUE,
                            accountId,
                            rs.getLong(1),
                            name,
                            0,
                            rs.getLong(3)
                    ));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private final DbKey dbKey;
    int height;
    long campaignId;
    NAME name;
    long accountId;
    long assetId;
    long amount;

    /**
     * @param height
     * @param campaignId rewarding campaign id or one of constants: 0 FIM POS reward, -1 FIM POP reward
     * @param name
     * @param accountId
     * @param assetId
     * @param amount
     */
    public RewardItem(int height, long campaignId, NAME name, long accountId, long assetId, long amount) {
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
        this.name = NAME.resolve(rs.getInt("name_code"));
        this.accountId = rs.getLong("account_id");
        this.assetId = rs.getLong("asset_id");
        this.amount = rs.getLong("amount");
        this.dbKey = dbKeyFactory.newKey(hash(height, accountId, assetId, campaignId));
    }

    public int getHeight() {
        return height;
    }

    public long getCampaignId() {
        return campaignId;
    }

    public NAME getName() {
        return name;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "RewardItem{" +
                "name=" + name +
                ", campaignId=" + Long.toUnsignedString(campaignId) +
                ", accountId=" + Long.toUnsignedString(accountId) +
                ", assetId=" + Long.toUnsignedString(assetId) +
                ", amount=" + amount +
                '}';
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO reward_item " +
                "(height, campaign_id, name_code, account_id, asset_id, amount) VALUES (?, ?, ?, ?, ?, ?) "
        )) {
            int i = 0;
            pstmt.setInt(++i, this.height);
            pstmt.setLong(++i, this.campaignId);
            pstmt.setInt(++i, this.name.code);
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
