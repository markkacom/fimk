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

package nxt;

import nxt.db.BasicDb;
import nxt.db.TransactionalDb;

public final class Db {

    public static final String PREFIX = Constants.isTestnet ? "fimk.testDb" : "fimk.db";
    public static final TransactionalDb db = new TransactionalDb(new BasicDb.DbProperties()
            .maxCacheSize(Nxt.getIntProperty("fimk.dbCacheKB"))
            .dbUrl(Nxt.getStringProperty(PREFIX + "Url"))
            .dbType(Nxt.getStringProperty(PREFIX + "Type"))
            .dbDir(Nxt.getStringProperty(PREFIX + "Dir"))
            .dbParams(Nxt.getStringProperty(PREFIX + "Params"))
            .dbUsername(Nxt.getStringProperty(PREFIX + "Username"))
            .dbPassword(Nxt.getStringProperty(PREFIX + "Password"))
            .maxConnections(Nxt.getIntProperty("fimk.maxDbConnections"))
            .loginTimeout(Nxt.getIntProperty("fimk.dbLoginTimeout"))
            .defaultLockTimeout(Nxt.getIntProperty("fimk.dbDefaultLockTimeout") * 1000)
    );

    static void init() {
        db.init(new NxtDbVersion());
    }

    static void shutdown() {
        db.shutdown();
    }

    private Db() {} // never

}
