package com.zz.matchsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@SpringBootApplication
@EnableWebSocket
@EnableAsync
public class MatchSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchSystemApplication.class, args);
    }

}
