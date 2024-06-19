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

import io.swagger.v3.jaxrs2.integration.OpenApiServlet;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;
import nxt.Constants;
import nxt.Nxt;
import nxt.util.Logger;
import nxt.util.ThreadPool;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import java.util.function.Function;

import static nxt.http.JSONResponses.INCORRECT_ADMIN_PASSWORD;
import static nxt.http.JSONResponses.NO_PASSWORD_IN_CONFIG;

@OpenAPIDefinition(
        tags = {
                @Tag(name = APITag2.DEBUG, description = "Debug operations"),
                @Tag(name = APITag2.DGS, description = "Market operations"),
                @Tag(name = APITag2.NETWORK, description = "Network operations"),
                @Tag(name = APITag2.ACCOUNT, description = "Account operations"),
                @Tag(name = APITag2.DATA, description = "Data operations"),
                @Tag(name = APITag2.ASSET, description = "Asset operations"),
                @Tag(name = APITag2.AE, description = "Asset Exchange operations"),
                @Tag(name = APITag2.CREATE_TRANSACTION, description = "Transaction creation"),
                @Tag(name = APITag2.TRANSACTIONS, description = "Transaction operations"),
                @Tag(name = APITag2.MESSAGES, description = "Messages operations"),
                @Tag(name = APITag2.BLOCKCHAIN, description = "Blockchain operations"),
                @Tag(name = APITag2.ALIASES, description = "Aliases operations"),
                @Tag(name = APITag2.TOKEN, description = "Token operations"),
                @Tag(name = APITag2.PHASING, description = "Phasing"),
                @Tag(name = APITag2.MS, description = "Monetary System"),
                @Tag(name = APITag2.VS, description = "Voting System")
        },
        info = @Info(
                title = "FIMK API",
                version = "1.0.1"
//                contact = @Contact(
//                        name = "Example API Support",
//                        url = "http://exampleurl.com/contact",
//                        email = "techsupport@example.com"),
//                license = @License(
//                        name = "Apache 2.0",
//                        url = "https://www.apache.org/licenses/LICENSE-2.0.html")
        )
)
public final class API {

    public static final int TESTNET_API_PORT = 6886;
    public static final int TESTNET_API_SSLPORT = 6887;

    public static final Set<String> allowedBotHosts;
    private static final List<NetworkAddress> allowedBotNets;
    static final String adminPassword = Nxt.getStringProperty("fimk.adminPassword", "", true);
    static final boolean disableAdminPassword;
    static final int maxRecords = Nxt.getIntProperty("fimk.maxAPIRecords");

    private static final Server apiServer;
    private static URI browserUri;

    static {
        List<String> allowedBotHostsList = Nxt.getStringListProperty("fimk.allowedBotHosts");
        if (! allowedBotHostsList.contains("*")) {
            Set<String> hosts = new HashSet<>();
            List<NetworkAddress> nets = new ArrayList<>();
            for (String host : allowedBotHostsList) {
                if (host.contains("/")) {
                    try {
                        nets.add(new NetworkAddress(host));
                    } catch (UnknownHostException e) {
                        Logger.logErrorMessage("Unknown network " + host, e);
                        throw new RuntimeException(e.toString(), e);
                    }
                } else {
                    hosts.add(host);
                }
            }
            allowedBotHosts = Collections.unmodifiableSet(hosts);
            allowedBotNets = Collections.unmodifiableList(nets);
        } else {
            allowedBotHosts = null;
            allowedBotNets = null;
        }

        boolean enableAPIServer = Nxt.getBooleanProperty("fimk.enableAPIServer");
        if (enableAPIServer) {
            final int port = Nxt.getIntPropertyNew("apiServerPort", 0, API.TESTNET_API_PORT);
            final int sslPort = Nxt.getIntPropertyNew("apiServerSSLPort", 0, API.TESTNET_API_SSLPORT);
            final String host = Nxt.getStringProperty("fimk.apiServerHost");
            disableAdminPassword = Nxt.getBooleanProperty("fimk.disableAdminPassword") || ("127.0.0.1".equals(host) && adminPassword.isEmpty());

            apiServer = new Server();
            ServerConnector connector;
            boolean enableSSL = Nxt.getBooleanProperty("fimk.apiSSL");
            //
            // Create the HTTP connector
            //
            if (!enableSSL || port != sslPort) {
                connector = new ServerConnector(apiServer);
                connector.setPort(port);
                connector.setHost(host);
                connector.setIdleTimeout(Nxt.getIntProperty("fimk.apiServerIdleTimeout"));
                connector.setReuseAddress(true);
                apiServer.addConnector(connector);
                Logger.logMessage("API server using HTTP port " + port);
            }
            //
            // Create the HTTPS connector
            //
            if (enableSSL) {
                HttpConfiguration https_config = new HttpConfiguration();
                https_config.setSecureScheme("https");
                https_config.setSecurePort(sslPort);
                https_config.addCustomizer(new SecureRequestCustomizer());
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStorePath(Nxt.getStringProperty("fimk.keyStorePath"));
                sslContextFactory.setKeyStorePassword(Nxt.getStringProperty("fimk.keyStorePassword", null, true));
                sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                        "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                        "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");
                sslContextFactory.setExcludeProtocols("SSLv3");
                connector = new ServerConnector(apiServer, new SslConnectionFactory(sslContextFactory, "http/1.1"),
                        new HttpConnectionFactory(https_config));
                connector.setPort(sslPort);
                connector.setHost(host);
                connector.setIdleTimeout(Nxt.getIntProperty("fimk.apiServerIdleTimeout"));
                connector.setReuseAddress(true);
                apiServer.addConnector(connector);
                Logger.logMessage("API server using HTTPS port " + sslPort);
            }
            try {
                browserUri = new URI(enableSSL ? "https" : "http", null, "localhost", enableSSL ? sslPort : port, "/index.html", null, null);
            } catch (URISyntaxException e) {
                Logger.logInfoMessage("Cannot resolve browser URI", e);
            }

            HandlerList apiHandlers = new HandlerList();

            ServletContextHandler apiServletContextHandler = new ServletContextHandler();
            String apiResourceBase = Nxt.getStringProperty("fimk.apiResourceBase");
            if (apiResourceBase != null) {
                ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
                defaultServletHolder.setInitParameter("dirAllowed", "false");
                defaultServletHolder.setInitParameter("resourceBase", apiResourceBase);
                defaultServletHolder.setInitParameter("welcomeServlets", "true");
                defaultServletHolder.setInitParameter("redirectWelcome", "true");
                defaultServletHolder.setInitParameter("gzip", "true");
                defaultServletHolder.setInitParameter("etags", "true");
                apiServletContextHandler.addServlet(defaultServletHolder, "/*");
                apiServletContextHandler.setWelcomeFiles(new String[]{Nxt.getStringProperty("fimk.apiWelcomeFile")});
            }

//            SwaggerDoc.registerSwaggerJsonResource(null);

            // Setup Swagger servlet
//            ServletHolder swaggerServlet = apiHandler.addServlet(DefaultJaxrsConfig.class, "/swagger-core");
//            swaggerServlet.setInitOrder(2);
//            swaggerServlet.setInitParameter("api.version", "1.0.0");

//            ServletHolder holder = new ServletHolder(new APIServlet());
//            ServletContextHandler sch = new ServletContextHandler();
//            sch.setContextPath("/api/v1");
//            sch.addServlet(holder, "/*");

            ServletHolder openApiServlet = apiServletContextHandler.addServlet(OpenApiServlet.class, "/api/*");
            openApiServlet.setInitParameter("openApi.configuration.resourcePackages", "nxt.http");


            String javadocResourceBase = Nxt.getStringProperty("fimk.javadocResourceBase");
            if (javadocResourceBase != null) {
                ContextHandler contextHandler = new ContextHandler("/doc");
                ResourceHandler docFileHandler = new ResourceHandler();
                docFileHandler.setDirectoriesListed(false);
                docFileHandler.setWelcomeFiles(new String[]{"index.html"});
                docFileHandler.setResourceBase(javadocResourceBase);
                contextHandler.setHandler(docFileHandler);
                apiHandlers.addHandler(contextHandler);
            }

            Function<String, Void> registerServlet = pathSpec -> {
                ServletHolder servletHolder = apiServletContextHandler.addServlet(APIServlet.class, pathSpec);
                servletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(null, Constants.MAX_TAGGED_DATA_DATA_LENGTH, -1L, 0));
                if (Nxt.getBooleanProperty("fimk.enableAPIServerGZIPFilter")) {
                    FilterHolder gzipFilterHolder = apiServletContextHandler.addFilter(GzipFilter.class, pathSpec, null);
                    gzipFilterHolder.setInitParameter("methods", "GET,POST");
                    gzipFilterHolder.setAsyncSupported(true);
                }
                return null;
            };
            registerServlet.apply("/nxt");  //backward compatibility
            registerServlet.apply("/fimk");

            apiServletContextHandler.addServlet(APITestServlet.class, "/test");

            apiServletContextHandler.addServlet(DbShellServlet.class, "/dbshell");

            //apiServletContextHandler.addServlet(SwaggerConfigServlet.class, "/docs");

            try {
                apiHandlers.addHandler(buildSwaggerUI("/docs"));
            } catch (Exception e) {
                Logger.logErrorMessage("Failed to start interactive API doc server", e);
            }

            if (Nxt.getBooleanProperty("fimk.apiServerCORS")) {
                FilterHolder filterHolder = apiServletContextHandler.addFilter(CrossOriginFilter.class, "/*", null);
                filterHolder.setInitParameter("allowedHeaders", "*");
                filterHolder.setAsyncSupported(true);
            }

            apiHandlers.addHandler(apiServletContextHandler);

            //apiHandlers.addHandler(new DefaultHandler());


            apiServer.setHandler(apiHandlers);
            apiServer.setStopAtShutdown(true);

            ThreadPool.runBeforeStart(() -> {
                try {
                    apiServer.start();
                    Logger.logMessage("Started API server at " + host + ":" + port + (enableSSL && port != sslPort ? ", " + host + ":" + sslPort : ""));
                } catch (Exception e) {
                    Logger.logErrorMessage("Failed to start API server", e);
                    throw new RuntimeException(e.toString(), e);
                }

            }, true);

        } else {
            apiServer = null;
            disableAdminPassword = false;
            Logger.logMessage("API server not enabled");
        }

    }

    public static void init() {}

    public static void shutdown() {
        if (apiServer != null) {
            try {
                apiServer.stop();
            } catch (Exception e) {
                Logger.logShutdownMessage("Failed to stop API server", e);
            }
        }
    }

    static void verifyPassword(HttpServletRequest req) throws ParameterException {
        if (API.disableAdminPassword) {
            return;
        }
        if (API.adminPassword.isEmpty()) {
            throw new ParameterException(NO_PASSWORD_IN_CONFIG);
        } else if (!API.adminPassword.equals(req.getParameter("adminPassword"))) {
            Logger.logWarningMessage("Incorrect adminPassword");
            throw new ParameterException(INCORRECT_ADMIN_PASSWORD);
        }
    }

    static boolean checkPassword(HttpServletRequest req) {
        return (API.disableAdminPassword || (!API.adminPassword.isEmpty() && API.adminPassword.equals(req.getParameter("adminPassword"))));
    }

    static boolean isAllowed(String remoteHost) {
        if (API.allowedBotHosts == null || API.allowedBotHosts.contains(remoteHost)) {
            return true;
        }
        try {
            BigInteger hostAddressToCheck = new BigInteger(InetAddress.getByName(remoteHost).getAddress());
            for (NetworkAddress network : API.allowedBotNets) {
                if (network.contains(hostAddressToCheck)) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            // can't resolve, disallow
            Logger.logMessage("Unknown remote host " + remoteHost);
        }
        return false;

    }

    public static ContextHandler buildSwaggerUI(String path) throws Exception {
        ResourceHandler rh = new ResourceHandler();
        rh.setResourceBase(API.class.getClassLoader()
                .getResource("META-INF/resources/webjars/swagger-ui/4.18.1")
                .toURI().toString());
        ContextHandler context = new ContextHandler();
        context.setContextPath(path);
        context.setHandler(rh);
        return context;
    }

    private static class NetworkAddress {

        private BigInteger netAddress;
        private BigInteger netMask;

        private NetworkAddress(String address) throws UnknownHostException {
            String[] addressParts = address.split("/");
            if (addressParts.length == 2) {
                InetAddress targetHostAddress = InetAddress.getByName(addressParts[0]);
                byte[] srcBytes = targetHostAddress.getAddress();
                netAddress = new BigInteger(1, srcBytes);
                int maskBitLength = Integer.valueOf(addressParts[1]);
                int addressBitLength = (targetHostAddress instanceof Inet4Address) ? 32 : 128;
                netMask = BigInteger.ZERO
                        .setBit(addressBitLength)
                        .subtract(BigInteger.ONE)
                        .subtract(BigInteger.ZERO.setBit(addressBitLength - maskBitLength).subtract(BigInteger.ONE));
            } else {
                throw new IllegalArgumentException("Invalid address: " + address);
            }
        }

        private boolean contains(BigInteger hostAddressToCheck) {
            return hostAddressToCheck.and(netMask).equals(netAddress);
        }

    }

    public static URI getBrowserUri() {
        return browserUri;
    }

    public static boolean enabled() {
        return apiServer != null;
    }

    private API() {} // never

}
