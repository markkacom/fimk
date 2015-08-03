package nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import nxt.db.DbKey;
import nxt.db.VersionedEntityDbTable;

public class MofoVerificationAuthority {

    private DbKey dbKey;
    private long accountId;
    private int period;
    private int height;

    public static void init() {}
    
    private static final DbKey.LongKeyFactory<MofoVerificationAuthority> verificationAuthorityDbKeyFactory = new DbKey.LongKeyFactory<MofoVerificationAuthority>("account_id") {

        @Override
        public DbKey newKey(MofoVerificationAuthority verificationAuthority) {
            return verificationAuthority.dbKey;
        }
    };

    private static final VersionedEntityDbTable<MofoVerificationAuthority> verificationAuthorityTable = new VersionedEntityDbTable<MofoVerificationAuthority>("verification_authority", verificationAuthorityDbKeyFactory) {

        @Override
        protected MofoVerificationAuthority load(Connection con, ResultSet rs) throws SQLException {
            return new MofoVerificationAuthority(rs);
        }

        @Override
        protected void save(Connection con, MofoVerificationAuthority verificationAuthority) throws SQLException {
            verificationAuthority.save(con);
        }
    };

    public static void addOrUpdateVerificationAuthority(Transaction transaction, MofoAttachment.VerificationAuthorityAssignmentAttachment attachment) {
        MofoVerificationAuthority verificationAuthority = getVerificationAuthority(transaction.getRecipientId());
        if (verificationAuthority == null) {
            verificationAuthority = new MofoVerificationAuthority(transaction.getRecipientId(), attachment.getPeriod());
        } 
        else {
            verificationAuthority.period = attachment.getPeriod();
        }
        verificationAuthorityTable.insert(verificationAuthority);
    }
    
    public static MofoVerificationAuthority getVerificationAuthority(long account_id) {
        return verificationAuthorityTable.get(verificationAuthorityDbKeyFactory.newKey(account_id));
    }
    
    private MofoVerificationAuthority(long accountId, int period) {
        this.dbKey = verificationAuthorityDbKeyFactory.newKey(accountId);
        this.accountId = accountId;
        this.period = period;
        this.height = -1;
    }
  
    private MofoVerificationAuthority(ResultSet rs) throws SQLException {
        this.dbKey = verificationAuthorityDbKeyFactory.newKey(this.accountId);
        this.accountId = rs.getLong("account_id");
        this.period = rs.getInt("period");
        this.height = rs.getInt("height");
    }    
    
    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO verification_authority (account_id, period, height) "
                + "VALUES (?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setInt(++i, period);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public static boolean getIsVerificationAuthority(long account_id) {
        MofoVerificationAuthority verificationAuthority = getVerificationAuthority(account_id);
        if (verificationAuthority != null) {
            if (verificationAuthority.height == -1) {
                throw new RuntimeException("Critical exception please contact developers and report this bug");
            }
            return (verificationAuthority.height + verificationAuthority.period) > Nxt.getBlockchain().getHeight();
        }
        return false;
    }
    
    public int getPeriod() {
        return period;
    }
}
