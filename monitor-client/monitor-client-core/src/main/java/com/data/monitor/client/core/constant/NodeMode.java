package com.data.monitor.client.core.constant;

import com.data.monitor.base.constant.Constants;

/**
 * @author lk
 * 节点性质
 */
public enum  NodeMode {
    EPHEMERAL("EPHEMERAL",Constants.ROOT_PATH_EPHEMERAL),
    PERSISTENT("PERSISTENT",Constants.ROOT_PATH_PERSISTENT);
    //节点类型
    private String symbol;
    //目录
    private String label;

    private NodeMode(String symol, String label){
        this.label = label;
        this.symbol = symol;
    }

    public String symbol() {
        return symbol;
    }

    public String label() {
        return label;
    }
}
