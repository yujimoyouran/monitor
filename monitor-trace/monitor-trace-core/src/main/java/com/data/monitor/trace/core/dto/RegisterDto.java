package com.data.monitor.trace.core.dto;

import org.I0Itec.zkclient.ZkClient;

/**
 * @author lk
 * 向注册中心注册的信息dto
 */
public class RegisterDto {
    private String app;
    private String host;
    private ZkClient zkClient;

    public RegisterDto(){}

    public RegisterDto(String app,String host,ZkClient zkClient){
        this.app = app;
        this.host = host;
        this.zkClient = zkClient;
    }

    public RegisterDto setApp(String app) {
        this.app = app;
        return this;
    }

    public RegisterDto setHost(String host) {
        this.host = host;
        return this;
    }

    public RegisterDto setZkClient(ZkClient zkClient) {
        this.zkClient = zkClient;
        return this;
    }

    public String getApp() {
        return app;
    }

    public String getHost() {
        return host;
    }

    public ZkClient getZkClient() {
        return zkClient;
    }
}
