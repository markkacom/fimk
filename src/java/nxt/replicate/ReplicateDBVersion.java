package nxt.replicate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import nxt.Nxt;
import nxt.db.DbUtils;
import nxt.util.Logger;

public abstract class ReplicateDBVersion {

    private ReplicateDB db;

    void init(ReplicateDB db) {
        this.db = db;
        Connection con = null;
        Statement stmt = null;
        try {
            con = db.getConnection();
            stmt = con.createStatement();
            int nextUpdate = 1;
            try {
                ResultSet rs = stmt.executeQuery("SELECT next_update FROM version");
                if (! rs.next()) {
                    throw new RuntimeException("Invalid replicated version table");
                }
                nextUpdate = rs.getInt("next_update");
                if (! rs.isLast()) {
                    throw new RuntimeException("Invalid replicated version table");
                }
                int overrideDbVersion = Nxt.getIntProperty("nxt.debugOverrideReplicatedDbVersion");
                if (overrideDbVersion != 0) {
                    Logger.logMessage("Overriding replicated db version "+nextUpdate+" with "+overrideDbVersion);
                    nextUpdate = overrideDbVersion;
                    stmt.executeUpdate("UPDATE version SET next_update = " + nextUpdate);
                }
                rs.close();
                Logger.logMessage("Replicated database update may take a while if needed, current db version " + (nextUpdate - 1) + "...");
            } catch (SQLException e) {
                Logger.logMessage("Initializing an empty replicated database");
                stmt.executeUpdate("CREATE TABLE version (next_update INT NOT NULL)");
                stmt.executeUpdate("INSERT INTO version VALUES (1)");
            }
            update(nextUpdate);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            DbUtils.close(stmt, con);
        }
    }

    protected void apply(String sql) {
        Connection con = null;
        Statement stmt = null;
        try {
            con = db.getConnection();
            stmt = con.createStatement();
            try {
                if (sql != null) {
                    Logger.logDebugMessage("Replicated databse, will apply sql:\n" + sql);
                    stmt.executeUpdate(sql);
                }
                stmt.executeUpdate("UPDATE version SET next_update = next_update + 1");
            } catch (Exception e) {
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Replicated database error executing " + sql, e);
        } finally {
            DbUtils.close(stmt, con);
        }
    }

    protected abstract void update(int nextUpdate);
}
