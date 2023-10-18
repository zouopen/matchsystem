package com.zz.matchsystem.trader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lmax.disruptor.RingBuffer;
import com.sun.org.apache.xpath.internal.operations.Or;
import com.zz.matchsystem.model.MessageModel;
import com.zz.matchsystem.model.PlateModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author：EvilSay
 * @Date：17.10.23 20:00
 * @description:
 */
@Slf4j
public class CoinTrader {

    private final RingBuffer<PlateModel> plateModel;
    /**
     * 币种
     */
    private String symbol;

    /**
     * 买入限价列表
     */
    private TreeMap<BigDecimal,MergeOrder> buyLimitPriceQueue;

    /**
     * 卖出限价列表
     */
    private TreeMap<BigDecimal,MergeOrder> sellLimitPriceQueue;

    /**
     * 买入市价列表
     */
    private Deque<ExchangeOrder> buyMarketPriceQueue;

    /**
     * 卖出市价列表
     */
    private Deque<ExchangeOrder> sellMarketPriceQueue;

    /**
     * 买盘盘口
     */
    private TradePlate buyTradePlate;
    /**
     * 卖盘盘口
     */
    private TradePlate sellTradePlate;

    private SimpleDateFormat dateTimeFormat;

    public CoinTrader(String symbol,RingBuffer<PlateModel> plate){
        this.symbol = symbol;
        this.plateModel = plate;
        initialize();
    }
    /**
     * 初始化交易线程
     */
    public void initialize(){
        log.info("init CoinTrader for symbol {}",symbol);
        //买单队列价格降序排列
        buyLimitPriceQueue = new TreeMap<>(Comparator.reverseOrder());
        //卖单队列价格升序排列
        this.sellLimitPriceQueue = new TreeMap<>(Comparator.naturalOrder());
        this.buyMarketPriceQueue = new ArrayDeque<>();
        this.sellMarketPriceQueue = new ArrayDeque<>();
        this.sellTradePlate = new TradePlate(symbol,ExchangeOrderDirection.SELL);
        this.buyTradePlate = new TradePlate(symbol,ExchangeOrderDirection.BUY);
        this.dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 撮合主动输入的订单
     */
    public void trade(ExchangeOrder exchangeOrder){
        //买入量或成交量 <= 0退出
        if(exchangeOrder.getAmount().compareTo(BigDecimal.ZERO) <=0 || exchangeOrder.getAmount().subtract(exchangeOrder.getTradedAmount()).compareTo(BigDecimal.ZERO)<=0){
            return ;
        }
        //限价列表
        TreeMap<BigDecimal,MergeOrder> limitPriceOrderList;
        //市价列表
        Deque<ExchangeOrder> marketPriceOrderList;

        //当前买单方向
        if (exchangeOrder.getDirection().equals(ExchangeOrderDirection.BUY)){
            limitPriceOrderList  = sellLimitPriceQueue;
            marketPriceOrderList = sellMarketPriceQueue;
        }else{
            limitPriceOrderList  = buyLimitPriceQueue;
            marketPriceOrderList = buyMarketPriceQueue;
        }
        //订单为市价单
        if(exchangeOrder.getType().equals(ExchangeOrderType.MARKET_PRICE)){
            matchMarketPriceWithLPList(limitPriceOrderList,exchangeOrder);
        //订单为限价单
        }else if (exchangeOrder.getType() == ExchangeOrderType.LIMIT_PRICE){
            //限价单价格必须大于
            if(exchangeOrder.getPrice().compareTo(BigDecimal.ZERO) <= 0){
                return ;
            }
            //先与限价单交易
            matchLimitPriceWithLPList(limitPriceOrderList, exchangeOrder,false);
            if (exchangeOrder.getAmount().compareTo(exchangeOrder.getTradedAmount()) > 0){
                //后跟市价单进行交易
                matchLimitPriceWithMPList(marketPriceOrderList,exchangeOrder);
            }
        }
    }

    private static final ReentrantLock marketOrderLock = new ReentrantLock();
    /**
     *
     * 限价委托单与市价订单进行匹配
     * @param marketPriceOrderList 市价对手单队列
     * @param focusedOrder 委托订单
     */
    private void matchLimitPriceWithMPList(Deque<ExchangeOrder> marketPriceOrderList, ExchangeOrder focusedOrder) {
        //交易中队列
        ArrayDeque<ExchangeTrade> exchangeOrderDeque  = new ArrayDeque<>();
        //交易完成队列
        ArrayDeque<ExchangeOrder> completeOrderDeque  = new ArrayDeque<>();

        try {
            marketOrderLock.lock();

            Iterator<ExchangeOrder> iterator = marketPriceOrderList.iterator();
            while (iterator.hasNext()){
                ExchangeOrder matchOrder = iterator.next();
                ExchangeTrade trade = processMatch(focusedOrder, matchOrder);
                log.info(">>>>>"+trade);

                if(trade != null){
                    exchangeOrderDeque.add(trade);
                }
                //判断匹配单是否完成，市价单amount为成交量
                if(matchOrder.isCompleted()){
                    iterator.remove();
                    completeOrderDeque.add(matchOrder);
                }
                //判断吃单是否完成，判断成交量是否完成
                if (focusedOrder.isCompleted()) {
                    //交易完成
                    completeOrderDeque.add(focusedOrder);
                    //退出循环
                    break;
                }
            }
        }finally {
            marketOrderLock.unlock();
        }

        //如果还没有交易完，订单压入列表中
        if (focusedOrder.getTradedAmount().compareTo(focusedOrder.getAmount()) < 0) {
            addLimitPriceOrder(focusedOrder);
        }
        //每个订单的匹配批量推送
        handleExchangeTrade(exchangeOrderDeque);
        orderCompleted(completeOrderDeque);
    }

    /**
     * 限价委托订单与限价订单匹配
     * @param limitPriceOrderList 限价队列
     * @param exchangeOrder 委托订单
     * @param b
     */
    private static final ReentrantLock lockLP = new ReentrantLock();
    private void matchLimitPriceWithLPList(TreeMap<BigDecimal, MergeOrder> limitPriceOrderList, ExchangeOrder focusedOrder, boolean b) {
        ArrayDeque<ExchangeTrade> exchangeTrades = new ArrayDeque<>();
        ArrayDeque<ExchangeOrder> completedOrders  = new ArrayDeque<>();

        try {
            lockLP.lock();

            Iterator<Map.Entry<BigDecimal, MergeOrder>> mergeOrderIterator = limitPriceOrderList.entrySet().iterator();
            boolean exitLoop = false;
            while (!exitLoop && mergeOrderIterator.hasNext()){
                Map.Entry<BigDecimal, MergeOrder> entry = mergeOrderIterator.next();
                MergeOrder mergeOrder = entry.getValue();
                Iterator<ExchangeOrder> orderIterator = mergeOrder.iterator();

                while (orderIterator.hasNext()){
                    //买入单需要匹配的价格不大于委托价
                    if (focusedOrder.getDirection() == ExchangeOrderDirection.BUY && mergeOrder.getPrice().compareTo(focusedOrder.getPrice()) > 0){
                        break;
                    }
                    //卖出单需要匹配单价格小于委托价
                    if(focusedOrder.getDirection() == ExchangeOrderDirection.SELL && mergeOrder.getPrice().compareTo(focusedOrder.getPrice()) < 0){
                        break;
                    }
                    while (orderIterator.hasNext()) {
                        ExchangeOrder matchOrder = orderIterator.next();

                        //处理匹配
                        ExchangeTrade trade = processMatch(focusedOrder, matchOrder);
                        if(trade != null){
                            exchangeTrades.add(trade);
                        }
                        //判断匹配单是否完成
                        if (matchOrder.isCompleted()) {
                            //当前匹配的订单完成交易，删除该订单
                            orderIterator.remove();
                            completedOrders.add(matchOrder);
                        }
                        //判断交易单是否完成
                        if (focusedOrder.isCompleted()) {
                            //交易完成
                            completedOrders.add(focusedOrder);
                            //退出循环
                            exitLoop = true;
                            break;
                        }
                    }
                    if(mergeOrder.size() == 0){
                        mergeOrderIterator.remove();
                    }
                }
            }
        }finally {
            lockLP.unlock();
        }
        //如果还没有交易完，订单压入列表中
        if (focusedOrder.getTradedAmount().compareTo(focusedOrder.getAmount()) < 0 && b) {
            addLimitPriceOrder(focusedOrder);
        }
        //每个订单的匹配批量推送
        handleExchangeTrade(exchangeTrades);
        if(completedOrders.size() > 0){
            orderCompleted(completedOrders);
            TradePlate plate = focusedOrder.getDirection() == ExchangeOrderDirection.BUY ? sellTradePlate : buyTradePlate;
            sendTradePlateMessage(plate);
        }
    }
    private static final ReentrantLock limitOrderLock = new ReentrantLock();
    /**
     * 增加限价单到队列,买入单从高到底，卖出单按从低到高
     * @param exchangeOrder
     */
    private void addLimitPriceOrder(ExchangeOrder exchangeOrder) {
        if(exchangeOrder.getType() != ExchangeOrderType.LIMIT_PRICE){
            return ;
        }
        TreeMap<BigDecimal,MergeOrder> list;
        if (exchangeOrder.getDirection() == ExchangeOrderDirection.BUY) {
            list = buyLimitPriceQueue;
            buyTradePlate.add(exchangeOrder);

            sendTradePlateMessage(buyTradePlate);
        }else{
            list = sellLimitPriceQueue;
            sellTradePlate.add(exchangeOrder);

            sendTradePlateMessage(sellTradePlate);
        }


        try {
            limitOrderLock.lock();
            MergeOrder mergeOrder = list.get(exchangeOrder.getPrice());
            if (mergeOrder == null){
                mergeOrder = new MergeOrder();
                mergeOrder.add(exchangeOrder);
                list.put(exchangeOrder.getPrice(),mergeOrder);
            }else{
                mergeOrder.add(exchangeOrder);
            }
        }finally {
            limitOrderLock.unlock();
        }


    }

    /**
     * 市价单交易
     * @param limitPriceOrderList 限价对手单列表
     * @param exchangeOrder 待交易订单
     */
    private static final ReentrantLock reentrantLock = new ReentrantLock();
    private void matchMarketPriceWithLPList(TreeMap<BigDecimal, MergeOrder> limitPriceOrderList, ExchangeOrder focusedOrder) {
        //交易中队列
        ArrayDeque<ExchangeTrade> exchangeOrderDeque  = new ArrayDeque<>();
        //交易完成队列
        ArrayDeque<ExchangeOrder> completeOrderDeque  = new ArrayDeque<>();

        reentrantLock.lock();

        try {
            Iterator<Map.Entry<BigDecimal, MergeOrder>> mergeOrderIterator = limitPriceOrderList.entrySet().iterator();

            boolean loop = false;

            while (!loop && mergeOrderIterator.hasNext()){
                Map.Entry<BigDecimal, MergeOrder> mergeOrderEntry = mergeOrderIterator.next();
                MergeOrder mergeOrder = mergeOrderEntry.getValue();

                Iterator<ExchangeOrder> orderIterator = mergeOrder.iterator();

                while (orderIterator.hasNext()){
                    ExchangeOrder matchOrder = orderIterator.next();

                    //撮合开始
                    ExchangeTrade trade = processMatch(focusedOrder,matchOrder);

                    if (trade != null){
                        exchangeOrderDeque.add(trade);
                    }

                    //判断当前匹配单是否完成
                    if(matchOrder.isCompleted()){
                        //当前匹配的订单完成交易，删除该订单
                        mergeOrderIterator.remove();
                        completeOrderDeque.add(matchOrder);
                    }

                    //判断焦点订单是否完成
                    if (focusedOrder.isCompleted()){
                        completeOrderDeque.add(focusedOrder);
                        loop = true;
                        break;
                    }
                }
                if(mergeOrder.size() == 0){
                    mergeOrderIterator.remove();
                }
            }
        }finally {
            reentrantLock.unlock();
        }

        //如果还没有交易完,订单压入列表中,市价买单按成交量来算
        if (focusedOrder.getDirection() == ExchangeOrderDirection.SELL&&focusedOrder.getTradedAmount().compareTo(focusedOrder.getAmount()) < 0
                || focusedOrder.getDirection() == ExchangeOrderDirection.BUY&& focusedOrder.getTurnover().compareTo(focusedOrder.getAmount()) < 0) {
            addMarketPriceOrder(focusedOrder);
        }
        //每个订单的匹配批量推送
        handleExchangeTrade(exchangeOrderDeque);
        if(completeOrderDeque.size() > 0){
            orderCompleted(completeOrderDeque);
            TradePlate plate = focusedOrder.getDirection() == ExchangeOrderDirection.BUY ? sellTradePlate : buyTradePlate;
            sendTradePlateMessage(plate);
        }
    }

    private void sendTradePlateMessage(TradePlate plate) {
        long sequence = plateModel.next();

        try {
            PlateModel plateModel1 = plateModel.get(sequence);
            plateModel1.setPlate(plate);
        }finally {
            //发布Event，激活观察者去消费，将sequence传递给改消费者
            //注意最后的publish方法必须放在finally中以确保必须得到调用；如果某个请求的sequence未被提交将会堵塞后续的发布操作或者其他的producer
            plateModel.publish(sequence);
        }
    }

    private void orderCompleted(ArrayDeque<ExchangeOrder> completeOrderDeque) {
        log.info("[orderCompleted]交易完成的订单"+completeOrderDeque.toString());
    }

    private void handleExchangeTrade(ArrayDeque<ExchangeTrade> exchangeOrderDeque) {
        log.info("[handleExchangeTrade]处理过的订单"+exchangeOrderDeque.toString());
    }
    private static final ReentrantLock addLastLock = new ReentrantLock();
    private void addMarketPriceOrder(ExchangeOrder exchangeOrder) {

        if(exchangeOrder.getType() != ExchangeOrderType.MARKET_PRICE){
            return ;
        }
        log.info("addMarketPriceOrder,orderId = {}", exchangeOrder.getOrderId());
        //放入当前对手单列表
        Deque<ExchangeOrder> list = exchangeOrder.getDirection() == ExchangeOrderDirection.BUY ? buyMarketPriceQueue : sellMarketPriceQueue;

        try {
            addLastLock.lock();
            list.addLast(exchangeOrder);
        }finally {
            addLastLock.unlock();
        }

    }

    private ExchangeTrade processMatch(ExchangeOrder focusedOrder, ExchangeOrder matchOrder) {
        //成交量，成交价，可用数量
        BigDecimal needAmount,dealPrice,availAmount;
        //如果匹配单是限价单，则以其价格为成交价
        if (matchOrder.getType().equals(ExchangeOrderType.LIMIT_PRICE)){
            dealPrice = matchOrder.getPrice();
        }else{
            dealPrice = focusedOrder.getPrice();
        }

        if (dealPrice.compareTo(BigDecimal.ZERO) <= 0){
            return new ExchangeTrade();
        }
        //计算成交量
        needAmount =  calculateTradedAmount(focusedOrder,dealPrice);
        availAmount = calculateTradedAmount(matchOrder,dealPrice);

        //计算成交量
        BigDecimal tradedAmount = (availAmount.compareTo(needAmount) >= 0 ? needAmount : availAmount);
        log.info("[processMatch]dealPrice = {} amount ={}",dealPrice,tradedAmount);
        //如果成交额为0说明剩余额度无法成交，退出
        if (tradedAmount.compareTo(BigDecimal.ZERO) <= 0){
            return null;
        }

        //计算成交额
        BigDecimal turnover = tradedAmount.multiply(dealPrice);
        matchOrder.setTradedAmount(matchOrder.getTradedAmount().add(tradedAmount));
        matchOrder.setTurnover(matchOrder.getTurnover().add(turnover));
        focusedOrder.setTradedAmount(focusedOrder.getTradedAmount().add(tradedAmount));
        focusedOrder.setTurnover(focusedOrder.getTurnover().add(turnover));

        //创建成交量
        ExchangeTrade exchangeTrade = new ExchangeTrade();
        exchangeTrade.setSymbol(symbol);
        exchangeTrade.setAmount(tradedAmount);
        exchangeTrade.setDirection(focusedOrder.getDirection());
        exchangeTrade.setPrice(dealPrice);
        exchangeTrade.setBuyTurnover(turnover);
        exchangeTrade.setSellTurnover(turnover);

        //校正市价单剩余成交额
        if (ExchangeOrderType.MARKET_PRICE.equals(focusedOrder.getType()) && focusedOrder.getDirection().equals(ExchangeOrderDirection.BUY)){
            BigDecimal adjustTurnover = adjustMarketOrderTurnover(focusedOrder,dealPrice);
            exchangeTrade.setBuyTurnover(turnover.add(adjustTurnover));
        } else if (matchOrder.getType().equals(ExchangeOrderType.MARKET_PRICE) && matchOrder.getDirection().equals(ExchangeOrderDirection.BUY)) {
            BigDecimal adjustTurnover = adjustMarketOrderTurnover(matchOrder, dealPrice);
            exchangeTrade.setBuyTurnover(turnover.add(adjustTurnover));
        }

        if (focusedOrder.getDirection().equals(ExchangeOrderDirection.BUY)){
            exchangeTrade.setBuyOrderId(focusedOrder.getOrderId());
            exchangeTrade.setSellOrderId(matchOrder.getOrderId());
        }else{
            exchangeTrade.setBuyOrderId(matchOrder.getOrderId());
            exchangeTrade.setSellOrderId(focusedOrder.getOrderId());
        }

        //匹配限价单则从盘口中删除相关交易
        exchangeTrade.setTime(Calendar.getInstance().getTimeInMillis());
        if (matchOrder.getType().equals(ExchangeOrderType.LIMIT_PRICE)){
            if (matchOrder.getDirection().equals(ExchangeOrderDirection.BUY)){
                buyTradePlate.remove(matchOrder,tradedAmount);
            }else{
                sellTradePlate.remove(matchOrder,tradedAmount);
            }
        }
        return exchangeTrade;
    }

    /**
     * 调整市价单剩余成交额,当剩余成交额不足时，设置完成
     * @param focusedOrder 订单
     * @param dealPrice 价格
     * @return
     */
    private BigDecimal adjustMarketOrderTurnover(ExchangeOrder focusedOrder, BigDecimal dealPrice) {
        if (focusedOrder.getDirection().equals(ExchangeOrderDirection.BUY) && focusedOrder.getType().equals(ExchangeOrderType.MARKET_PRICE)){
            BigDecimal leftTurnover = focusedOrder.getAmount().subtract(focusedOrder.getTurnover());
            if (leftTurnover.divide(dealPrice,4, RoundingMode.DOWN).compareTo(BigDecimal.ZERO) == 0){
                focusedOrder.setTurnover(focusedOrder.getAmount());
                return leftTurnover;
            }
        }
        return BigDecimal.ZERO;
    }


    /**
     * 计算委托单剩余可成交的数量
     * @param focusedOrder 委托单
     * @param dealPrice 成交价格
     * @return
     */
    private BigDecimal calculateTradedAmount(ExchangeOrder focusedOrder, BigDecimal dealPrice) {
        if (focusedOrder.getDirection().equals(ExchangeOrderDirection.BUY) && focusedOrder.getType().equals(ExchangeOrderType.MARKET_PRICE)){
            //剩余成交量
            BigDecimal leftTurnover = focusedOrder.getAmount().subtract(focusedOrder.getTurnover());
            return leftTurnover.divide(dealPrice,4, RoundingMode.DOWN);
        }else{
            return focusedOrder.getAmount().subtract(focusedOrder.getTradedAmount());
        }
    }
}
