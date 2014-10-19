package nxt.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nxt.Account;
import nxt.NamespacedAlias;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetNamespacedAliases extends APIServlet.APIRequestHandler {

    static final GetNamespacedAliases instance = new GetNamespacedAliases();

    private GetNamespacedAliases() {
        super(new APITag[] {APITag.ALIASES}, "account", "timestamp", "filter", "firstIndex", "lastIndex");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
      
        Account account = ParameterParser.getAccount(req);
        int timestamp = ParameterParser.getTimestamp(req);
        String filter = ParameterParser.getFilter(req);
        
        Collection<NamespacedAlias> filtered = new ArrayList<NamespacedAlias>();
        if (filter == null && timestamp == 0) {
            filtered = NamespacedAlias.getAliasesByOwner(account.getId());
        }
        else {
            for (NamespacedAlias alias : NamespacedAlias.getAllAliases()) {
                if (alias.getAccountId().equals(account.getId())) {
                    if (filter != null && !alias.getAliasName().startsWith(filter)) {
                        continue;
                    }
                    if (alias.getTimestamp() >= timestamp) {
                        filtered.add(alias);
                    }
                }
            }
        }

        List<NamespacedAlias> list = new ArrayList<NamespacedAlias>(filtered);        
        
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = Math.max(ParameterParser.getLastIndex(req), 0);
        if ((lastIndex - firstIndex) > 1000) {
            lastIndex = firstIndex + 1000; /* Guard against server spam */
        }

        JSONArray aliases = new JSONArray();
        for (NamespacedAlias alias : list.subList(firstIndex, lastIndex > list.size() ? list.size() : lastIndex)) {
            aliases.add(JSONData.namespacedAlias(alias));
        }
        
        JSONObject response = new JSONObject();
        response.put("aliases", aliases);
        return response;        
    }

}