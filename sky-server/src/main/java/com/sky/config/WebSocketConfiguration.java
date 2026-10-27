package com.sky.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/*
 * WebSocket配置类，用于注册WebSocket的Bean
 */
@Configuration
public class WebSocketConfiguration {


    //注册ServerEndpointExporter(它是另一个容器（Tomcat）的注册代理)
    //它会自己去 Spring 容器里按 @ServerEndpoint 注解批量捞 WebSocketServer(之前已通过@Component注册进Spring容器)
    //找到好将其注册到Tomcat中(浏览器才能与其连接)
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {

        return new ServerEndpointExporter();
    }

}
