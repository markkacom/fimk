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

import nxt.Account;
import nxt.NxtException;
import nxt.Account.AccountInfo;
import nxt.db.DbIterator;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_IDENTIFIER;
import static nxt.http.JSONResponses.UNKNOWN_IDENTIFIER;

public final class MofoGetAccountByIdentifier extends APIServlet.APIRequestHandler {

    static final MofoGetAccountByIdentifier instance = new MofoGetAccountByIdentifier();

    private MofoGetAccountByIdentifier() {
        super(new APITag[] {APITag.ACCOUNTS}, "identifier", "includeLessors", "includeAssets", "includeCurrencies");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String identifier = Convert.emptyToNull(req.getParameter("identifier"));
        if (identifier == null) {
            throw new ParameterException(MISSING_IDENTIFIER);
        }
        Account account = Account.getAccountByIdentifier(identifier);
        if (account == null) {
            throw new ParameterException(UNKNOWN_IDENTIFIER);
        }
        boolean includeLessors = !"false".equalsIgnoreCase(req.getParameter("includeLessors"));
        boolean includeAssets = !"false".equalsIgnoreCase(req.getParameter("includeAssets"));
        boolean includeCurrencies = !"false".equalsIgnoreCase(req.getParameter("includeCurrencies"));

        JSONObject response = JSONData.accountBalance(account, false);
        JSONData.putAccount(response, "account", account.getId());

        if (account.getPublicKey() != null) {
            response.put("publicKey", Convert.toHexString(account.getPublicKey()));
        }
        AccountInfo info = account.getAccountInfo();
        if (info != null) {
            if (info.getName() != null) {
                response.put("name", info.getName());
            }
            if (info.getDescription() != null) {
                response.put("description", info.getDescription());
            }
        }

        Account.AccountLease accountLease = account.getAccountLease();
        if (accountLease != null && accountLease.getCurrentLesseeId() != 0) {
            JSONData.putAccount(response, "currentLessee", accountLease.getCurrentLesseeId());
            response.put("currentLeasingHeightFrom", accountLease.getCurrentLeasingHeightFrom());
            response.put("currentLeasingHeightTo", accountLease.getCurrentLeasingHeightTo());
        }
        if (accountLease != null && accountLease.getNextLesseeId() != 0) {
            JSONData.putAccount(response, "nextLessee", accountLease.getNextLesseeId());
            response.put("nextLeasingHeightFrom", accountLease.getNextLeasingHeightFrom());
            response.put("nextLeasingHeightTo", accountLease.getNextLeasingHeightTo());
        }

        if (includeLessors) {
            try (DbIterator<Account> lessors = account.getLessors()) {
                if (lessors.hasNext()) {
                    JSONArray lessorIds = new JSONArray();
                    JSONArray lessorIdsRS = new JSONArray();
                    JSONArray lessorInfo = new JSONArray();
                    while (lessors.hasNext()) {
                        Account lessor = lessors.next();
                        lessorIds.add(Long.toUnsignedString(lessor.getId()));
                        lessorIdsRS.add(Convert.rsAccount(lessor.getId()));
                        lessorInfo.add(JSONData.lessor(lessor, false));
                    }
                    response.put("lessors", lessorIds);
                    response.put("lessorsRS", lessorIdsRS);
                    response.put("lessorsInfo", lessorInfo);
                }
            }
        }

        if (includeAssets) {
            try (DbIterator<Account.AccountAsset> accountAssets = account.getAssets(0, -1)) {
                JSONArray assetBalances = new JSONArray();
                JSONArray unconfirmedAssetBalances = new JSONArray();
                while (accountAssets.hasNext()) {
                    Account.AccountAsset accountAsset = accountAssets.next();
                    JSONObject assetBalance = new JSONObject();
                    assetBalance.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
                    assetBalance.put("balanceQNT", String.valueOf(accountAsset.getQuantityQNT()));
                    assetBalances.add(assetBalance);
                    JSONObject unconfirmedAssetBalance = new JSONObject();
                    unconfirmedAssetBalance.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
                    unconfirmedAssetBalance.put("unconfirmedBalanceQNT", String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
                    unconfirmedAssetBalances.add(unconfirmedAssetBalance);
                }
                if (assetBalances.size() > 0) {
                    response.put("assetBalances", assetBalances);
                }
                if (unconfirmedAssetBalances.size() > 0) {
                    response.put("unconfirmedAssetBalances", unconfirmedAssetBalances);
                }
            }
        }

        if (includeCurrencies) {
            try (DbIterator<Account.AccountCurrency> accountCurrencies = account.getCurrencies(0, -1)) {
                JSONArray currencyJSON = new JSONArray();
                while (accountCurrencies.hasNext()) {
                    currencyJSON.add(JSONData.accountCurrency(accountCurrencies.next(), false, true));
                }
                if (currencyJSON.size() > 0) {
                    response.put("accountCurrencies", currencyJSON);
                }
            }
        }

        return response;

    }

}
