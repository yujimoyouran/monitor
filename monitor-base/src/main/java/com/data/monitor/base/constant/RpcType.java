package com.data.monitor.base.constant;

/**
 * @author lk
 * rpc type类型
 */
public enum RpcType {
    none(Constants.RPC_TYPE_NONE,"none"),
    dubbo(Constants.RPC_TYPE_DUBBO, "dubbo"),
    sc(Constants.RPC_TYPE_SC, "spring-clout");

    private String symbol;
    private String label;

    private RpcType(String symbol, String label){
        this.symbol = symbol;
        this.label = label;
    }

    public String symbol() {
        return symbol;
    }

    public String label() {
        return label;
    }
}
