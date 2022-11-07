package nxt.reward;

import nxt.Db;
import nxt.Nxt;
import nxt.Transaction;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.EntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Register candidate for asset rewarding.
 * Stage 1: account sends transaction to register the account and node for rewarding. The not completed record is created.
 * Stage 2: reward giver node handles the registration transaction - generate token, send transaction with token back to the account.
 */
public final class AccountNode {

    /**
     * Account used to register the applicant. Applicant sends transaction to this account to register itself
     */
    public static long REGISTRATION_ACCOUNT_ID;
    public static int MIN_SCORE = -20;

    private static int PAUSE_PENALTY_SECONDS = 30;
    private static int MONTH_SECONDS = 2_678_400;
    private static int REGISTRATION_AGE_LIMIT = 6 * MONTH_SECONDS;

    static {
        String s = Nxt.getStringProperty("fimk.popReward.registrationAccount");
        if (s != null) REGISTRATION_ACCOUNT_ID = Long.parseUnsignedLong(s);
    }

    public static void init() {
    }

    private static final DbKey.LongKeyFactory<AccountNode> accountNodeDbKeyFactory = new DbKey.LongKeyFactory<AccountNode>("transaction_id") {
        @Override
        public DbKey newKey(AccountNode accountNode) {
            return accountNode.dbKey;
        }
    };

    private static final EntityDbTable<AccountNode> accountNodeTable = new EntityDbTable<AccountNode>("account_node", accountNodeDbKeyFactory) {
        @Override
        protected AccountNode load(Connection con, ResultSet rs) throws SQLException {
            return new AccountNode(rs);
        }

        @Override
        protected void save(Connection con, AccountNode accountNode) throws SQLException {
            accountNode.save(con);
        }
    };

    public static void save(Transaction transaction, long registerAccountId, long accountId, String address, String token) {
        accountNodeTable.insert(new AccountNode(transaction, registerAccountId, accountId, address, token));
    }

    public static void update(List<AccountNode> accountNodes) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "UPDATE account_node SET score = ?, request_peer_timestamp = ? WHERE transaction_id = ?")) {
            for (AccountNode accountNode : accountNodes) {
                pstmt.setInt(1, accountNode.getScore());
                pstmt.setLong(2, accountNode.getRequestPeerTimestamp());
                pstmt.setLong(3, accountNode.getTransactionId());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static DbIterator<AccountNode> getAll(int from, int to) {
        return accountNodeTable.getAll(from, to);
    }

    public static int getCount() {
        return accountNodeTable.getCount();
    }

    public static List<AccountNode> getActualAccountNodes() {
        Connection con = null;
        DbIterator<AccountNode> iterator;
        int now = Nxt.getEpochTime();
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(
                    "SELECT * FROM account_node WHERE timestamp > ? ORDER BY timestamp"
            );
            pstmt.setInt(1, now - REGISTRATION_AGE_LIMIT);
            iterator = accountNodeTable.getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }

        ArrayList<AccountNode> result = new ArrayList<>();
        for (AccountNode accountNode : iterator) {
            if (accountNode.registrationAccountId == AccountNode.REGISTRATION_ACCOUNT_ID
                    && accountNode.getPenaltyTimestamp() < now) {
                result.add(accountNode);
            }
        }

        return result;
    }


    private final DbKey dbKey;
    int timestamp;  // timestamp of creation (transaction timestamp)
    long transactionId;
    int height;
    long registrationAccountId;
    long accountId;
    String address;
    String token;
    int score;
    int requestPeerTimestamp; // timestamp of last request to peer
    int roundScore;

    public AccountNode(Transaction transaction, long registerAccountId, long accountId, String address, String token) {
        this.dbKey = accountNodeDbKeyFactory.newKey(this.transactionId);
        this.transactionId = transaction.getId();
        this.height = transaction.getHeight();
        this.timestamp = transaction.getTimestamp();
        this.registrationAccountId = registerAccountId;
        this.accountId = accountId;
        this.address = address;
        this.token = token;
    }

    private AccountNode(ResultSet rs) throws SQLException {
        this.transactionId = rs.getLong("transaction_id");
        this.height = rs.getInt("height");
        this.dbKey = accountNodeDbKeyFactory.newKey(this.transactionId);
        this.accountId = rs.getLong("account_id");
        this.address = rs.getString("address");
        this.token = rs.getString("token");
        this.requestPeerTimestamp = rs.getInt("request_peer_timestamp");
        this.timestamp = rs.getInt("timestamp");
        this.score = rs.getInt("score");
        this.registrationAccountId = rs.getLong("registration_account_id");
    }

    /*
    CREATE TABLE IF NOT EXISTS account_node (
    db_id IDENTITY,
    applicant_transaction_id BIGINT NOT NULL,
    address VARCHAR NOT NULL,
    account_id BIGINT NOT NULL,
    applicant_timestamp INT NOT NULL,
    token VARCHAR,
    token_transaction_id BIGINT,
    token_height INT,
    token_sender_id BIGINT,
    token_timestamp INT,
    score TINYINT,
    request_peer_timestamp INT);
    CREATE UNIQUE INDEX IF NOT EXISTS account_node_address_account_id_idx
    ON account_node (address, account_id);
    CREATE INDEX IF NOT EXISTS account_node_timestamp_idx ON account_node (applicant_timestamp);
    */
    private void save(Connection con) throws SQLException {
        // overwrite previous record on same address and account
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO account_node " +
                "(address, account_id, token, transaction_id, score, request_peer_timestamp, timestamp, " +
                "height, registration_account_id) "
                + "KEY (address, account_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ")) {
            int i = 0;
            pstmt.setString(++i, this.address);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.token);
            pstmt.setLong(++i, this.transactionId);
            pstmt.setInt(++i, this.score);
            pstmt.setInt(++i, this.requestPeerTimestamp);
            pstmt.setInt(++i, this.timestamp);
            pstmt.setLong(++i, this.height);
            pstmt.setLong(++i, this.registrationAccountId);
            pstmt.executeUpdate();
        }
    }

    public String getAddress() {
        return address;
    }

    public String getToken() {
        return token;
    }

    public int getScore() {
        return score;
    }

    public int getRequestPeerTimestamp() {
        return requestPeerTimestamp;
    }

    public int getRoundScore() {
        return roundScore;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getRegistrationAccountId() {
        return registrationAccountId;
    }

    public void setRoundScore(int roundScore) {
        this.roundScore = roundScore;
    }

    public void updateRequestTimestamp() {
        requestPeerTimestamp = Nxt.getEpochTime();
    }

    public void updateScore() {
        score = roundScore;
    }

    public int getPenaltyTimestamp() {
        // for example score=-2 PAUSE_PENALTY_SECONDS=30 so the penalty time requestPeerTimestamp+60
        return getRequestPeerTimestamp() - getScore() * PAUSE_PENALTY_SECONDS;
    }
}
