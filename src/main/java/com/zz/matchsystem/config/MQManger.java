package com.zz.matchsystem.config;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.zz.matchsystem.event.MessageEventFactory;
import com.zz.matchsystem.event.PlateEventFactory;
import com.zz.matchsystem.event.handle.MessageEventHandle;
import com.zz.matchsystem.event.handle.PlateEventHandle;
import com.zz.matchsystem.model.MessageModel;
import com.zz.matchsystem.model.PlateModel;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @Author：EvilSay
 * @Date：17.10.23 23:50
 * @description:
 *
 * todo 可以抽象成工厂类交给Spring管理队列的生命周期
 */
@Component
public class MQManger {
    @Bean("messageModel")
    public RingBuffer<MessageModel> messageModelRingBuffer() {
        //定义用于事件处理的线程池，Disruptor通过java.util.concurrent.ExecutorSerivce提供的线程来触发consumer的事件处理
        ThreadFactory executor = Executors.defaultThreadFactory();

        //指定事件工厂
        MessageEventFactory factory = new MessageEventFactory();

        //指定RingBuffer字节大小，必须为2的N次方（能将求模运算转为位运算提高效率），否则将影响效率
        int bufferSize = 1024 * 256;

        //单线程模式，获取额外的性能
        Disruptor<MessageModel> disruptor = new Disruptor<>(factory, bufferSize,executor,
                ProducerType.SINGLE,new YieldingWaitStrategy());

        disruptor.handleEventsWith(new MessageEventHandle());

        disruptor.start();

        //获取RingBuffer环，用于接取生产者生产的事件
        return disruptor.getRingBuffer();
    }
    @Bean("plateModel")
    public RingBuffer<PlateModel> plateModelRingBuffer() {
        //定义用于事件处理的线程池，Disruptor通过java.util.concurrent.ExecutorSerivce提供的线程来触发consumer的事件处理
        ThreadFactory executor = Executors.defaultThreadFactory();

        //指定事件工厂
        PlateEventFactory factory = new PlateEventFactory();

        //指定RingBuffer字节大小，必须为2的N次方（能将求模运算转为位运算提高效率），否则将影响效率
        int bufferSize = 1024 * 256;

        //单线程模式，获取额外的性能
        Disruptor<PlateModel> disruptor = new Disruptor<>(factory, bufferSize,executor,
                ProducerType.SINGLE,new YieldingWaitStrategy());

        disruptor.handleEventsWith(new PlateEventHandle());

        disruptor.start();

        //获取RingBuffer环，用于接取生产者生产的事件
        return disruptor.getRingBuffer();
    }
}
