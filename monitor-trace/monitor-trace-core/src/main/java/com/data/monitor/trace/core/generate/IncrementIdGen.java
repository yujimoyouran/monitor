package com.data.monitor.trace.core.generate;

import com.data.monitor.base.constant.Constants;
import com.data.monitor.trace.core.dto.RegisterDto;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.data.Stat;

public class IncrementIdGen implements IdGen {
    //为某台机器上的某个项目分配的serviceId（注意区分Span中的serviceId）
    private static String serviceId;
    //注册信息
    private RegisterDto registerDto;

    /**
     * 利用zookeeper
     * @return
     */
    @Override
    public String nextId() {
        String app = this.registerDto.getApp();
        String host = this.registerDto.getHost();
        ZkClient zkClient = this.registerDto.getZkClient();
        String path = Constants.ZK_REGISTRY_ID_ROOT_PATH + Constants.SLASH
                + app + Constants.SLASH + host;
        if (zkClient.exists(path)){
            //如果已经有该节点，表示已经为当前的host上部署的app分配过编号
            //应对某个服务重启之后编号不变的问题，直接获取id
            return zkClient.readData(path);
        }else{
            /*
            节点不存在，那么需要生产id，利用zk节点的版本号，
            每写一次就自增的机制来实现
             */
            Stat stat = zkClient.writeDataReturnStat(Constants.ZK_REGISTRY_SEQ,new byte[0],-1);
            //生产id
            String id = String.valueOf(stat.getVersion());
            zkClient.createPersistent(path,true);
            zkClient.writeData(path,id);
            return id;
        }
    }

    /**
     * 获取ID
     * @return
     */
    public static String getId() {
        return serviceId;
    }

    /**
     * 对ID赋值
     * @param id
     * @return
     */
    public static void setId(String id) {
        serviceId = id;
    }

    public IncrementIdGen() {

    }

    public IncrementIdGen(RegisterDto registerDto) {
        this.registerDto = registerDto;
    }

    public RegisterDto getRegisterDto() {
        return registerDto;
    }

    public IncrementIdGen setRegisterDto(RegisterDto registerDto) {
        this.registerDto = registerDto;
        return this;
    }









}






