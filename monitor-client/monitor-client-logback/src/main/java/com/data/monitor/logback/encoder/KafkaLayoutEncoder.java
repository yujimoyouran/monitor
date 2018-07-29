package com.data.monitor.logback.encoder;

import ch.qos.logback.core.Layout;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

import java.nio.charset.Charset;

/**
 * kafka的encoder
 * @author lk
 * @param <E>
 */
public class KafkaLayoutEncoder<E> extends ContextAwareBase implements LifeCycle {
    //layout 定义日志输出格式
    private Layout<E> layout;
    //编码，默认UTF-8
    private Charset charset;
    private Boolean started = false;
    private static final Charset UTF8 = Charset.forName("utf-8");

    public String deEncode(E event){
        return this.layout.doLayout(event);
    }


    @Override
    public void start() {
        if (charset == null){
            addInfo("no set charset, set the default charset is utf-8");
            charset = UTF8;
        }
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    public void setLayout(Layout<E> layout) {
        this.layout = layout;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public Boolean getStarted() {
        return started;
    }

    public Layout<E> getLayout() {

        return layout;
    }

    public Charset getCharset() {
        return charset;
    }
}
