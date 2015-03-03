package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.EntityDbTable;
import nxt.util.Convert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.simple.JSONObject;

public final class Asset {
  
    public final static byte TYPE_REGULAR_ASSET = (byte) 0;
    public final static byte TYPE_PRIVATE_ASSET = (byte) 1;    

    private static final DbKey.LongKeyFactory<Asset> assetDbKeyFactory = new DbKey.LongKeyFactory<Asset>("id") {

        @Override
        public DbKey newKey(Asset asset) {
            return asset.dbKey;
        }

    };

    private static final EntityDbTable<Asset> assetTable = new EntityDbTable<Asset>("asset", assetDbKeyFactory) {

        @Override
        protected Asset load(Connection con, ResultSet rs) throws SQLException {
            return new Asset(rs);
        }

        @Override
        protected void save(Connection con, Asset asset) throws SQLException {
            asset.save(con);
        }

    };

    public static DbIterator<Asset> getAllAssets(int from, int to) {
        return assetTable.getAll(from, to);
    }

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
        return assetTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC, asset.height DESC ");
    }

    static void addAsset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
        assetTable.insert(new Asset(transaction, attachment));
    }
    
    public static void putAsset(JSONObject json, long id) {
        Asset asset = getAsset(id);
        if (asset != null) {
            json.put("asset", Convert.toUnsignedLong(id));
            json.put("name", asset.getName());
            json.put("decimals", asset.getDecimals());
            json.put("type", asset.getType());
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

    private Asset(Transaction transaction, Attachment.ColoredCoinsAssetIssuance attachment) {
        this.assetId = transaction.getId();
        this.dbKey = assetDbKeyFactory.newKey(this.assetId);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
        this.quantityQNT = attachment.getQuantityQNT();
        this.decimals = attachment.getDecimals();
        this.type = privateEnabled() ? attachment.getType() : 0;
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
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO asset (id, account_id, name, "
                + "description, quantity, decimals, type, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setString(++i, this.getName());
            pstmt.setString(++i, this.getDescription());
            pstmt.setLong(++i, this.getQuantityQNT());
            pstmt.setByte(++i, this.getDecimals());
            pstmt.setByte(++i, this.getType());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
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
    
    public DbIterator<Account.AccountAsset> getAccounts(int from, int to) {
        return Account.getAssetAccounts(this.assetId, from, to);
    }

    public DbIterator<Account.AccountAsset> getAccounts(int height, int from, int to) {
        if (height < 0) {
            return getAccounts(from, to);
        }
        return Account.getAssetAccounts(this.assetId, height, from, to);
    }

    public DbIterator<Trade> getTrades(int from, int to) {
        return Trade.getAssetTrades(this.assetId, from, to);
    }

    public DbIterator<AssetTransfer> getAssetTransfers(int from, int to) {
        return AssetTransfer.getAssetTransfers(this.assetId, from, to);
    }

    public static boolean privateEnabled() {
        if(Nxt.getBlockchain().getLastBlock().getHeight() >= Constants.PRIVATE_ASSETS_BLOCK) {
            return true;
        }
        return false;
    }
}
