package nxt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;

public final class MofoAsset {

    final static int ONE_HUNDRED_PERCENT = 100000000;
  
    public static class AssetFee {
        public static AssetFee NULL_FEE = new AssetFee(0, 0);
      
        private int orderFeePercentage;
        private int tradeFeePercentage;
        
        public AssetFee(int orderFeePercentage, int tradeFeePercentage) {
            this.orderFeePercentage = orderFeePercentage;
            this.tradeFeePercentage = tradeFeePercentage;
        }
        
        public int getOrderFeePercentage() {
            return orderFeePercentage;
        }
        
        public int getTradeFeePercentage() {
            return tradeFeePercentage;
        }
    }
  
    private static class PrivateAsset {

        private final long assetId;
        private final DbKey dbKey;
        private int orderFeePercentage;
        private int tradeFeePercentage;        

        private PrivateAsset(long assetId, int orderFeePercentage, int tradeFeePercentage) {
            this.assetId = assetId;
            this.dbKey = privateAssetDbKeyFactory.newKey(this.assetId);
            this.orderFeePercentage = orderFeePercentage;
            this.tradeFeePercentage = tradeFeePercentage;
        }
    
        private PrivateAsset(ResultSet rs) throws SQLException {
            this.assetId = rs.getLong("asset_id");
            this.dbKey = privateAssetDbKeyFactory.newKey(this.assetId);
            this.orderFeePercentage = rs.getInt("order_fee_percentage");
            this.tradeFeePercentage = rs.getInt("trade_fee_percentage");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO private_asset "
                    + "(asset_id, order_fee_percentage, trade_fee_percentage, height, latest) "
                    + "KEY (asset_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.assetId);
                pstmt.setInt(++i, this.orderFeePercentage);
                pstmt.setInt(++i, this.tradeFeePercentage);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }

        public void save() {
            privateAssetTable.insert(this);
        }

        @Override
        public String toString() {
            return "PrivateAsset asset_id: " + Convert.toUnsignedLong(assetId)
                    + " order fee: " + orderFeePercentage + " trade fee: " + tradeFeePercentage;
        }    
    }
    
    public static class PrivateAssetAccount {
  
        private final long assetId;
        private final long accountId;          
        private boolean allowed;
        private final DbKey dbKey;
  
        public PrivateAssetAccount(long assetId, long accountId, boolean allowed) {
            this.assetId = assetId;
            this.accountId = accountId;            
            this.dbKey = privateAssetAccountDbKeyFactory.newKey(this.assetId, this.accountId);
            this.allowed = allowed;
        }
    
        private PrivateAssetAccount(ResultSet rs) throws SQLException {
            this.assetId = rs.getLong("asset_id");
            this.accountId = rs.getLong("account_id");
            this.dbKey = privateAssetAccountDbKeyFactory.newKey(this.assetId, this.accountId);
            this.allowed = rs.getBoolean("allowed");
        }

        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO private_asset_account "
                    + "(asset_id, account_id, allowed, height, latest) "
                    + "KEY (asset_id, account_id, height) VALUES (?, ?, ?, ?, TRUE)")) {
                int i = 0;
                pstmt.setLong(++i, this.assetId);
                pstmt.setLong(++i, this.accountId);
                pstmt.setBoolean(++i, this.allowed);
                pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
                pstmt.executeUpdate();
            }
        }
        
        public void save() {
            privateAssetAccountTable.insert(this);
        }

        @Override
        public String toString() {
            return "PrivateAssetAccount asset_id: " + Convert.toUnsignedLong(assetId) 
                    + " account_id: " + Convert.rsAccount(accountId)
                    + " allowed: " + allowed;
        }
    }
    
    private static final DbKey.LongKeyFactory<PrivateAsset> privateAssetDbKeyFactory = new DbKey.LongKeyFactory<PrivateAsset>("asset_id") {
  
        @Override
        public DbKey newKey(PrivateAsset privateAsset) {
            return privateAsset.dbKey;
        }
  
    };
  
    private static final VersionedEntityDbTable<PrivateAsset> privateAssetTable = new VersionedEntityDbTable<PrivateAsset>("private_asset", privateAssetDbKeyFactory) {
  
        @Override
        protected PrivateAsset load(Connection con, ResultSet rs) throws SQLException {
            return new PrivateAsset(rs);
        }
  
        @Override
        protected void save(Connection con, PrivateAsset privateAsset) throws SQLException {
            privateAsset.save(con);
        }
  
        @Override
        protected String defaultSort() {
            return " ORDER BY asset_id ";
        }
  
    }; 
    
    private static final DbKey.LinkKeyFactory<PrivateAssetAccount> privateAssetAccountDbKeyFactory = new DbKey.LinkKeyFactory<PrivateAssetAccount>("asset_id", "account_id") {
  
        @Override
        public DbKey newKey(PrivateAssetAccount item) {
            return item.dbKey;
        }
  
    };
  
    private static final VersionedEntityDbTable<PrivateAssetAccount> privateAssetAccountTable = new VersionedEntityDbTable<PrivateAssetAccount>("private_asset_account", privateAssetAccountDbKeyFactory) {
  
        @Override
        protected PrivateAssetAccount load(Connection con, ResultSet rs) throws SQLException {
            return new PrivateAssetAccount(rs);
        }
  
        @Override
        protected void save(Connection con, PrivateAssetAccount item) throws SQLException {
            item.save(con);
        }
  
        @Override
        protected String defaultSort() {
            return " ORDER BY asset_id, account_id ";
        }  
    };
    
    public static boolean isPrivateAsset(long assetId) {
        if (Asset.privateEnabled()) {
            Asset asset = Asset.getAsset(assetId);
            if (asset != null) {
                return isPrivateAsset(asset);
            }
        }
        return false; 
    }
    
    public static boolean isPrivateAsset(Asset asset) {
        return asset.getType() == Asset.TYPE_PRIVATE_ASSET;
    }
    
    public static boolean getAccountAllowed(long assetId, long accountId) {
        if (Asset.privateEnabled()) {
            Asset asset = Asset.getAsset(assetId);
            if (asset != null && asset.getAccountId() == accountId) {
                return true;
            }
            PrivateAssetAccount privateAssetAccount;
            privateAssetAccount = privateAssetAccountTable.get(privateAssetAccountDbKeyFactory.newKey(assetId, accountId));
            if (privateAssetAccount != null) {
                return privateAssetAccount.allowed;
            }
        }
        return false;
    }
    
    public static void setAccountAllowed(long assetId, long accountId, boolean allowed) {
        PrivateAssetAccount privateAssetAccount;
        privateAssetAccount = privateAssetAccountTable.get(privateAssetAccountDbKeyFactory.newKey(assetId, accountId));
        if (privateAssetAccount == null) {
            privateAssetAccount = new PrivateAssetAccount(assetId, accountId, allowed);
        } else {
            privateAssetAccount.allowed = allowed;
        }
        privateAssetAccount.save();
    }

    public static void setFee(long assetId, int orderFeePercentage, int tradeFeePercentage) {
        PrivateAsset privateAsset;
        privateAsset = privateAssetTable.get(privateAssetDbKeyFactory.newKey(assetId));
        if (privateAsset == null) {
            privateAsset = new PrivateAsset(assetId, orderFeePercentage, tradeFeePercentage);
        } else {
            privateAsset.orderFeePercentage = orderFeePercentage;
            privateAsset.tradeFeePercentage = tradeFeePercentage;
        }
        privateAsset.save();
    }
    
    public static AssetFee getFee(long assetId) {
        if (Asset.privateEnabled()) {
            PrivateAsset privateAsset;
            privateAsset = privateAssetTable.get(privateAssetDbKeyFactory.newKey(assetId));
            if (privateAsset != null) {
                return new AssetFee(privateAsset.orderFeePercentage, privateAsset.tradeFeePercentage);
            }
        }
        return AssetFee.NULL_FEE;
    }
    
    /**
     * Order fee is always calculated up to the nearest whole number.
     * 
     * 2000%      - 2,000,000,000
     * 100%       - 100,000,000
     * 10%        - 10,000,000
     * 1%         - 1,000,000
     * 0.1%       - 100,000
     * 0.01%      - 10,000
     * 0.001%     - 1,000
     * 0.0001%    - 100
     * 0.00001%   - 10
     * 0.000001%  - 1
     */
    public static long calculateOrderFee(long assetId, long amount) {
        AssetFee fee = getFee(assetId);
        if (AssetFee.NULL_FEE != fee) {
            return BigDecimal.valueOf(amount).
                              divide(BigDecimal.valueOf(ONE_HUNDRED_PERCENT)).
                              multiply(BigDecimal.valueOf(fee.getOrderFeePercentage())).
                              setScale(0, RoundingMode.CEILING).
                              longValue();
        }
        return 0;
    }
    
    static void init() {}
}
