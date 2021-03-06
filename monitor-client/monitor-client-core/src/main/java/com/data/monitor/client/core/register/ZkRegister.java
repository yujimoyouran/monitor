package com.data.monitor.client.core.register;

import com.data.monitor.base.constant.Constants;
import com.data.monitor.base.constant.RpcType;
import com.data.monitor.client.core.constant.NodeMode;
import com.data.monitor.client.core.util.SysUtil;
import com.data.monitor.trace.core.dto.RegisterDto;
import com.data.monitor.trace.core.generate.IncrementIdGen;
import com.data.monitor.trace.core.registry.Registry;
import com.data.monitor.trace.core.registry.ZookeeperRegistry;
import org.I0Itec.zkclient.ZkClient;

import java.util.ArrayList;
import java.util.List;

public class ZkRegister {
    private ZkClient client;

    public ZkRegister(ZkClient client) {
        this.client = client;
    }

    /**
     * 对app进行编号，如果一个host上部署了一个app的多个实例
     * 那么对app原生名字进行编号，第一个app后面加#01
     * 第二个后面加#02，以此类推
     * @param app
     * @param host
     * @return
     */
    public String mark(String app,String host){
        //计算编号
        List<String> apps = this.client.getChildren(Constants.ROOT_PATH_EPHEMERAL);
        List<String> marks = new ArrayList<>();
        int max = 0;
        for (String path: apps){
            if (path.startsWith(app)){
                //获取当前app
                List<String> hosts = this.client.getChildren(Constants.ROOT_PATH_EPHEMERAL + Constants.SLASH + path);
                if (hosts.contains(host)){
                    String[] am = path.split(Constants.JING_HAO);
                    marks.add(am[1]);
                    int mark = Integer.parseInt(am[1]);
                    if (max < mark){
                        max = mark;
                    }
                }
            }
        }
        //如果当前没有坑可以补
        if (max == marks.size()){
            return app + Constants.JING_HAO + String.format("%02d", max + 1);
        }
        //如果需要补坑，直接补
        for (int i = 1; i <= max; i++){
            String mark = String.format("%02d", i);
            if (!marks.contains(mark)) {
                return app + Constants.JING_HAO + mark;
            }
        }
        return app;
    }

    public void registerNode(String host, String app, String mail){
        // 注册永久节点用于历史日志查询
        this.create(Constants.SLASH + app + Constants.SLASH + host, NodeMode.PERSISTENT);
        this.getClient().writeData(Constants.ROOT_PATH_PERSISTENT + Constants.SLASH + app + Constants.SLASH + host,
                mail + Constants.SEMICOLON + SysUtil.userDir);
        // 注册临时节点用于日志滚屏
        this.getClient().createPersistent(Constants.ROOT_PATH_EPHEMERAL + Constants.SLASH + app, true);
        this.create(Constants.SLASH + app + Constants.SLASH + host, NodeMode.EPHEMERAL,
                Constants.APPENDER_INIT_DATA + Constants.SEMICOLON + SysUtil.userDir);
    }

    /**
     * rpc trace注册中心
     * @param host
     * @param app
     * @param rpc
     */
    public void registerRpc(String host,String app,String rpc){
        if (!rpc.equals(RpcType.none.symbol())){
            RegisterDto dto = new RegisterDto(app,host,this.client);
            Registry registry = new ZookeeperRegistry();
            IncrementIdGen.setId(registry.register(dto));
        }
    }

    /**
     * 创建带data的节点
     * @param path
     * @param nodeMode
     * @param data
     */
    private void create(String path, NodeMode nodeMode, String data) {
        if (nodeMode.symbol().equals(NodeMode.PERSISTENT.symbol())) {
            // 创建永久节点，加入数据
            this.client.createPersistent(nodeMode.label() + path, true);
        } else if (nodeMode.symbol().equals(NodeMode.EPHEMERAL.symbol())) {
            // 创建临时节点，加入数据
            this.client.createEphemeral(nodeMode.label() + path, data);
        }
    }

    /**
     * 创建节点
     * @param path
     * @param nodeMode
     */
    private void create(String path, NodeMode nodeMode){
        if (nodeMode.symbol().equals((NodeMode.PERSISTENT.symbol()))){
            //永久节点
            this.client.createPersistent(nodeMode.label()+path,true);
        } else if (nodeMode.symbol().equals(NodeMode.EPHEMERAL.symbol())){
            //零时节点
            this.client.createEphemeral(nodeMode.label()+path);
        }
    }

    /**
     * 写节点数据
     * @param path
     * @param nodeMode
     * @param data
     */
    public void write(String path,NodeMode nodeMode,String data){
        if (nodeMode.symbol().equals(NodeMode.PERSISTENT.symbol())){
            //
            this.client.writeData(nodeMode.label() + path, true);
        }else if (nodeMode.symbol().equals(NodeMode.EPHEMERAL.symbol())){
            //
            this.client.writeData(nodeMode.label()+path,data);
        }
    }

    public ZkClient getClient(){
        return client;
    }


















}
