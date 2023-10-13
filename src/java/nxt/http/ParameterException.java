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

import nxt.NxtException;

import org.json.simple.JSONStreamAware;

@SuppressWarnings("serial")
public final class ParameterException extends NxtException {

    private final JSONStreamAware errorResponse;

    public ParameterException(JSONStreamAware errorResponse) {
        this.errorResponse = errorResponse;
    }

    public JSONStreamAware getErrorResponse() {
        return errorResponse;
    }

}
