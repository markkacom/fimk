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

import nxt.Exchange;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_OFFER;
import static nxt.http.JSONResponses.MISSING_OFFER;

public final class GetExchangesByOffer extends APIServlet.APIRequestHandler {

    static final GetExchangesByOffer instance = new GetExchangesByOffer();

    private GetExchangesByOffer() {
        super(new APITag[] {APITag.MS}, "offer", "includeCurrencyInfo", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        // can't use ParameterParser.getCurrencyBuyOffer because offer may have been already deleted
        String offerValue = Convert.emptyToNull(req.getParameter("offer"));
        if (offerValue == null) {
            throw new ParameterException(MISSING_OFFER);
        }
        long offerId;
        try {
            offerId = Convert.parseUnsignedLong(offerValue);
        } catch (RuntimeException e) {
            throw new ParameterException(INCORRECT_OFFER);
        }
        boolean includeCurrencyInfo = !"false".equalsIgnoreCase(req.getParameter("includeCurrencyInfo"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        JSONObject response = new JSONObject();
        JSONArray exchangesData = new JSONArray();
        try (DbIterator<Exchange> exchanges = Exchange.getOfferExchanges(offerId, firstIndex, lastIndex)) {
            while (exchanges.hasNext()) {
                exchangesData.add(JSONData.exchange(exchanges.next(), includeCurrencyInfo));
            }
        }
        response.put("exchanges", exchangesData);
        return response;
    }

}
