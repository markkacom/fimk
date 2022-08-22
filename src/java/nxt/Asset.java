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

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;
import nxt.txn.AssetIssuanceAttachment;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class Asset {

    public final static byte TYPE_PRIVATE_BIT_POS = 0;     // private  BIN 00000001
    public final static byte TYPE_MINTABLE_BIT_POS = 1;    // private and mintable  BIN 00000011

    private static final DbKey.LongKeyFactory<Asset> assetDbKeyFactory = new DbKey.LongKeyFactory<Asset>("id") {

        @Override
        public DbKey newKey(Asset asset) {
            return asset.dbKey;
        }

    };

    private static final EntityDbTable<Asset> assetTable = new EntityDbTable<Asset>("asset", assetDbKeyFactory, "name,description") {

        @Override
        protected Asset load(Connection con, ResultSet rs) throws SQLException {
            return new Asset(rs);
        }

        @Override
        protected void save(Connection con, Asset asset) throws SQLException {
            asset.save(con);
        }

        public boolean hasForeignKey() {
            return true;
        };
    };

    public static DbIterator<Asset> getAllAssets(int from, int to) {
        return assetTable.getAll(from, to);
//        int nowEpochTime = Nxt.getEpochTime();
//        DbClause.FixedClause dbClause = new DbClause.FixedClause(String.format(" expiry IS NULL OR expiry > %d ", nowEpochTime));
//        return assetTable.getManyBy(dbClause, from, to);
    }

    /*public static DbIterator<Asset> getAllAssets2(int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                    "SELECT a.*, b.block_timestamp FROM asset a INNER JOIN transaction b ON a.id = b.id"
                            + " ORDER BY b.block_timestamp DESC, a.db_id DESC "
                            + DbUtils.limitsClause(from, to)
            );
            return assetTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }*/

    public static int getCount() {
        return assetTable.getCount();
    }

    public static Asset getAsset(long id) {
        return assetTable.get(assetDbKeyFactory.newKey(id));
    }

    public static DbIterator<Asset> getAssetsIssuedBy(long accountId, int from, int to) {
        return assetTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static DbIterator<Asset> searchAssets(String query, int from, int to) {
        return assetTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC, asset.height DESC, asset.db_id DESC ");
    }

    public static void addAsset(Transaction transaction, AssetIssuanceAttachment attachment) {
        assetTable.insert(new Asset(transaction, attachment));
    }

    public static void putAsset(JSONObject json, long id) {
        Asset asset = getAsset(id);
        if (asset != null) {
            json.put("asset", Long.toUnsignedString(id));
            json.put("name", asset.getName());
            json.put("decimals", asset.getDecimals());
            if (privateEnabled()) {
                json.put("type", asset.getType());
            }
        }
    }

    static void init() {}


    private final long assetId;
    private final DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String description;
    private final long quantityQNT;
    private final byte decimals;
    private final byte type;
    private int expiry;
    private final int blockTimestamp;
    private final int height;

    private Asset(Transaction transaction, AssetIssuanceAttachment attachment) {
        this.assetId = transaction.getId();
        this.dbKey = assetDbKeyFactory.newKey(this.assetId);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.quantityQNT = attachment.getQuantityQNT();
        this.decimals = attachment.getDecimals();
        this.type = privateEnabled() ? attachment.getType() : 0;
        this.expiry = Integer.MAX_VALUE;  // MAX_VALUE means "no expiry"
        this.blockTimestamp = transaction.getBlockTimestamp();
        this.height = transaction.getHeight();
    }

    private Asset(ResultSet rs) throws SQLException {
        this.assetId = rs.getLong("id");
        this.dbKey = assetDbKeyFactory.newKey(this.assetId);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
        this.quantityQNT = rs.getLong("quantity");
        this.decimals = rs.getByte("decimals");
        this.type = privateEnabled() ? rs.getByte("type") : 0;
        int v = rs.getInt("expiry");
        this.expiry = v == 0 ? Integer.MAX_VALUE : v;
        this.blockTimestamp = rs.getInt("block_timestamp");
        this.height = rs.getInt("height");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset (id, account_id, name, "
                + "description, quantity, decimals, type, height, expiry, block_timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.name);
            pstmt.setString(++i, this.description);
            pstmt.setLong(++i, this.quantityQNT);
            pstmt.setByte(++i, this.decimals);
            pstmt.setByte(++i, this.getType());
            pstmt.setInt(++i, this.height);
            pstmt.setInt(++i, this.expiry);
            pstmt.setInt(++i, this.blockTimestamp);
            pstmt.executeUpdate();
        }
    }

    public int updateExpiry(int expiry) throws SQLException {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE asset SET expiry = ? WHERE id = ?")) {
            pstmt.setInt(1, expiry);
            pstmt.setLong(2, this.getId());
            int result = pstmt.executeUpdate();
            this.expiry = expiry;
            return result;
        }
    }

    public long getId() {
        return assetId;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getQuantityQNT() {
        return quantityQNT;
    }

    public byte getDecimals() {
        return decimals;
    }

    public byte getType() {
      return type;
    }

    public int getExpiry() {
        return expiry;
    }

    public int getBlockTimestamp() {
        return blockTimestamp;
    }

    public int getHeight() {
        return height;
    }

    public DbIterator<Account.AccountAsset> getAccounts(int from, int to) {
        return Account.getAssetAccounts(this.assetId, from, to);
    }

    public DbIterator<Account.AccountAsset> getAccounts(int height, int from, int to) {
        return Account.getAssetAccounts(this.assetId, height, from, to);
    }

    public DbIterator<Trade> getTrades(int from, int to) {
        return Trade.getAssetTrades(this.assetId, from, to);
    }

    public DbIterator<AssetTransfer> getAssetTransfers(int from, int to) {
        return AssetTransfer.getAssetTransfers(this.assetId, from, to);
    }

    public static boolean privateEnabled() {
        return HardFork.PRIVATE_ASSETS_BLOCK();
    }
}
