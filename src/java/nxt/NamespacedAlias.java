package nxt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;
import nxt.db.DbKey.LinkKeyFactory;

public final class NamespacedAlias {

    private static final DbKey.LongKeyFactory<NamespacedAlias> aliasDbKeyFactory = new DbKey.LongKeyFactory<NamespacedAlias>("id") {
    
        @Override
        public DbKey newKey(NamespacedAlias alias) {
            return alias.dbKey;
        }
    
    };
    
    private static final VersionedEntityDbTable<NamespacedAlias> aliasTable = new VersionedEntityDbTable<NamespacedAlias>("namespaced_alias", aliasDbKeyFactory) {
    
        @Override
        protected NamespacedAlias load(Connection con, ResultSet rs) throws SQLException {
            return new NamespacedAlias(rs);
        }
    
        @Override
        protected void save(Connection con, NamespacedAlias alias) throws SQLException {
            alias.save(con);
        }
    
        @Override
        protected String defaultSort() {
            return " ORDER BY alias_name_lower ";
        }
    
    };

    public static DbIterator<NamespacedAlias> getAliasesByOwner(long accountId, int from, int to) {
        return aliasTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    public static NamespacedAlias getAlias(Account sender, String aliasName) {
        return aliasTable.getBy(new DbClause.StringClause("alias_name_lower", aliasName.toLowerCase()));
    }

    public static NamespacedAlias getAlias(Long id) {
        return aliasIdToAliasMappings.get(id);
    }

    static void addOrUpdateAlias(Account account, Long transactionId, String aliasName, String aliasURI, int timestamp) {
        String normalizedAlias = aliasName.toLowerCase();
        NamespacedAlias oldAlias = aliases.get(NamespacedKey.valueOf(account.getId(), normalizedAlias));
        if (oldAlias == null) {
            NamespacedAlias newAlias = new NamespacedAlias(account, transactionId, aliasName, aliasURI, timestamp);
            aliases.put(NamespacedKey.valueOf(account.getId(), normalizedAlias), newAlias);
            aliasIdToAliasMappings.put(transactionId, newAlias);
        } else {
            oldAlias.aliasURI = aliasURI.intern();
            oldAlias.timestamp = timestamp;
        }
    }

    static void remove(NamespacedAlias alias) {
        aliases.remove(NamespacedKey.valueOf(alias.getAccountId(), alias.getAliasName().toLowerCase()));
        aliasIdToAliasMappings.remove(alias.getId());
    }

    static void clear() {
        aliases.clear();
        aliasIdToAliasMappings.clear();
    }

    private final Long accountId;
    private final Long id;
    private final String aliasName;
    private volatile String aliasURI;
    private volatile int timestamp;

    private NamespacedAlias(Account account, Long id, String aliasName, String aliasURI, int timestamp) {
        this.accountId = account.getId();
        this.id = id;
        this.aliasName = aliasName.intern();
        this.aliasURI = aliasURI.intern();
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getAliasURI() {
        return aliasURI;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Long getAccountId() {
        return accountId;
    }

    public static boolean isEnabled() {
        if(Nxt.getBlockchain().getLastBlock().getHeight() >= Constants.NAMESPACED_ALIAS_BLOCK) {
            return true;
        }
        return false;
    }
}