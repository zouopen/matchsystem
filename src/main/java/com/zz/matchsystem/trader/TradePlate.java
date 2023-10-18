package com.zz.matchsystem.trader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author：EvilSay
 * @Date：17.10.23 20:19
 * @description: 盘口信息
 */
@Slf4j
@Data
public class TradePlate {
    private List<TradePlateItem> items;
    //最大深度
    private int maxDepth = 100;
    //方向
    private ExchangeOrderDirection direction;
    private String symbol;

    public TradePlate(String symbol, ExchangeOrderDirection direction) {
        this.direction = direction;
        this.symbol = symbol;
        items = Collections.synchronizedList(new LinkedList<>());
    }

    private static final ReentrantLock addLock = new ReentrantLock();

    public boolean add(ExchangeOrder exchangeOrder) {
        try {
            addLock.lock();
            int index = 0;
            if (exchangeOrder.getType() == ExchangeOrderType.MARKET_PRICE) {
                return false;
            }
            if (exchangeOrder.getDirection() != direction) {
                return false;
            }

            if (items.size() > 0) {
                for (index = 0; index < items.size(); index++) {
                    TradePlateItem item = items.get(index);
                    if (exchangeOrder.getDirection() == ExchangeOrderDirection.BUY && item.getPrice().compareTo(exchangeOrder.getPrice()) > 0
                            || exchangeOrder.getDirection() == ExchangeOrderDirection.SELL && item.getPrice().compareTo(exchangeOrder.getPrice()) < 0) {
                        continue;
                    } else if (item.getPrice().compareTo(exchangeOrder.getPrice()) == 0) {
                        BigDecimal deltaAmount = exchangeOrder.getAmount().subtract(exchangeOrder.getTradedAmount());
                        item.setAmount(item.getAmount().add(deltaAmount));
                        return true;
                    } else {
                        break;
                    }
                }
            }
            if (index < maxDepth) {
                TradePlateItem newItem = new TradePlateItem();
                newItem.setAmount(exchangeOrder.getAmount().subtract(exchangeOrder.getTradedAmount()));
                newItem.setPrice(exchangeOrder.getPrice());
                items.add(index, newItem);
            }
        } finally {
            addLock.unlock();
        }
        return true;
    }

    private static final ReentrantLock removeLock = new ReentrantLock();

    public void remove(ExchangeOrder order, BigDecimal amount) {
        try {
            removeLock.lock();
            for (int index = 0; index < items.size(); index++) {
                TradePlateItem item = items.get(index);
                if (item.getPrice().compareTo(order.getPrice()) == 0) {
                    item.setAmount(item.getAmount().subtract(amount));
                    if (item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        items.remove(index);
                    }
                    return;
                }
            }
            log.info("items>>return_size={}", items.size());
        } finally {
            removeLock.unlock();
        }
    }

    public void remove(ExchangeOrder order) {
        remove(order, order.getAmount().subtract(order.getTradedAmount()));
    }


    public void setItems(LinkedList<TradePlateItem> items) {
        this.items = items;
    }

    public BigDecimal getLowestPrice() {
        if (items == null || items.size() == 0) {
            return BigDecimal.ZERO;
        }
        if (direction == ExchangeOrderDirection.BUY) {
            return items.get(items.size() - 1).getPrice();
        } else {
            return items.get(0).getPrice();
        }
    }

    public BigDecimal getMaxAmount() {
        if (items == null || items.size() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = BigDecimal.ZERO;
        for (TradePlateItem item : items) {
            if (item.getAmount().compareTo(amount) > 0) {
                amount = item.getAmount();
            }
        }
        return amount;
    }

    public BigDecimal getHighestPrice() {
        if (items == null || items.size() == 0) {
            return BigDecimal.ZERO;
        }
        if (direction == ExchangeOrderDirection.BUY) {
            return items.get(0).getPrice();
        } else {
            return items.get(items.size() - 1).getPrice();
        }
    }

    public BigDecimal getMinAmount() {
        if (items == null || items.size() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = items.get(0).getAmount();
        for (TradePlateItem item : items) {
            if (item.getAmount().compareTo(amount) < 0) {
                amount = item.getAmount();
            }
        }
        return amount;
    }

    public JSONObject toJSON(int limit) {
        JSONObject json = new JSONObject();
        json.put("direction", direction);
        json.put("maxAmount", getMaxAmount());
        json.put("minAmount", getMinAmount());
        json.put("highestPrice", getHighestPrice());
        json.put("lowestPrice", getLowestPrice());
        json.put("symbol", getSymbol());
        json.put("items", items.size() > limit ? items.subList(0, limit) : items);
        return json;
    }

    public String toJSONString(){
        synchronized (items){
            return JSON.toJSONString(this);
        }
    }
}
