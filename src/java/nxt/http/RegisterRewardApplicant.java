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

package nxt.http;

import io.swagger.v3.oas.annotations.Parameter;
import nxt.Account;
import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;


//@Path("/fimk?requestType=accountColorList")
public final class RegisterRewardApplicant extends CreateTransaction {

    static final RegisterRewardApplicant instance = new RegisterRewardApplicant();

    private RegisterRewardApplicant() {
        super(new APITag[] {}, "recipient");
    }

    @Override
    @POST
    public JSONStreamAware processRequest(@Parameter(hidden = true) HttpServletRequest req) throws NxtException {
        long recipient = ParameterParser.getAccountId(req, "recipient", true);
        Account account = ParameterParser.getSenderAccount(req);
        return createTransaction(req, account, recipient, 0,
                nxt.txn.AccountControlTxnType.REWARD_APPLICANT_REGISTRATION_ATTACHMENT);
    }

}
