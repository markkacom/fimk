package nxt;

import nxt.Constants;

public class HardFork {

    static boolean isEnabled(String blockId) {
        if (Constants.isTestnet) {
            return true;
        }
        Alias alias = Alias.getAlias(blockId);
        if (alias != null && "enabled".equals(alias.getAliasURI())) {
            return true;
        }
        return false;
    }

    public static boolean PRIVATE_ASSETS_BLOCK() {
        return isEnabled("PRIVATEASSETSBLOCK");
    }

    public static boolean ACCOUNT_IDENTIFIER_BLOCK() {
        return isEnabled("ACCOUNTIDENTIFIERBLOCK");
    }

    public static boolean COLORED_ACCOUNTS_BLOCK() {
        return isEnabled("COLOREDACCOUNTSBLOCK");
    }
}
