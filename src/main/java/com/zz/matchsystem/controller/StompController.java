package com.zz.matchsystem.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author：EvilSay
 * @Date：19.10.23 20:38
 * @description:
 */
@RestController
@RequestMapping("/wsTemplate")
@Slf4j
public class StompController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/send")
    @SendTo("/topic/symbol")
    public String send(@RequestBody JSONObject json) {
        messagingTemplate.convertAndSend("/topic/symbol", json);
        return "ok";
    }

}
