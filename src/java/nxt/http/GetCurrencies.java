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

import nxt.Currency;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_CURRENCY;
import static nxt.http.JSONResponses.UNKNOWN_CURRENCY;

public final class GetCurrencies extends APIServlet.APIRequestHandler {

    static final GetCurrencies instance = new GetCurrencies();

    private GetCurrencies() {
        super(new APITag[] {APITag.MS}, "currencies", "currencies", "currencies", "includeCounts"); // limit to 3 for testing
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String[] currencies = req.getParameterValues("currencies");
        boolean includeCounts = !"false".equalsIgnoreCase(req.getParameter("includeCounts"));

        JSONObject response = new JSONObject();
        JSONArray currenciesJSONArray = new JSONArray();
        response.put("currencies", currenciesJSONArray);
        if (currencies == null) {
            return response;
        }
        for (String currencyIdString : currencies) {
            if (currencyIdString == null || currencyIdString.equals("")) {
                continue;
            }
            try {
                Currency currency = Currency.getCurrency(Convert.parseUnsignedLong(currencyIdString));
                if (currency == null) {
                    return UNKNOWN_CURRENCY;
                }
                currenciesJSONArray.add(JSONData.currency(currency, includeCounts));
            } catch (RuntimeException e) {
                return INCORRECT_CURRENCY;
            }
        }
        return response;
    }

}
