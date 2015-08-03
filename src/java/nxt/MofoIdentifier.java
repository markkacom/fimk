package nxt;

public class MofoIdentifier {
  
    static final String DEFAULT_SERVER = "fimk.fi";
  
    private String normalizedId;
    private boolean isDefaultServer;

    @SuppressWarnings("serial")
    public static class InvalidAccountIdentifier extends RuntimeException {
        InvalidAccountIdentifier() {
            super("Invalid account identifier");
        }
    }
    
    public MofoIdentifier(String id) {
        normalizedId = id.toLowerCase();
        
        if (normalizedId.length() > Constants.MAX_ACCOUNT_ID_LENGTH) {
            throw new InvalidAccountIdentifier();
        }
        
        String[] parts = normalizedId.split("@");
        if (parts.length != 2) {
            throw new InvalidAccountIdentifier();
        }
        
        String name = parts[0];
        String server = parts[1];
        
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
        
        isDefaultServer = DEFAULT_SERVER.equals(server);       
    }
    
    public boolean getIsDefaultServer() {
        return isDefaultServer;
    }
    
    public String getNormalizedId() {
        return normalizedId;
    }
}