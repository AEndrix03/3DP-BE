package it.aredegalli.printer.config;

import it.aredegalli.printer.websocket.SlicingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket configuration for real-time slicing queue updates
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final SlicingWebSocketHandler slicingWebSocketHandler;

    @Value("${websocket.slicing.endpoint:/ws/slicing}")
    private String slicingEndpoint;

    @Value("${websocket.slicing.max-connections-per-user:5}")
    private int maxConnectionsPerUser;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("Registering WebSocket handlers - Slicing endpoint: {}", slicingEndpoint);

        registry.addHandler(slicingWebSocketHandler, slicingEndpoint)
                .setAllowedOrigins("*") // Configure appropriately for production
                .withSockJS(); // Enable SockJS fallback for older browsers

        log.info("WebSocket handlers registered successfully");
    }

    /**
     * Configure WebSocket container settings
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        // Set buffer sizes
        container.setMaxTextMessageBufferSize(32768); // 32KB
        container.setMaxBinaryMessageBufferSize(32768); // 32KB

        // Set session timeout
        container.setMaxSessionIdleTimeout(300000L); // 5 minutes

        // Set maximum connections per user
        container.setAsyncSendTimeout(5000L); // 5 seconds

        log.info("WebSocket container configured - Max connections per user: {}", maxConnectionsPerUser);

        return container;
    }
}