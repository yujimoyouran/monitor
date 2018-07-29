package com.data.monitor.logback.builder;

/**
 * ProducerRecord需要的key参数，根据该值进行分区
 */
public interface KeyBuilder<E> {
    /**
     * 生成producerRecord需要的key参数
     * @param e log event, ch.qos.logback.classic.spi.ILoggingEvent
     * @return
     */
    byte[] build(E e);
}
