package nxt.http.websocket;

import java.util.concurrent.TimeUnit;

import nxt.Constants;
import nxt.Nxt;
import nxt.util.Logger;
import nxt.util.ThreadPool;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public final class WebsocketServer {
  
    public static final int TESTNET_API_PORT = 6986;

    private static Server server;
    private final static boolean enableWebsockets = Nxt.getBooleanProperty("nxt.enableWebsockets");
    private final static int port = Constants.isTestnet ? TESTNET_API_PORT : Nxt.getIntProperty("nxt.websocketServerPort");
    private final static String host = Nxt.getStringProperty("nxt.websocketServerHost");
    private static final boolean enableSSL = Nxt.getBooleanProperty("nxt.websocketSSL");
    
    public static void init() {}
    
    static {
        if (enableWebsockets) {
            ThreadPool.runBeforeStart(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (enableSSL) {
                            startSSL();
                        }
                        else {
                            start();
                        }
                        Logger.logMessage("Started WebSocket server at " + host + ":" + port);
                    } catch (Exception e) {
                        Logger.logErrorMessage("Failed to start API server", e);
                        throw new RuntimeException(e.toString(), e);
                    }
        
                }
            }, true);
        }
    }
    
    
    public static void start() throws Exception {
        server = new Server();
  
        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
        http.setHost(host);
        http.setPort(port);
        server.setConnectors(new Connector[]{http});
  
        configureContextHandler();
        server.start();
    }
    
    public static void startSSL() throws Exception {
        server = new Server();
  
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(port);
        http_config.setOutputBufferSize(32768);
  
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(Nxt.getStringProperty("nxt.keyStorePath"));
        sslContextFactory.setKeyStorePassword(Nxt.getStringProperty("nxt.keyStorePassword"));
        sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
        sslContextFactory.setExcludeProtocols("SSLv3");        

        HttpConfiguration https_config = new HttpConfiguration(http_config);
        https_config.addCustomizer(new SecureRequestCustomizer());
  
        ServerConnector https = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        https.setPort(port);
        https.setIdleTimeout(500000);
  
        server.setConnectors(new Connector[] { https });
        
        configureContextHandler();
        
        server.setStopAtShutdown(true);
        server.start();    
    }
    
    private static void configureContextHandler() {      
        WebSocketHandler wsHandler = new WebSocketHandler()
        {
            @Override
            public void configure(WebSocketServletFactory factory)
            {
                factory.getPolicy().setIdleTimeout(TimeUnit.MINUTES.toMillis(5));
                factory.register(MofoWebSocketAdapter.class);
            }
        };
        ContextHandler context = new ContextHandler();
        context.setContextPath("/ws/*");
        context.setHandler(wsHandler);
        server.setHandler(context);
    }
}
