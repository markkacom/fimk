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

import nxt.Account;
import nxt.Asset;
import nxt.Attachment;
import nxt.NxtException;
import nxt.txn.DividendPaymentAttachment;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class DividendPayment extends CreateTransaction {

    static final DividendPayment instance = new DividendPayment();

    private DividendPayment() {
        super(new APITag[] {APITag.AE, APITag.CREATE_TRANSACTION}, "asset", "height", "amountNQTPerQNT");
    }

    @Override
    JSONStreamAware processRequest(final HttpServletRequest request)
            throws NxtException
    {
        final int height = ParameterParser.getHeight(request);
        final long amountNQTPerQNT = ParameterParser.getAmountNQTPerQNT(request);
        final Account account = ParameterParser.getSenderAccount(request);
        final Asset asset = ParameterParser.getAsset(request);
        final Attachment attachment = new DividendPaymentAttachment(asset.getId(), height, amountNQTPerQNT);
        return this.createTransaction(request, account, attachment);
    }

}
