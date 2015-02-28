package nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;

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
    
    public static int getCount() {
        return aliasTable.getCount();
    }
    
    public static int getAccountAliasCount(long accountId) {
        return aliasTable.getCount(new DbClause.LongClause("account_id", accountId));
    }
    
    public static DbIterator<NamespacedAlias> getAliasesByOwner(long accountId, int from, int to) {
        return aliasTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }
    
    public static NamespacedAlias getAlias(final long sender_id, final String aliasName) {
        DbClause dbClause = new DbClause(" account_id = ? AND alias_name_lower = ? ") {
            @Override
            public int set(PreparedStatement pstmt, int index) throws SQLException {
                pstmt.setLong(index++, sender_id);
                pstmt.setString(index++, aliasName);
                return index;
            }
        };      
        return aliasTable.getBy(dbClause);
    }
    
    public static NamespacedAlias getAlias(long id) {
        return aliasTable.get(aliasDbKeyFactory.newKey(id));
    }
    
    static void addOrUpdateAlias(Transaction transaction, MofoAttachment.NamespacedAliasAssignmentAttachment attachment) {
      NamespacedAlias alias = getAlias(transaction.getSenderId(), attachment.getAliasName());
        if (alias == null) {
            alias = new NamespacedAlias(transaction.getId(), transaction, attachment);
        } else {
            alias.aliasURI = attachment.getAliasURI();
            alias.timestamp = transaction.getBlockTimestamp();
        }
        aliasTable.insert(alias);
    }
    
    static void init() {}
    
    private long accountId;
    private final long id;
    private final DbKey dbKey;
    private final String aliasName;
    private String aliasURI;
    private int timestamp;
    
    private NamespacedAlias(long id, long accountId, String aliasName, String aliasURI, int timestamp) {
      this.id = id;
      this.dbKey = aliasDbKeyFactory.newKey(this.id);
      this.accountId = accountId;
      this.aliasName = aliasName;
      this.aliasURI = aliasURI;
      this.timestamp = timestamp;
    }    

    private NamespacedAlias(long aliasId, Transaction transaction, MofoAttachment.NamespacedAliasAssignmentAttachment attachment) {
      this(aliasId, transaction.getSenderId(), attachment.getAliasName(), attachment.getAliasURI(),
          transaction.getBlockTimestamp());
    }
    
    private NamespacedAlias(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.dbKey = aliasDbKeyFactory.newKey(this.id);
        this.accountId = rs.getLong("account_id");
        this.aliasName = rs.getString("alias_name");
        this.aliasURI = rs.getString("alias_uri");
        this.timestamp = rs.getInt("timestamp");
    }
    
    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO namespaced_alias (id, account_id, alias_name, "
                + "alias_uri, timestamp, height) "
                + "VALUES (?, ?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.getId());
            pstmt.setLong(++i, this.getAccountId());
            pstmt.setString(++i, this.getAliasName());
            pstmt.setString(++i, this.getAliasURI());
            pstmt.setInt(++i, this.getTimestamp());
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }
    
    public long getId() {
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
    
    public long getAccountId() {
        return accountId;
    }

    public static boolean isEnabled() {
        if(Nxt.getBlockchain().getLastBlock().getHeight() >= Constants.NAMESPACED_ALIAS_BLOCK) {
            return true;
        }
        return false;
    }
}