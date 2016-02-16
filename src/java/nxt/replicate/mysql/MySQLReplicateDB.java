package nxt.replicate.mysql;

import nxt.replicate.IReplicator;
import nxt.replicate.ReplicateDB;

public class MySQLReplicateDB extends ReplicateDB {

    /**
     * Installing mysql
     *
     * For server setup use chef mysql cookbook which includes all required
     * security setups.
     *
     * For local testing usage:
     *
     *      sudo apt-get install mysql-server
     *
     *      # Run the mysql_secure_installation script to address several security
     *      # concerns in a default MySQL installation.
     *      # NOT SURE IF NEEDED ON LOCALHOST ONLY
     *      sudo mysql_secure_installation
     *
     * For the rest see the following guide
     *
     * https://www.linode.com/docs/databases/mysql/how-to-install-mysql-on-ubuntu-14-04
     *
     * To start/stop mysql
     *
     *      sudo service mysql stop
     *      sudo service mysql start
     *
     * To prevent mysql from starting at startup.
     *
     *      echo manual | sudo tee /etc/init/mysql.override
     *
     * Setting up mysql
     * @see http://www.vogella.com/tutorials/MySQLJava/article.html
     *
     * Connect to mysql server
     *
     *      mysql -u root
     *
     * Create a new database called fimk_replicate and start using it with the
     * following command.
     *
     *      CREATE DATABASE fimk_replicate;
     *      USE fimk_replicate;
     *
     * Create a user with the following command.
     *
     *      CREATE USER sqluser IDENTIFIED BY 'sqluserpw';
     *
     *      GRANT USAGE on *.* TO sqluser@localhost IDENTIFIED BY 'sqluserpw';
     *      GRANT ALL PRIVILEGES ON fimk_replicate.* to sqluser@localhost;
     *      exit;
     *
     * Remove the fimk_replicate database
     *
     *      DROP DATABASE IF EXISTS fimk_replicate
     *
     * Remove all tables
     *
USE fimk_replicate;
SET FOREIGN_KEY_CHECKS = 0;
SET GROUP_CONCAT_MAX_LEN=32768;
SET @tables = NULL;
SELECT GROUP_CONCAT('`', table_name, '`') INTO @tables
  FROM information_schema.tables
  WHERE table_schema = (SELECT DATABASE());
SELECT IFNULL(@tables,'dummy') INTO @tables;

SET @tables = CONCAT('DROP TABLE IF EXISTS ', @tables);
PREPARE stmt FROM @tables;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET FOREIGN_KEY_CHECKS = 1;
     *
     */

    public static final String VENDOR_TYPE = "mysql";
    private final IReplicator replicator;

    public MySQLReplicateDB(DbProperties dbProperties) {
        super(dbProperties);
        replicator = new MySQLReplicator(this);
    }

    @Override
    public void dropTables() {
        replicator.rescanBegin(0);
    }

    @Override
    public IReplicator getReplicator() {
        return replicator;
    }

    @Override
    public void vendorInit() {
    }

    @Override
    public void vendorShutdown() {
    }
}
