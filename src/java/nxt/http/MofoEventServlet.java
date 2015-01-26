package nxt.http;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@SuppressWarnings("serial")
public class MofoEventServlet extends WebSocketServlet {
  
    final static long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis(1); //  1 minutes
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().println("HTTP GET method not implemented.");
    }
  
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.register(MofoEventSocket.class);
        factory.getPolicy().setIdleTimeout(DEFAULT_TIMEOUT);
    }
}