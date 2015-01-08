package nxt;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import nxt.db.DbIterator;
import nxt.db.DbUtils;

public class MofoQueries {
  
    public static class TransactionFilter {
        int type;
        int subtype;
        
        public TransactionFilter(int type, int subtype) {
            this.type = type;
            this.subtype = subtype;
        }
        
        @Override
        public String toString() {
            return " NOT(type = "+this.type+" AND subtype = "+this.subtype+") ";
        }
    }
    
    public static DbIterator<? extends Block> getBlocks(int timestamp, int limit) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                    + " WHERE timestamp < ? " 
                    + " ORDER BY timestamp DESC LIMIT ?");
            int i = 0;
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            return Nxt.getBlockchain().getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static DbIterator<? extends Block> getBlocks(Account account, int timestamp, int limit) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                    + " WHERE generator_id = ? AND "
                    + " timestamp < ? "
                    + " ORDER BY timestamp DESC LIMIT ?");
            int i = 0;
            pstmt.setLong(++i, account.getId());
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            return Nxt.getBlockchain().getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Trade> getAccountAssetTradesBefore(Account account, int timestamp, int limit) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE seller_id = ? AND timestamp <= ?"
                    + " UNION ALL SELECT * FROM trade WHERE buyer_id = ? " 
                    + " AND seller_id <> ? "
                    + " AND timestamp < ? "
                    + " ORDER BY db_id DESC LIMIT ?");
            int i = 0;
            pstmt.setLong(++i, account.getId());
            pstmt.setInt(++i, timestamp);
            pstmt.setLong(++i, account.getId());
            pstmt.setLong(++i, account.getId());
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            return Trade.getTable().getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
  
    public static DbIterator<Trade> getAssetTradesBefore(int timestamp, int limit) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE timestamp < ?"
                    + " ORDER BY db_id DESC LIMIT ?");
            int i = 0;
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            return Trade.getTable().getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<? extends Transaction> getTransactions(int timestamp, int limit, List<TransactionFilter> filters) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction "
                    + " WHERE block_timestamp < ? "
                    + filterClause(filters)
                    + " ORDER BY db_id DESC LIMIT ?");
            
            int i = 0;
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static DbIterator<? extends Transaction> getTransactions(Account account, int timestamp, int limit, List<TransactionFilter> filters) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction "
                    + " WHERE block_timestamp < ? AND "
                    + " (recipient_id = ? OR sender_id = ?) "
                    + filterClause(filters)
                    + " ORDER BY db_id DESC LIMIT ?");
            
            int i = 0;
            pstmt.setInt(++i, timestamp);
            pstmt.setLong(++i, account.getId());
            pstmt.setLong(++i, account.getId());
            pstmt.setInt(++i, limit);
            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    static String filterClause(List<TransactionFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        
        StringBuilder b = new StringBuilder();
        b.append(" AND ");
        for (int i=0; i<filters.size(); i++) {
            if (i > 0) {
                b.append(" AND ");
            }
            b.append(filters.get(i).toString());
        }
          
        return b.toString();
    }
    
    final static int SECONDS_24H = 24 * 60 * 60;
    
    /* Average block time in the past 24 hours - very rude approach */
    public static int getAverageBlockTime24H() {        
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM block WHERE timestamp >= ?")) {
            pstmt.setInt(1, Nxt.getEpochTime() - SECONDS_24H);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                
                int totalBlocks24H = rs.getInt(1);
                return SECONDS_24H / totalBlocks24H;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }      
    }
    
    public static int getTransactionCountSince(int timestamp) {
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction WHERE timestamp >= ?")) {
            pstmt.setInt(1, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static class RewardsStruct {
        int timestamp;
        long sum;
        int count;
        
        public RewardsStruct(int timestamp, long l, int count) {
            this.timestamp = timestamp;
            this.sum = l;
            this.count = count;
        }
        
        public long getTotalRewards() {
            long total = this.sum;
            if (Nxt.APPLICATION == "FIMK") {
                total += count * (Constants.ONE_NXT * 200);
            }
            return total;
        }
    }

    public static RewardsStruct getBlockRewardsSince(int timestamp) {
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT " 
                + "CAST(SUM(total_fee) AS BIGINT) AS sum_a, "
                + "COUNT(*) AS count_a "
                + "FROM block WHERE timestamp >= ?")
        ) {
            pstmt.setInt(1, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return new RewardsStruct(timestamp, rs.getLong(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
