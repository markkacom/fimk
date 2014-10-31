package nxt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class NamespacedAlias {

    static class NamespacedKey {
        private final Long namespace;
        private final String key;
    
        static NamespacedKey valueOf(Long namespace, String key) {
            return new NamespacedKey(namespace, key);
        }
        
        private NamespacedKey(Long namespace, String key) {
            if (namespace == null || key == null) { 
                throw new NullPointerException("String or Long cannot be null");
            }
            this.namespace = namespace;
            this.key = key;
        }
        
        @Override
        public boolean equals(Object o) {
            if(o != null && o instanceof NamespacedKey){
                NamespacedKey other = (NamespacedKey) o;
                return this.namespace.equals(other.namespace) && this.key.equals(other.key);
            }    
            return false;
        }
        
        @Override
        public int hashCode() {
            return key.hashCode() * 37 + namespace.intValue();
        }
    }
  
    private static final ConcurrentMap<NamespacedKey, NamespacedAlias> aliases = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Long, NamespacedAlias> aliasIdToAliasMappings = new ConcurrentHashMap<>();
    private static final Collection<NamespacedAlias> allAliases = Collections.unmodifiableCollection(aliases.values());

    public static Collection<NamespacedAlias> getAllAliases() {
        return allAliases;
    }

    public static Collection<NamespacedAlias> getAliasesByOwner(Long accountId) {
        List<NamespacedAlias> filtered = new ArrayList<>();
        for (NamespacedAlias alias : NamespacedAlias.getAllAliases()) {
            if (alias.getAccountId().equals(accountId)) {
                filtered.add(alias);
            }
        }
        return filtered;
    }

    public static NamespacedAlias getAlias(Account sender, String aliasName) {
        return aliases.get(NamespacedKey.valueOf(sender.getId(), aliasName.toLowerCase()));
    }

    public static NamespacedAlias getAlias(Long id) {
        return aliasIdToAliasMappings.get(id);
    }

    static void addOrUpdateAlias(Account account, Long transactionId, String aliasName, String aliasURI, int timestamp) {
        String normalizedAlias = aliasName.toLowerCase();
        NamespacedAlias oldAlias = aliases.get(NamespacedKey.valueOf(account.getId(), normalizedAlias));
        if (oldAlias == null) {
            NamespacedAlias newAlias = new NamespacedAlias(account, transactionId, aliasName, aliasURI, timestamp);
            aliases.put(NamespacedKey.valueOf(account.getId(), normalizedAlias), newAlias);
            aliasIdToAliasMappings.put(transactionId, newAlias);
        } else {
            oldAlias.aliasURI = aliasURI.intern();
            oldAlias.timestamp = timestamp;
        }
    }

    static void remove(NamespacedAlias alias) {
        aliases.remove(NamespacedKey.valueOf(alias.getAccountId(), alias.getAliasName().toLowerCase()));
        aliasIdToAliasMappings.remove(alias.getId());
    }

    static void clear() {
        aliases.clear();
        aliasIdToAliasMappings.clear();
    }

    private final Long accountId;
    private final Long id;
    private final String aliasName;
    private volatile String aliasURI;
    private volatile int timestamp;

    private NamespacedAlias(Account account, Long id, String aliasName, String aliasURI, int timestamp) {
        this.accountId = account.getId();
        this.id = id;
        this.aliasName = aliasName.intern();
        this.aliasURI = aliasURI.intern();
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getAliasURI() {
        return aliasURI;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public Long getAccountId() {
        return accountId;
    }

    public static boolean isEnabled() {
        if(Nxt.getBlockchain().getLastBlock().getHeight() >= Constants.NAMESPACED_ALIAS_BLOCK) {
            return true;
        }
        return false;
    }
}
