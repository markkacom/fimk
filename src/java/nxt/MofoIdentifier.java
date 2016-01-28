package nxt;

public class MofoIdentifier {

    static final String[] DEFAULT_SERVERS = new String[] { "fimk.fi", "lompsa.com" };

    private String normalizedId;
    private boolean isDefaultServer;
    private String name;
    private String server;

    @SuppressWarnings("serial")
    public static class InvalidAccountIdentifier extends RuntimeException {
        InvalidAccountIdentifier() {
            super("Invalid account identifier");
        }
    }

    public MofoIdentifier(String id) {
        normalizedId = id.toLowerCase();

        if (normalizedId.length() > Constants.MAX_ACCOUNT_IDENTIFIER_LENGTH) {
            throw new InvalidAccountIdentifier();
        }

        String[] parts = normalizedId.split("@");
        if (parts.length != 2) {
            throw new InvalidAccountIdentifier();
        }

        name = parts[0];
        server = parts[1];

        for (int i=0; i<name.length(); i++) {
            if (Constants.ALLOWED_ACCOUNT_ID_NAME.indexOf(name.charAt(i)) < 0) {
                throw new InvalidAccountIdentifier();
            }
        }
        for (int i=0; i<server.length(); i++) {
            if (Constants.ALLOWED_ACCOUNT_ID_SERVER.indexOf(server.charAt(i)) < 0) {
                throw new InvalidAccountIdentifier();
            }
        }

        for (int i=0; i<DEFAULT_SERVERS.length; i++) {
            if (DEFAULT_SERVERS[i].equals(server)) {
                isDefaultServer = true;
                return;
            }
        }
        isDefaultServer = false;
    }

    public boolean getIsDefaultServer() {
        return isDefaultServer;
    }

    public String getNormalizedId() {
        return normalizedId;
    }

    public String getSimilarId() {
        if (isDefaultServer) {
            int index = DEFAULT_SERVERS[0].equals(server) ? 1 : 0;
            return name + '@' + DEFAULT_SERVERS[index];
        }
        return "";
    }
}