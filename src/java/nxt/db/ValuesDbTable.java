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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class ValuesDbTable<T,V> extends DerivedDbTable {

    private final boolean multiversion;
    protected final DbKey.Factory<T> dbKeyFactory;

    protected ValuesDbTable(String table, DbKey.Factory<T> dbKeyFactory) {
        this(table, dbKeyFactory, false);
    }

    ValuesDbTable(String table, DbKey.Factory<T> dbKeyFactory, boolean multiversion) {
        super(table);
        this.dbKeyFactory = dbKeyFactory;
        this.multiversion = multiversion;
    }

    protected abstract V load(Connection con, ResultSet rs) throws SQLException;

    protected abstract void save(Connection con, T t, V v) throws SQLException;

    protected void clearCache() {
        db.getCache(table).clear();
    }

    public final List<V> get(DbKey dbKey) {
        List<V> values;
        if (db.isInTransaction()) {
            values = (List<V>) db.getCache(table).get(dbKey);
            if (values != null) {
                return values;
            }
        }
        try (Connection con = db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + table + dbKeyFactory.getPKClause()
                     + (multiversion ? " AND latest = TRUE" : "") + " ORDER BY db_id")) {
            dbKey.setPK(pstmt);
            values = get(con, pstmt);
            if (db.isInTransaction()) {
                db.getCache(table).put(dbKey, values);
            }
            return values;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private List<V> get(Connection con, PreparedStatement pstmt) {
        try {
            List<V> result = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(load(con, rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public final void insert(T t, List<V> values) {
        if (!db.isInTransaction()) {
            throw new IllegalStateException("Not in transaction");
        }
        DbKey dbKey = dbKeyFactory.newKey(t);
        db.getCache(table).put(dbKey, values);
        try (Connection con = db.getConnection()) {
            if (multiversion) {
                try (PreparedStatement pstmt = con.prepareStatement("UPDATE " + table
                        + " SET latest = FALSE " + dbKeyFactory.getPKClause() + " AND latest = TRUE")) {
                    dbKey.setPK(pstmt);
                    pstmt.executeUpdate();
                }
            }
            for (V v : values) {
                save(con, t, v);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public final void rollback(int height) {
        if (multiversion) {
            VersionedEntityDbTable.rollback(db, table, height, dbKeyFactory);
        } else {
            super.rollback(height);
            db.getCache(table).clear();
        }
    }

    @Override
    public final void trim(int height) {
        if (multiversion) {
            VersionedEntityDbTable.trim(db, table, height, dbKeyFactory);
        } else {
            super.trim(height);
        }
    }

    @Override
    public final void truncate() {
        super.truncate();
        db.getCache(table).clear();
    }

}
