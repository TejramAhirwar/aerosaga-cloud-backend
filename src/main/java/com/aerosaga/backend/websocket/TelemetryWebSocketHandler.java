
package com.aerosaga.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryWebSocketHandler
        extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions =
            ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) {

        sessions.add(session);
        System.out.println(
                "WebSocket connected: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            org.springframework.web.socket.CloseStatus status) {

        sessions.remove(session);
        System.out.println(
                "WebSocket disconnected: " + session.getId());
    }


    public void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }

            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                System.err.println(
                        "Telemetry send failed for session "
                                + session.getId() + ": " + e.getMessage()
                );

                sessions.remove(session);

                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (IOException closeException) {
                    System.err.println(
                            "Failed to close session: "
                                    + closeException.getMessage()
                    );
                }
            }
        }
    }

}

