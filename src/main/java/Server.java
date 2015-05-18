import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

class Server {
    Server(String host, int port, boolean ssl) throws Exception {
        List<Handler> handlerList = new ArrayList<>();
        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory webSocketServletFactory) {
                webSocketServletFactory.register(WSSerial.class);
            }
        };

        ContextHandler staticContextHandler = new ContextHandler();
        ResourceHandler staticResourceHandler = new ResourceHandler();
        staticResourceHandler.setResourceBase(getClass().getResource("web").toString());
        MimeTypes mimeTypes = new MimeTypes();
        mimeTypes.addMimeMapping("html", "text/html");
        mimeTypes.addMimeMapping("js", "application/javascript");
        staticResourceHandler.setMimeTypes(mimeTypes);
        staticContextHandler.setHandler(staticResourceHandler);
        handlerList.add(staticContextHandler);

        ContextHandler certHandler = new ContextHandler();
        class CertificateContextHandler extends AbstractHandler {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                if (target.toLowerCase().endsWith(".cer")) {
                    try {
                        KeyStore httpKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                        httpKeyStore.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());
                        baseRequest.setHandled(true);
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/x-x509-ca-cert");
                        String alias = target.replaceAll("/", "").substring(0, target.length() - 5);
                        response.getOutputStream().write(httpKeyStore.getCertificate(alias).getEncoded());
                    } catch (Exception ignored) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                }
            }
        }
        CertificateContextHandler certificateContextHandler = new CertificateContextHandler();
        certHandler.setHandler(certificateContextHandler);
        handlerList.add(certHandler);

        ContextHandler wsContextHandler = new ContextHandler();
        wsContextHandler.setHandler(wsHandler);
        wsContextHandler.setContextPath("/ws");
        handlerList.add(wsHandler);

        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();
        HttpConnectionFactory httpConnectionFactory;
        ServerConnector connector;
        if (ssl) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());
            sslContextFactory.setKeyStore(keyStore);
            sslContextFactory.setKeyManagerPassword("password");
            SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
            httpConnectionFactory = new HttpConnectionFactory(new HttpConfiguration());
            connector = new ServerConnector(server, sslConnectionFactory, httpConnectionFactory);
        } else {
            httpConnectionFactory = new HttpConnectionFactory(new HttpConfiguration());
            connector = new ServerConnector(server, httpConnectionFactory);
        }
        connector.setHost(host);
        connector.setPort(port);
        server.addConnector(connector);
        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(handlerList.toArray(new Handler[handlerList.size()]));
        server.setHandler(handlerCollection);
        server.setStopAtShutdown(true);
        server.start();
    }
}
