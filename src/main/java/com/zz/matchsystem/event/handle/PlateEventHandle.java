package com.zz.matchsystem.event.handle;

import com.alibaba.fastjson.JSON;
import com.lmax.disruptor.EventHandler;
import com.zz.matchsystem.model.PlateModel;
import com.zz.matchsystem.trader.TradePlate;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author：EvilSay
 * @Date：17.10.23 23:46
 * @description: 消息消费
 */
@Slf4j
public class PlateEventHandle implements EventHandler<PlateModel> {
    @Override
    public void onEvent(PlateModel plateModel, long l, boolean b) throws Exception {
        TradePlate plate = plateModel.getPlate();
        log.info("当前盘口信息={}",plate);
    }
}
