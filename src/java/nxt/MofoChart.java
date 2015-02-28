package nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.util.Listener;
import nxt.util.Logger;

public class MofoChart {
    
    static final Map<Long, MofoChartWindow> one_hour_windows = new HashMap<Long, MofoChartWindow>();
    static final Map<Long, MofoChartWindow> one_day_windows = new HashMap<Long, MofoChartWindow>();
    static final Map<Long, MofoChartWindow> one_week_windows = new HashMap<Long, MofoChartWindow>();
    
    static void init() {
        Logger.logDebugMessage("Begin building asset price chart table");
        
        int timestamp = getLatestWindowTimestamp();
        try (DbIterator<Trade> trades = getTrades(timestamp)) {
            int count = 0;          
            while (trades.hasNext()) {
                count++;
                if (count % 500 == 0) {
                    Logger.logDebugMessage("Processed "+count+" trades");
                }
                addTrade(trades.next());                
            }
        }
        Logger.logDebugMessage("Done building asset price chart table");
        registerListeners();
    }    
    
    static void addTrade(Trade trade) {
        Long assetIdKey = Long.valueOf(trade.getAssetId());
      
        MofoChartWindow hour_window = one_hour_windows.get(assetIdKey);
        if (hour_window == null) {
            hour_window = new MofoChartWindow(assetIdKey, MofoChartWindow.HOUR);
            one_hour_windows.put(assetIdKey, hour_window);
        }
        
        MofoChartWindow day_window = one_day_windows.get(assetIdKey);
        if (day_window == null) {
            day_window = new MofoChartWindow(assetIdKey, MofoChartWindow.DAY);
            one_day_windows.put(assetIdKey, day_window);
        }        

        MofoChartWindow week_window = one_week_windows.get(assetIdKey);
        if (week_window == null) {
            week_window = new MofoChartWindow(assetIdKey, MofoChartWindow.WEEK);
            one_week_windows.put(assetIdKey, week_window);
        }
        
        if (hour_window.surpassWindow(trade)) {
            hour_window.accumulate();
            one_hour_windows.remove(assetIdKey);
        }
        else {
            hour_window.add(trade);
        }
        
        if (day_window.surpassWindow(trade)) {
            day_window.accumulate();
            one_day_windows.remove(assetIdKey);
        }
        else {
            day_window.add(trade);
        }
        
        if (week_window.surpassWindow(trade)) {
            week_window.accumulate();
            one_week_windows.remove(assetIdKey);
        }
        else {
            week_window.add(trade);
        }
    }
    
    static void registerListeners() {
        Trade.addListener(new Listener<Trade>() {    
            @Override
            public void notify(Trade trade) {
                MofoChart.addTrade(trade); 
            }
        }, Trade.Event.TRADE);
    
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {              
                MofoChart.rollback(block.getHeight());
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED); 
    }
    
    static int getLatestWindowTimestamp() {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT timestamp "
                    + "FROM mofo_asset_chart "
                    + "ORDER BY timestamp DESC LIMIT 1");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }            
            return 0;
        } 
        catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    static DbIterator<Trade> getTrades(int timestamp) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                "SELECT * FROM trade WHERE timestamp >= ? ORDER BY timestamp");
                    
            int i = 0;
            pstmt.setInt(++i, timestamp);
            return Trade.getTable().getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    static void rollback(int height) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmtDelete = con.prepareStatement(
                 "DELETE FROM mofo_asset_chart WHERE height > ?")) {
             pstmtDelete.setInt(1, height);
             pstmtDelete.executeUpdate();
        } 
        catch (SQLException e) {
             throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static List<MofoChartWindow> getChartData(long asset_id, byte window, int timestamp, int count) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * "
                    + "FROM mofo_asset_chart "
                    + "WHERE asset_id = ? AND window = ? AND timestamp < ? "
                    + "ORDER BY timestamp DESC LIMIT ?");)
        {
            
            int i = 0;
            pstmt.setLong(++i, asset_id);
            pstmt.setByte(++i, window);
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, count);
            
            List<MofoChartWindow> result = new ArrayList<MofoChartWindow>();
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new MofoChartWindow(rs));
                }
            }            
            return result;
        } 
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
