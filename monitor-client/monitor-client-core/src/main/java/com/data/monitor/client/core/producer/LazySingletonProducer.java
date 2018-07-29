package com.data.monitor.client.core.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import java.util.Map;

/**
 * @author lukuan
 * dubbo check 实现kafkaProducer的懒加载
 */
public class LazySingletonProducer {
    private static volatile Producer<byte[], String> producer;

    private LazySingletonProducer() {
    }

    public static Producer<byte[],String> getInstatnce(Map<String,Object> config){
        if (producer == null){
            synchronized (LazySingletonProducer.class){
                if (producer == null){
                    producer = new KafkaProducer<byte[], String>(config);
                }
            }
        }
        return producer;
    }

    /**
     * 是否初始化
     * @return
     */
    public static boolean isInstanced() {
        return producer != null;
    }















}
