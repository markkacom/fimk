/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt.http;

import static nxt.http.JSONResponses.INCORRECT_JSON_ARGS;

import javax.servlet.http.HttpServletRequest;

import nxt.NxtException;
import nxt.http.APIServlet.APIRequestHandler;
import nxt.util.Convert;

import org.json.simple.JSONStreamAware;

public final class MofoCallAPIFunction extends APIServlet.APIRequestHandler {

    public static final MofoCallAPIFunction instance = new MofoCallAPIFunction();
    
    private MofoCallAPIFunction() {
        super(new APITag[] {APITag.MOFO});
    }
  
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
      
        String requestType = Convert.emptyToNull(req.getParameter("requestType"));
        if (requestType == null) {
            throw new ParameterException(INCORRECT_JSON_ARGS);
        }
        
        APIRequestHandler apiRequestHandler = APIServlet.apiRequestHandlers.get(requestType);
        if (apiRequestHandler == null) {
            throw new ParameterException(INCORRECT_JSON_ARGS);
        }
        
        if (apiRequestHandler.requirePassword()) {
            API.verifyPassword(req);
        }

        return apiRequestHandler.processRequest(req);
    }
}
