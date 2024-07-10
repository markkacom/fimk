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
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nxt.*;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

abstract class AdminAPIRequestHandler extends APIServlet.APIRequestHandler {

    AdminAPIRequestHandler(APITag[] apiTags, String... parameters) {
        super(apiTags, parameters);
    }

    @Parameter(name = "adminPassword", in = ParameterIn.QUERY, description = "admin password")
    abstract public JSONStreamAware processRequest(HttpServletRequest request) throws NxtException;

    /**
     * Require the administrator password
     *
     * @return                      TRUE if the admin password is required
     */
    @Override
    boolean requirePassword() {
        return true;
    }
}
