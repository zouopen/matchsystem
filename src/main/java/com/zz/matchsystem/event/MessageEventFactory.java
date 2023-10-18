package com.zz.matchsystem.event;

import com.lmax.disruptor.EventFactory;
import com.zz.matchsystem.model.MessageModel;

/**
 * @Author：EvilSay
 * @Date：17.10.23 23:45
 * @description: 指定事件
 */
public class MessageEventFactory implements EventFactory<MessageModel> {
    @Override
    public MessageModel newInstance() {
        return new MessageModel();
    }
}
