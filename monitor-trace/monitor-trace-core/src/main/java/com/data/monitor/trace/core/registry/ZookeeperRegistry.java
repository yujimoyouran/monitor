package com.data.monitor.trace.core.registry;

import com.data.monitor.base.constant.Constants;
import com.data.monitor.trace.core.dto.RegisterDto;
import com.data.monitor.trace.core.generate.IdGen;
import com.data.monitor.trace.core.generate.IncrementIdGen;
import org.I0Itec.zkclient.ZkClient;

/**
 * @author lk
 * 利用zookeeper实现注册中心
 */
public class ZookeeperRegistry implements Registry{

    /**
     * 想注册中心进行注册，生产该服务的编号并返回
     * @param registerDto
     * @return
     */
    @Override
    public String register(RegisterDto registerDto) {
        String host = registerDto.getHost();
        String app = registerDto.getApp();

        //向注册中心注册
        ZkClient zkClient = registerDto.getZkClient();
        zkClient.createPersistent(Constants.ZK_REGISTRY_SERVICE_ROOT_PATH + Constants.SLASH + app, true);
        IdGen idGen = new IncrementIdGen(registerDto);
        String id = idGen.nextId();

        zkClient.createEphemeral(Constants.ZK_REGISTRY_SERVICE_ROOT_PATH
        + Constants.SLASH + app + Constants.SLASH + host, id);
        return id;
    }








}
