package nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.util.Convert;

public final class MofoAsset {

    public static class PrivateAsset {

        private final long assetId;
        private int orderFeePercentage;
        private int tradeFeePercentage;
        private final DbKey dbKey;

        private PrivateAsset(long assetId, int orderFeePercentage, int tradeFeePercentage) {
            this.assetId = assetId;
            this.dbKey = privateAssetDbKeyFactory.newKey(this.assetId);
            this.orderFeePercentage = orderFeePercentage;
            this.tradeFeePercentage = tradeFeePercentage;
        }
    
        private PrivateAsset(ResultSet rs) throws SQLException {
            this.assetId = rs.getLong("asset_id");
            this.dbKey = privateAssetDbKeyFactory.newKey(this.assetId);
            this.orderFeePercentage = rs.getInt("orderFeePercentage");
            this.tradeFeePercentage = rs.getInt("tradeFeePercentage");
        }
    
        private void save(Connection con) throws SQLException {
            try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_asset "
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

        public long getAssetId() {
            return assetId;
        }

        public int getOrderFeePercentage() {
            return orderFeePercentage;
        }

        public int getTradeFeePercentage() {
            return tradeFeePercentage;
        }

        @Override
        public String toString() {
            return "PrivateAsset asset_id: " + Convert.toUnsignedLong(assetId)
                    + " order fee: " + orderFeePercentage + " trade fee: " + tradeFeePercentage;
        }

        public long calculateOrderFee(long amount) {
            return Convert.safeMultiply( 
                      Convert.safeDivide(
                          amount, 
                          100000000
                      ), 
                      getOrderFeePercentage()
                   );
        }       
    }
    
    public static class PrivateAssetAccount {
  
        private final long assetId;
        private final long accountId;          
        private boolean allowed;
        private final DbKey dbKey;
  
        private PrivateAssetAccount(long assetId, long accountId, boolean allowed) {
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
                    + "(account_id, asset_id, allowed, height, latest) "
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

        public long getAssetId() {
            return assetId;
        }        
        
        public long getAccountId() {
            return accountId;
        }
  
        public boolean getAllowed() {
            return allowed;
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
    
    public static PrivateAsset getPrivateAsset(long asset_id) {
        if (Asset.privateEnabled()) {
            return privateAssetTable.get(privateAssetDbKeyFactory.newKey(asset_id));
        }
        return null;
    }
  
    public static PrivateAssetAccount getPrivateAssetAccount(long assetId, long accountId) {
        if (Asset.privateEnabled()) {
            return privateAssetAccountTable.get(privateAssetAccountDbKeyFactory.newKey(assetId, accountId));
        }
        return null;
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
    
    static void init() {}
}
