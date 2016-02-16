package nxt.replicate.mysql;

import nxt.replicate.ReplicateDBVersion;

public class MySQLReplicateDBVersion extends ReplicateDBVersion {

    @Override
    protected void update(int nextUpdate) {
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE IF NOT EXISTS account (db_id BIGINT NOT NULL AUTO_INCREMENT, "
                        + "id BIGINT NOT NULL, "
                        + "balance BIGINT NOT NULL, "
                        + "unconfirmed_balance BIGINT NOT NULL, "
                        + "forged_balance BIGINT NOT NULL, "
                        + "current_leasing_height_from INT, "
                        + "current_leasing_height_to INT, "
                        + "current_lessee_id BIGINT NULL, "
                        + "next_leasing_height_from INT, "
                        + "next_leasing_height_to INT, "
                        + "next_lessee_id BIGINT NULL, "
                        + "height INT NOT NULL,"
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE, "
                        + "PRIMARY KEY (db_id))");
            case 2:
                apply("CREATE UNIQUE INDEX account_id_height_idx ON account (id, height DESC)");
            case 3:
                apply("CREATE TABLE IF NOT EXISTS block (id BIGINT NOT NULL, "
                        + "timestamp INT NOT NULL, "
                        + "total_amount BIGINT NOT NULL, "
                        + "total_fee BIGINT NOT NULL, "
                        + "height INT NOT NULL, "
                        + "generator_id BIGINT NOT NULL, "
                        + "PRIMARY KEY (id))");
            case 4:
                apply("CREATE UNIQUE INDEX block_id_idx ON block (id)");
            case 5:
                apply("CREATE UNIQUE INDEX block_height_idx ON block (height)");
            case 6:
                apply("CREATE INDEX block_generator_id_idx ON block (generator_id)");
            case 7:
                apply("CREATE TABLE IF NOT EXISTS public_key (db_id BIGINT NOT NULL AUTO_INCREMENT, "
                        + "account_id BIGINT NOT NULL, "
                        + "public_key BINARY(32), "
                        + "height INT NOT NULL, "
                        + "FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, "
                        + "PRIMARY KEY (db_id))");
            case 8:
                apply("CREATE UNIQUE INDEX public_key_account_id_idx ON public_key (account_id)");
            case 9:
                apply("CREATE TABLE IF NOT EXISTS transaction (id BIGINT NOT NULL, "
                        + "deadline SMALLINT NOT NULL, "
                        + "recipient_id BIGINT, "
                        + "amount BIGINT NOT NULL, "
                        + "fee BIGINT NOT NULL, "
                        + "height INT NOT NULL, "
                        + "block_id BIGINT NOT NULL, "
                        + "FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, "
                        + "signature BINARY(64) NOT NULL, "
                        + "timestamp INT NOT NULL, "
                        + "type TINYINT NOT NULL, "
                        + "subtype TINYINT NOT NULL, "
                        + "sender_id BIGINT NOT NULL, "
                        + "block_timestamp INT NOT NULL, "
                        + "referenced_transaction_full_hash BINARY(32), "
                        + "attachment_bytes VARBINARY(43016), " // Constants.MAX_PRUNABLE_MESSAGE_LENGTH = 42 * 1024; + 8
                        + "transaction_version TINYINT NOT NULL, "
                        + "has_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "transaction_index SMALLINT NOT NULL, "
                        + "PRIMARY KEY (id))");
            case 10:
                apply("CREATE UNIQUE INDEX transaction_id_idx ON transaction (id)");
            case 11:
                apply("CREATE INDEX transaction_sender_id_idx ON transaction (sender_id)");
            case 12:
                apply("CREATE INDEX transaction_recipient_id_idx ON transaction (recipient_id)");
            case 13:
                apply("CREATE INDEX transaction_timestamp_desc_idx ON transaction (timestamp DESC)");
            case 14:
                apply("CREATE TABLE IF NOT EXISTS unconfirmed_transaction (id BIGINT NOT NULL, "
                        + "deadline SMALLINT NOT NULL, "
                        + "recipient_id BIGINT, "
                        + "sender_id BIGINT NOT NULL, "
                        + "amount BIGINT NOT NULL, "
                        + "fee BIGINT NOT NULL, "
                        + "type TINYINT NOT NULL, "
                        + "subtype TINYINT NOT NULL, "
                        + "timestamp INT NOT NULL, "
                        + "attachment_bytes VARBINARY(43016), " // Constants.MAX_PRUNABLE_MESSAGE_LENGTH = 42 * 1024; + 8
                        + "transaction_version TINYINT NOT NULL, "
                        + "has_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "expiration INT NOT NULL, "
                        + "height INT NOT NULL, "
                        + "transaction_index SMALLINT NOT NULL, "
                        + "PRIMARY KEY (id))");
            case 15:
                apply("CREATE INDEX unconfirmed_transaction_id_idx ON unconfirmed_transaction (id)");
            case 16:
                apply("CREATE INDEX unconfirmed_transaction_sender_id_idx ON unconfirmed_transaction (sender_id)");
            case 17:
                apply("CREATE INDEX unconfirmed_transaction_recipient_id_idx ON unconfirmed_transaction (recipient_id)");
            case 18:
                apply("CREATE INDEX unconfirmed_transaction_timestamp_desc_idx ON unconfirmed_transaction (timestamp DESC)");
            case 19:
                apply("CREATE TABLE IF NOT EXISTS account_asset (db_id BIGINT NOT NULL AUTO_INCREMENT, "
                        + "account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, "
                        + "quantity BIGINT NOT NULL, "
                        + "unconfirmed_quantity BIGINT NOT NULL, "
                        + "height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE, "
                        + "PRIMARY KEY (db_id))");
            case 20:
                apply("CREATE UNIQUE INDEX account_asset_id_height_idx ON account_asset (account_id, asset_id, height DESC)");
            case 21:
                apply("CREATE INDEX account_asset_quantity_idx ON account_asset (quantity DESC)");
            case 22:
                apply("CREATE INDEX account_asset_asset_id_idx ON account_asset (asset_id)");
            case 23:
                apply("CREATE INDEX account_asset_height_id_idx ON account_asset (height, account_id, asset_id)");
            case 24:
                return;
            default:
                throw new RuntimeException("Replication database inconsistent with code, at update " + nextUpdate + ", probably trying to run older code on newer database");
        }
    }
}