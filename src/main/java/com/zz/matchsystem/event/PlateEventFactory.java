package com.zz.matchsystem.event;

import com.lmax.disruptor.EventFactory;
import com.zz.matchsystem.model.MessageModel;
import com.zz.matchsystem.model.PlateModel;
import com.zz.matchsystem.trader.TradePlate;

/**
 * @Author：EvilSay
 * @Date：17.10.23 23:45
 * @description: 指定事件
 */
public class PlateEventFactory implements EventFactory<PlateModel> {
    @Override
    public PlateModel newInstance() {
        return new PlateModel();
    }
}
