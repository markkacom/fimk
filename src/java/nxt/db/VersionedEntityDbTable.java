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

package nxt.db;


import nxt.Nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class VersionedEntityDbTable<T> extends EntityDbTable<T> {

    protected VersionedEntityDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        super(table, dbKeyFactory, true, null);
    }

    protected VersionedEntityDbTable(String table, DbKey.Factory<T> dbKeyFactory, String fullTextSearchColumns) {
        super(table, dbKeyFactory, true, fullTextSearchColumns);
    }

    public final boolean delete(T t) {
        if (t == null) {
            return false;
        }
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        try (Connection con = db.getConnection();
             PreparedStatement pstmtCount = con.prepareStatement("SELECT COUNT(*) AS count FROM " + table + dbKeyFactory.getPKClause()
                + " AND height < ?")) {
            int i = dbKey.setPK(pstmtCount);
            pstmtCount.setInt(i, Nxt.getBlockchain().getHeight());
            try (ResultSet rs = pstmtCount.executeQuery()) {
                rs.next();
                if (rs.getInt("count") > 0) {
                    try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                            + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE LIMIT 1")) {
                        dbKey.setPK(pstmt);
                        pstmt.executeUpdate();
                        save(con, t);
                        pstmt.executeUpdate(); // delete after the save
                    }
                    return true;
                } else {
                    try (PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + dbKeyFactory.getPKClause())) {
                        dbKey.setPK(pstmtDelete);
                        return pstmtDelete.executeUpdate() > 0;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        } finally {
            db.getCache(table).remove(dbKey);
        }
    }

    static void rollback(final TransactionalDb db, final String table, final int height, final DbKey.Factory dbKeyFactory) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = db.getConnection();
             PreparedStatement pstmtSelectToDelete = con.prepareStatement("SELECT DISTINCT " + dbKeyFactory.getPKColumns()
                     + " FROM " + table + " WHERE height > ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table
                     + " WHERE height > ?");
             PreparedStatement pstmtSetLatest = con.prepareStatement("UPDATE " + table
                     + " SET latest = TRUE " + dbKeyFactory.getPKClause() + " AND height ="
                     + " (SELECT MAX(height) FROM " + table + dbKeyFactory.getPKClause() + ")")) {
            pstmtSelectToDelete.setInt(1, height);
            List<DbKey> dbKeys = new ArrayList<>();
            try (ResultSet rs = pstmtSelectToDelete.executeQuery()) {
                while (rs.next()) {
                    dbKeys.add(dbKeyFactory.newKey(rs));
                }
            }
            /*
            if (dbKeys.size() > 0 && Logger.isDebugEnabled()) {
                Logger.logDebugMessage(String.format("rollback table %s found %d records to update to latest", table, dbKeys.size()));
            }
            */
            pstmtDelete.setInt(1, height);
            int deletedRecordsCount = pstmtDelete.executeUpdate();
            /*
            if (deletedRecordsCount > 0 && Logger.isDebugEnabled()) {
                Logger.logDebugMessage(String.format("rollback table %s deleting %d records", table, deletedRecordsCount));
            }
            */
            for (DbKey dbKey : dbKeys) {
                int i = 1;
                i = dbKey.setPK(pstmtSetLatest, i);
                i = dbKey.setPK(pstmtSetLatest, i);
                pstmtSetLatest.executeUpdate();
                //Db.getCache(table).remove(dbKey);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        db.getCache(table).clear();
    }

    static void trim(final TransactionalDb db, final String table, final int height, final DbKey.Factory dbKeyFactory) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        try (Connection con = db.getConnection();
             PreparedStatement pstmtSelect = con.prepareStatement("SELECT " + dbKeyFactory.getPKColumns() + ", MAX(height) AS max_height"
                     + " FROM " + table + " WHERE height < ? GROUP BY " + dbKeyFactory.getPKColumns() + " HAVING COUNT(DISTINCT height) > 1");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM " + table + dbKeyFactory.getPKClause()
                     + " AND height < ? AND height >= 0");
            PreparedStatement pstmtDeleteDeleted = con.prepareStatement("DELETE FROM " + table + " WHERE height < ? AND height >= 0 AND latest = FALSE "
                    + " AND (" + dbKeyFactory.getPKColumns() + ") NOT IN (SELECT " + dbKeyFactory.getPKColumns() + " FROM "
                    + table + " WHERE height >= ?)")) {
            pstmtSelect.setInt(1, height);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                while (rs.next()) {
                    DbKey dbKey = dbKeyFactory.newKey(rs);
                    int maxHeight = rs.getInt("max_height");
                    int i = 1;
                    i = dbKey.setPK(pstmtDelete, i);
                    pstmtDelete.setInt(i, maxHeight);
                    pstmtDelete.executeUpdate();
                }
                pstmtDeleteDeleted.setInt(1, height);
                pstmtDeleteDeleted.setInt(2, height);
                pstmtDeleteDeleted.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
