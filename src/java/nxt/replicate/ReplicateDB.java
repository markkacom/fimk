package nxt.replicate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;

import nxt.util.Logger;

public abstract class ReplicateDB {

    public static final class DbProperties {

        private String jdbcUrl;
        private String username;
        private String password;
        private int connectionTimeout = 30000; // 30 seconds
        private int idleTimeout = 600000; // 10 minutes
        private int maxLifetime = 1800000; // 30 minutes
        private int minimumIdle = 10; // minimum number of idle connections
        private int maximumPoolSize = 10; // maximum size that the pool is allowed to reach

        public DbProperties jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public DbProperties username(String username) {
            this.username = username;
            return this;
        }

        public DbProperties password(String password) {
            this.password = password;
            return this;
        }

        public DbProperties connectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public DbProperties idleTimeout(int idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        public DbProperties maxLifetime(int maxLifetime) {
            this.maxLifetime = maxLifetime;
            return this;
        }

        public DbProperties minimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
            return this;
        }

        public DbProperties maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }
    }

    private HikariPool cp;
    private String jdbcUrl;
    private String username;
    private String password;
    private int connectionTimeout;
    private int idleTimeout;
    private int maxLifetime;
    private int minimumIdle;
    private int maximumPoolSize;

    private volatile boolean initialized = false;

    public ReplicateDB(DbProperties dbProperties) {
        this.jdbcUrl = dbProperties.jdbcUrl;
        this.username = dbProperties.username;
        this.password = dbProperties.password;
        this.connectionTimeout = dbProperties.connectionTimeout;
        this.idleTimeout = dbProperties.idleTimeout;
        this.maxLifetime = dbProperties.maxLifetime;
        this.minimumIdle = dbProperties.minimumIdle;
        this.maximumPoolSize = dbProperties.maximumPoolSize;
    }

    public abstract void vendorInit();

    public abstract void vendorShutdown();

    public abstract IReplicator getReplicator();

    public abstract void dropTables();

    public void init(ReplicateDBVersion dbVersion) {
        Logger.logDebugMessage("Replicated database jdbc url set to %s username %s", jdbcUrl, username);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setAutoCommit(true);
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(connectionTimeout));
        config.setIdleTimeout(TimeUnit.SECONDS.toMillis(idleTimeout));
        config.setMaxLifetime(TimeUnit.SECONDS.toMillis(maxLifetime));
        config.setMinimumIdle(minimumIdle);
        config.setMaximumPoolSize(maximumPoolSize);

        cp = new HikariPool(config);

        vendorInit();
        dbVersion.init(this);
        initialized = true;
    }

    public void shutdown() {
        if (!initialized) {
            return;
        }
        vendorShutdown();
        Logger.logShutdownMessage("Replicated database shutdown completed");
    }

    public Connection getConnection() throws SQLException {
        Connection con = getPooledConnection();
        con.setAutoCommit(true);
        return con;
    }

    protected Connection getPooledConnection() throws SQLException {
        Connection con = cp.getConnection();
        int activeConnections = cp.getActiveConnections();
        if (activeConnections > maximumPoolSize) {
            maximumPoolSize = activeConnections;
            Logger.logDebugMessage("Replicated database connection pool current size: " + activeConnections);
        }
        return con;
    }
}
