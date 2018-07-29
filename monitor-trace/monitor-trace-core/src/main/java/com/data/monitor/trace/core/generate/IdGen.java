package com.data.monitor.trace.core.generate;

/**
 * @author lk
 * ID生成器
 */
public interface IdGen {
    /**
     * 生产下一个id
     * @return
     */
    String nextId();
}
