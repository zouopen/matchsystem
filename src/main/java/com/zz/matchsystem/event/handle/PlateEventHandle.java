package com.zz.matchsystem.event.handle;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.lmax.disruptor.EventHandler;
import com.zz.matchsystem.model.PlateModel;
import com.zz.matchsystem.trader.TradePlate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.annotation.Resource;

/**
 * @Author：EvilSay
 * @Date：17.10.23 23:46
 * @description: 消息消费
 */
@Slf4j
public class PlateEventHandle implements EventHandler<PlateModel> {
    @Override
    public void onEvent(PlateModel plateModel, long l, boolean b)  {
        try {
            TradePlate plate = plateModel.getPlate();
            String jsonString = JSON.toJSONString(plate);
            String body = HttpRequest.post("http://localhost:8080/wsTemplate/send").body(jsonString).execute().body();
            if ("ok".equals(body)) {
                log.info("[发送STOMP消息]内容={}",jsonString);
            }
        } catch (Exception e) {
            log.error("PlateEventHandle error={}", e.getMessage());
        }
    }
}
