package nxt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import nxt.util.Convert;

public final class Constants {
  
    public static final boolean isTestnet = Nxt.getBooleanProperty("nxt.isTestnet");
  
    public static final long ONE_NXT = 100000000;
    
    public static final long MIN_FEE_NQT = ONE_NXT / 10;
    public static final int BLOCK_HEADER_LENGTH = 232;
    
    public static final int MAX_NUMBER_OF_TRANSACTIONS = 512;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * 176;
    public static final int SECONDS_BETWEEN_BLOCKS = 30;
    
    /* XXX - POS reward amounts and halving */
    public static final long FORGER_FEE_STAGE_CHANGE_AT_BLOCK = 889920;
    public static final long[] FORGER_FEE_AMOUNT_NQT_STAGES = { 200 * ONE_NXT, 100 * ONE_NXT, 50 * ONE_NXT, 25 * ONE_NXT };
    
    public static final long MAX_BALANCE_NXT = isTestnet ? 999965465 : 999455619;
    public static final long MAX_BALANCE_NQT = MAX_BALANCE_NXT * ONE_NXT;
    public static final long INITIAL_BASE_TARGET = isTestnet ? 307456352 : 307613193;
    
    public static final long MAX_BASE_TARGET = MAX_BALANCE_NXT * INITIAL_BASE_TARGET;
    public static final int MAX_ROLLBACK = Nxt.getIntProperty("nxt.maxRollback");
    static {
        if (MAX_ROLLBACK < 1440) {
            throw new RuntimeException("nxt.maxRollback must be at least 1440");
        }
    }

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;
    public static final int MAX_ENCRYPTED_MESSAGE_LENGTH = 1000;

    public static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    public static final int MAX_ACCOUNT_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_ACCOUNT_MESSAGE_PATTERN_LENGTH = 100;

    public static final long MAX_ASSET_QUANTITY_QNT = 1000000000L * 100000000L;
    public static final int MIN_ASSET_NAME_LENGTH = 3;
    public static final int MAX_ASSET_NAME_LENGTH = 10;
    public static final int MAX_ASSET_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_ASSET_TRANSFER_COMMENT_LENGTH = 1000;

    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;
    public static final int MAX_POLL_OPTION_COUNT = 100;

    public static final int MAX_DGS_LISTING_QUANTITY = 1000000000;
    public static final int MAX_DGS_LISTING_NAME_LENGTH = 100;
    public static final int MAX_DGS_LISTING_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_DGS_LISTING_TAGS_LENGTH = 100;
    public static final int MAX_DGS_GOODS_LENGTH = 10240;

    public static final int MAX_HUB_ANNOUNCEMENT_URIS = 100;
    public static final int MAX_HUB_ANNOUNCEMENT_URI_LENGTH = 1000;
    public static final long MIN_HUB_EFFECTIVE_BALANCE = 100000;
    
    public static final int SECOND_BIRTH_BLOCK = 1;
    public static final int THIRD_BIRTH_BLOCK = 203000;
    public static final int THIRD_BIRTH_BLOCK_TEST = 2;

    public static final int MIN_CURRENCY_NAME_LENGTH = 3;
    public static final int MAX_CURRENCY_NAME_LENGTH = 10;
    public static final int MIN_CURRENCY_CODE_LENGTH = 3;
    public static final int MAX_CURRENCY_CODE_LENGTH = 5;
    public static final int MAX_CURRENCY_DESCRIPTION_LENGTH = 1000;
    public static final long MAX_CURRENCY_TOTAL_SUPPLY = 1000000000L * 100000000L;
    public static final int MAX_MINTING_RATIO = 10000; // per mint units not more than 0.01% of total supply
    public static final byte MIN_NUMBER_OF_SHUFFLING_PARTICIPANTS = 3;
    public static final byte MAX_NUMBER_OF_SHUFFLING_PARTICIPANTS = 100;
    public static final short MIN_SHUFFLING_DELAY = 5;
    public static final short MAX_SHUFFLING_DELAY = 1440;
    public static final int MAX_SHUFFLING_RECIPIENTS_LENGTH = 10000;

    public static final boolean isOffline = Nxt.getBooleanProperty("nxt.isOffline");
    
    public static final int ALIAS_SYSTEM_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int ARBITRARY_MESSAGES_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_2 = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_3 = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_4 = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_5 = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_6 = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_7 = Integer.MAX_VALUE;
    public static final int TRANSPARENT_FORGING_BLOCK_8 = isTestnet ? THIRD_BIRTH_BLOCK_TEST : THIRD_BIRTH_BLOCK;

    public static final int NQT_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int FRACTIONAL_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int ASSET_EXCHANGE_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK = 0;
    public static final int NAMESPACED_ALIAS_BLOCK = isTestnet ? THIRD_BIRTH_BLOCK_TEST : THIRD_BIRTH_BLOCK;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP = 0;
    public static final int VOTING_SYSTEM_BLOCK = Integer.MAX_VALUE;
    public static final int DIGITAL_GOODS_STORE_BLOCK = isTestnet ? THIRD_BIRTH_BLOCK_TEST : THIRD_BIRTH_BLOCK;
    public static final int PUBLIC_KEY_ANNOUNCEMENT_BLOCK = isTestnet ? THIRD_BIRTH_BLOCK_TEST : THIRD_BIRTH_BLOCK;
    public static final int MONETARY_SYSTEM_BLOCK = isTestnet ? THIRD_BIRTH_BLOCK_TEST : Integer.MAX_VALUE;
    public static final int LAST_KNOWN_BLOCK = isTestnet ? 0 : 384000;
    public static final int PRIVATE_ASSETS_BLOCK = isTestnet ? THIRD_BIRTH_BLOCK_TEST : Integer.MAX_VALUE;
    
    /* range 0.000001% to 2000% / 1 - 2000000000 */
    public static final long MIN_PRIVATE_ASSET_FEE_PERCENTAGE = 0L; 
    public static final long MAX_PRIVATE_ASSET_FEE_PERCENTAGE = 2000000000L;
    
    public static final int[] MIN_VERSION = new int[] {0, 3, 3};                
    
    public static final long UNCONFIRMED_POOL_DEPOSIT_NQT = 100 * ONE_NXT;    
    
    public static final int FORGER_FEE_BLOCK = SECOND_BIRTH_BLOCK + 1;

    public static final long EPOCH_BEGINNING;
    static {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 24);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        EPOCH_BEGINNING = calendar.getTimeInMillis();
    }
    
    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final String ALLOWED_CURRENCY_CODE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String NAMESPACED_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz!#$%&()*+-./:;<=>?@[]_{|}";

    public static final int EC_RULE_TERMINATOR = 600; /* cfb: This constant defines a straight edge when "longest chain"
                                                        rule is outweighed by "economic majority" rule; the terminator
                                                        is set as number of seconds before the current time. */

    public static final int EC_BLOCK_DISTANCE_LIMIT = 60;
    
    /* XXX - List of Account ID's that are allowed to forge (or null to allow all) */
    public static final List<Long> allowedToForge; 
    static {
      List<String> allowed = Nxt.getStringListProperty("nxt.allowedToForge");
      if (allowed.size() == 0) {
        allowedToForge = Collections.emptyList();
      }
      else if (allowed.size() == 1 && "*".equals(allowed.get(0))) {
        allowedToForge = null;
      }
      else {
        allowedToForge = new ArrayList<Long>();
        for (String account : allowed) {
          allowedToForge.add(Convert.parseAccountId(account));
        }
      }
    }
    
    private Constants() {} // never

}
