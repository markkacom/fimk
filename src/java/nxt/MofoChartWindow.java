package nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MofoChartWindow {
  
    static final int ONE_HOUR_SECONDS = 60 * 60;
    static final int ONE_DAY_SECONDS  = 24 * ONE_HOUR_SECONDS;
    static final int ONE_WEEK_SECONDS = 7 * ONE_DAY_SECONDS;    

    public static final byte HOUR = 1;
    public static final byte DAY  = 2;
    public static final byte WEEK = 3;
    
    private List<Trade> trades = null;
    private byte window;
    private long asset_id;
    private long openNQT;
    private long highNQT;
    private long lowNQT;
    private long closeNQT;
    private long averageNQT;
    private long volumeQNT;
    private int height;
    private int timestamp;
  
    public MofoChartWindow(long assetId, byte period) {
        this.asset_id = assetId;
        this.window = period;
    }

    public MofoChartWindow(ResultSet rs) throws SQLException {
        this.asset_id = rs.getLong("asset_id");
        this.timestamp = rs.getInt("timestamp");
        this.window = rs.getByte("window");
        this.openNQT = rs.getLong("openNQT");
        this.highNQT = rs.getLong("highNQT");
        this.lowNQT = rs.getLong("lowNQT");
        this.closeNQT = rs.getLong("closeNQT");
        this.averageNQT = rs.getLong("averageNQT");
        this.volumeQNT = rs.getLong("volumeQNT");
        this.height = rs.getInt("height");
    }
    
    void save() {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                 "MERGE INTO mofo_asset_chart (asset_id, timestamp, window, openNQT, highNQT, "
               + "lowNQT, closeNQT, averageNQT, volumeQNT, height) "
               + "KEY (asset_id, window, height) "
               + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) 
        {
            int i = 0;
            pstmt.setLong(++i, asset_id);
            pstmt.setInt(++i, timestamp);
            pstmt.setByte(++i, window);          
            pstmt.setLong(++i, openNQT);
            pstmt.setLong(++i, highNQT);
            pstmt.setLong(++i, lowNQT);
            pstmt.setLong(++i, closeNQT);
            pstmt.setLong(++i, averageNQT);
            pstmt.setLong(++i, volumeQNT);
            pstmt.setLong(++i, height);
  
            pstmt.executeUpdate();
        } 
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public boolean surpassWindow(Trade trade) {
        if (trades != null) {
          int elapsed = trade.getTimestamp() - trades.get(0).getTimestamp();
          switch (window) {
          case HOUR:
              return elapsed > ONE_HOUR_SECONDS;
          case DAY:
              return elapsed > ONE_DAY_SECONDS;       
          case WEEK:
              return elapsed > ONE_WEEK_SECONDS;
          }            
        }
        return false;
    }
    
    public void add(Trade trade) {
        if (trades == null) {
            trades = new ArrayList<Trade>();
            height = trade.getHeight();
            timestamp = trade.getTimestamp();
        }      
        trades.add(trade);
    }
    
    /* TODO - use VOLUME WEIGHTED AVERAGE PRICE instead */
    public void accumulate() {
        openNQT    = trades.get(0).getPriceNQT();
        highNQT    = openNQT;
        lowNQT     = openNQT;
        closeNQT   = trades.get(trades.size() - 1).getPriceNQT();
        averageNQT = 0;
        volumeQNT  = 0;
        
        for (int i=0; i<trades.size(); i++) {
            long _priceNQT    = trades.get(i).getPriceNQT();
            long _quantityQNT = trades.get(i).getQuantityQNT();
            
            volumeQNT += _quantityQNT;    
            
            if (_priceNQT < lowNQT) {
                lowNQT = _priceNQT;
            }
            else if (_priceNQT > highNQT) {
                highNQT = _priceNQT;
            }            
        }
        
        /* XXX This is wrong.. Please look up how to calculate a proper average */
        averageNQT = closeNQT;
        save();
    }
    
    public byte getPeriod() {
        return window;
    }
    
    public long getAssetId() {
        return asset_id;
    }
    
    public long getOpenNQT() {
        return openNQT;
    }
    
    public long getHighNQT() {
        return highNQT;
    }
    
    public long getLowNQT() {
        return lowNQT;
    }
    
    public long getCloseNQT() {
        return closeNQT;
    }
    
    public long getAverageNQT() {
        return averageNQT;
    }
    
    public long getVolumeQNT() {
        return volumeQNT;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getTimestamp() {
        return timestamp;
    }
}