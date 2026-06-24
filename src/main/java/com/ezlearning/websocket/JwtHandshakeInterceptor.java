package com.ezlearning.websocket;

import com.ezlearning.service.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        var query = request.getURI().getQuery();
        if (query == null) {
            return false;
        }
        var params = parseQueryParams(query);
        var token = params.get("token");
        if (token == null || !jwtService.isTokenValid(token)) {
            return false;
        }
        var userId = jwtService.extractUserId(token);
        attributes.put("userId", userId.toString());
        return true;
    }

    private Map<String, String> parseQueryParams(String query) {
        var params = new HashMap<String, String>();
        for (var pair : query.split("&")) {
            var parts = pair.split("=", 2);
            if (parts.length == 2) {
                params.put(parts[0], parts[1]);
            }
        }
        return params;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
