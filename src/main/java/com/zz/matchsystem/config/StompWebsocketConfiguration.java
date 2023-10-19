package com.zz.matchsystem.config;

import com.zz.matchsystem.chat.ChatInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * @Author：EvilSay
 * @Date：19.10.23 20:13
 * @description:
 */
@EnableWebSocketMessageBroker
@Configuration
public class StompWebsocketConfiguration implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp").setAllowedOriginPatterns("*").addInterceptors();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/symbol");
        config.enableSimpleBroker("/topic","/symbol");
        config.setPreservePublishOrder(true);
        // 用户主题的前缀：默认是/user
        config.setUserDestinationPrefix("/top");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        //链接操作
        registration.interceptors(new ChatInterceptor());
    }
}
