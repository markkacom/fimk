package nxt.http;

import nxt.NamespacedAlias;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetNamespacedAlias extends APIServlet.APIRequestHandler {

    static final GetNamespacedAlias instance = new GetNamespacedAlias();

    private GetNamespacedAlias() {
        super(new APITag[] {APITag.ALIASES}, "account", "alias", "aliasName");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        NamespacedAlias alias = ParameterParser.getNamespacedAlias(req);
        return JSONData.namespacedAlias(alias);
    }

}
