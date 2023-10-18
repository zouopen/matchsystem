package com.zz.matchsystem;

import cn.hutool.core.thread.ConcurrencyTester;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpRequest;
import com.zz.matchsystem.trader.ExchangeOrderDirection;
import com.zz.matchsystem.trader.ExchangeOrderType;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author：EvilSay
 * @Date：18.10.23 00:35
 * @description:
 */
public class MatchTest {
    public static void main(String[] args) {
        buyLPOrder();
        buyMPOrder();
//        sellMPOrder();
//        sellLPOrder();
//        LPOne();
    }

    public static void LPOne(){
        Map<String,Object> paramsMap = new HashMap<>();
        paramsMap.put("direction",ExchangeOrderDirection.BUY);
        paramsMap.put("price",140);
        paramsMap.put("amount",1);
        paramsMap.put("type", ExchangeOrderType.LIMIT_PRICE);
        ConcurrencyTester concurrencyTester = ThreadUtil.concurrencyTest(1, () -> {
            System.out.println(HttpRequest.post("http://localhost:8080/order/add")
                    .timeout(20000).form(paramsMap).execute().body());
        });
    }

    public static void buyLPOrder(){
        Map<String,Object> paramsMap = new HashMap<>();
        paramsMap.put("direction",ExchangeOrderDirection.BUY);
        paramsMap.put("price",120);
        paramsMap.put("amount",10000);
        paramsMap.put("type", ExchangeOrderType.LIMIT_PRICE);
        ConcurrencyTester concurrencyTester = ThreadUtil.concurrencyTest(10, () -> {
            System.out.println(HttpRequest.post("http://localhost:8080/order/add")
                    .timeout(20000).form(paramsMap).execute().body());
        });
    }
    public static void buyMPOrder(){
        Map<String,Object> paramsMap = new HashMap<>();
        paramsMap.put("direction",ExchangeOrderDirection.BUY);
        paramsMap.put("price",0);
        paramsMap.put("amount",1);
        paramsMap.put("type", ExchangeOrderType.MARKET_PRICE);
        ConcurrencyTester concurrencyTester = ThreadUtil.concurrencyTest(30, () -> {
            System.out.println(HttpRequest.post("http://localhost:8080/order/add")
                    .timeout(20000).form(paramsMap).execute().body());
        });
    }
    public static void sellLPOrder(){
        Map<String,Object> paramsMap = new HashMap<>();
        paramsMap.put("direction",ExchangeOrderDirection.SELL);
        paramsMap.put("price",120);
        paramsMap.put("amount",10000);
        paramsMap.put("type", ExchangeOrderType.LIMIT_PRICE);
        ConcurrencyTester concurrencyTester = ThreadUtil.concurrencyTest(10, () -> {
            System.out.println(HttpRequest.post("http://localhost:8080/order/add")
                    .timeout(20000).form(paramsMap).execute().body());
        });
    }

    public static void sellMPOrder(){
        Map<String,Object> paramsMap = new HashMap<>();
        paramsMap.put("direction",ExchangeOrderDirection.SELL);
        paramsMap.put("price",0);
        paramsMap.put("amount",1);
        paramsMap.put("type", ExchangeOrderType.MARKET_PRICE);
        ConcurrencyTester concurrencyTester = ThreadUtil.concurrencyTest(30, () -> {
            System.out.println(HttpRequest.post("http://localhost:8080/order/add")
                    .timeout(20000).form(paramsMap).execute().body());
        });
    }


}
