package com.zz.matchsystem.event.handle;

import cn.hutool.extra.spring.SpringUtil;
import com.lmax.disruptor.EventHandler;
import com.zz.matchsystem.model.MessageModel;
import com.zz.matchsystem.trader.CoinTrader;
import com.zz.matchsystem.trader.CoinTraderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author：EvilSay
 * @Date：17.10.23 23:46
 * @description: 消息消费
 */
@Slf4j
public class MessageEventHandle implements EventHandler<MessageModel> {
    @Override
    public void onEvent(MessageModel messageModel, long l, boolean b) throws Exception {
        CoinTraderFactory coinTraderFactory = SpringUtil.getBean("coinTraderFactory");
        CoinTrader trader = coinTraderFactory.getTrader("USDT-BTC");
        trader.trade(messageModel.getExchangeOrder());
    }
}
