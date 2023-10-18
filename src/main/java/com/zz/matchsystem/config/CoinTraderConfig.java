package com.zz.matchsystem.config;

import com.lmax.disruptor.RingBuffer;
import com.zz.matchsystem.model.MessageModel;
import com.zz.matchsystem.model.PlateModel;
import com.zz.matchsystem.trader.CoinTrader;
import com.zz.matchsystem.trader.CoinTraderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @Author：EvilSay
 * @Date：18.10.23 00:05
 * @description:
 */
@Configuration
@Slf4j
public class CoinTraderConfig {

    @Autowired
    private RingBuffer<PlateModel> plateModel;

    @Bean("coinTraderFactory")
    public CoinTraderFactory getCoinTrader(){
        CoinTraderFactory factory = new CoinTraderFactory();
        CoinTrader trader = new CoinTrader("USDT-BTC",plateModel);
        factory.addTrader("USDT-BTC", trader);
        return factory;
    }
}
