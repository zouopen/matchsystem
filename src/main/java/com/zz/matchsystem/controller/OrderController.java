package com.zz.matchsystem.controller;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.lmax.disruptor.RingBuffer;
import com.zz.matchsystem.model.MessageModel;
import com.zz.matchsystem.trader.*;
import com.zz.matchsystem.utils.GeneratorUtil;
import com.zz.matchsystem.utils.MessageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Random;

/**
 * @Author：EvilSay
 * @Date：17.10.23 23:26
 * @description: API
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private RingBuffer<MessageModel> messageModel;

    @RequestMapping(value = "add",method = RequestMethod.POST)
    public MessageResult add (ExchangeOrderDirection direction, String symbol, BigDecimal price,
                              BigDecimal amount, ExchangeOrderType type){

        ExchangeOrder order = new ExchangeOrder();

        order.setMemberId(RandomUtil.randomLong());
        order.setSymbol(symbol);
        order.setBaseSymbol("USDT");
        order.setCoinSymbol("BTC");
        order.setType(type);
        order.setOrderId(GeneratorUtil.getOrderId("E"));
        order.setDirection(direction);
        order.setStatus(ExchangeOrderStatus.TRADING);

        if(order.getType() == ExchangeOrderType.MARKET_PRICE){
            order.setPrice(BigDecimal.ZERO);
        }
        else{
            order.setPrice(price);
        }

        order.setUseDiscount("0");
        //限价买入单时amount为用户设置的总成交额
        order.setAmount(amount);

        long sequence = messageModel.next();

        try {
            MessageModel model = messageModel.get(sequence);
            model.setExchangeOrder(order);
        }finally {
            //发布Event，激活观察者去消费，将sequence传递给改消费者
            //注意最后的publish方法必须放在finally中以确保必须得到调用；如果某个请求的sequence未被提交将会堵塞后续的发布操作或者其他的producer
            messageModel.publish(sequence);
        }

        return MessageResult.success();
    }
}
