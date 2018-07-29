package com.data.monitor.logback.appeneder;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.hook.DelayingShutdownHook;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import com.data.monitor.base.constant.Constants;
import com.data.monitor.base.constant.RpcType;
import com.data.monitor.client.core.constant.KafkaConfig;
import com.data.monitor.client.core.constant.NodeMode;
import com.data.monitor.client.core.kafka.partitioner.KeyModPartitioner;
import com.data.monitor.client.core.producer.LazySingletonProducer;
import com.data.monitor.client.core.register.ZkRegister;
import com.data.monitor.client.core.util.SysUtil;
import com.data.monitor.logback.builder.KeyBuilder;
import com.data.monitor.logback.encoder.KafkaLayoutEncoder;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author lk
 * @param <E>
 *     kafkaAppender,包含logback kafka appender的配置
 */
public class KafkaAppender<E> extends UnsynchronizedAppenderBase<E> {

    private String topic;
    private String host;
    private String app;
    private String zkServers;
    private String mail;
    //标记是否为rpc服务，取值为RpcTypr.java
    private String rpc;
    //kafkaProducer类的配置
    private Map<String,Object> config = new HashMap<>();
    //key生成器
    private KeyBuilder<? super E> keyBuilder;
    //编码器
    private KafkaLayoutEncoder encoder;
    //zk注册器
    private ZkRegister zkRegister;
    private DelayingShutdownHook shutdownHook;
    // kafkaAppender遇到异常需要向zk进行写入数据，由于onCompletion()的调用
    // 在kafka集群完全挂掉时会有很多阻塞的日志会调用，
    // 所以我们需要保证只向zk写一次数据，监控中心只会发生一次报警
    private volatile AtomicBoolean flag = new AtomicBoolean(true);
    //心跳检测
    private Timer timer;
    private byte[] key;
    //原始app
    private String orginApp;

    public KafkaAppender() {
        this.checkAndSetConfig(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        this.checkAndSetConfig(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // 设置分区类, 使用自定义的KeyModPartitioner，同样的key进入相同的partition
        this.checkAndSetConfig(ProducerConfig.PARTITIONER_CLASS_CONFIG, KeyModPartitioner.class.getName());

        // 由于容器部署需要从外部获取host
        this.checkAndSetConfig(ProducerConfig.CLIENT_ID_CONFIG, this.app + Constants.MIDDLE_LINE + this.host + Constants.MIDDLE_LINE + "logback");
        //不要使用kill -9，使用kill即可安全关闭，否则有可能发生内存泄漏
        shutdownHook = new DelayingShutdownHook();
    }

    @Override
    public void start() {
        //xml配置校验
        if (!this.checkNecessaryConfig()){
            addError("necessary config is not set, kafka appender is not started");
            return;
        }

        super.start();
        // 添加logback shutdown hook, 关闭所有的appender, 调用stop()方法
        shutdownHook.setContext(this.getContext());
        Runtime.getRuntime().addShutdownHook(new Thread(this.shutdownHook));
        //初始化zk
        this.zkRegister = new ZkRegister(new ZkClient(this.zkServers,60000,5000));
        //对app重新编号，防止一台host部署一个app的多个实例
        this.orginApp = app;
        this.app = this.zkRegister.mark(this.app,this.host);
        this.key = ByteBuffer.allocate(4).putInt(new StringBuilder(this.app).append(this.host).toString().hashCode()).array();
        //注册节点
        this.zkRegister.registerNode(this.host,this.app,this.mail);
        //rpc trace 注册中心
        this.zkRegister.registerRpc(this.host,this.app,this.rpc);
    }

    @Override
    public void stop() {
        super.stop();
        //停止心跳
        this.heartbeatStop();
        //关闭kafkaProuder
        if (LazySingletonProducer.isInstanced()){
            LazySingletonProducer.getInstatnce(config).close();
        }
        //关闭client，临时几点消失，监控系统进行感知报警
        ZkClient client = this.zkRegister == null ? null : this.zkRegister.getClient();
        if (null != client){
            client.close();
        }
    }

    @Override
    public void doAppend(E e) {
        if (!started){
            return;
        }
        final String value = System.nanoTime() + Constants.SEMICOLON
                + this.encoder.deEncode(e);
        /*
        对value的大小进行判定，当大于某个值，认为该日志太大直接对其
        防止影响到kafka
         */
        if (value.length() > 10000){
            return;
        }
        final ProducerRecord<byte[], String> record = new ProducerRecord<>(this.topic,
                this.key,value.replaceFirst(this.orginApp,this.app).replaceFirst(Constants.HOSTNAME,this.host));

        LazySingletonProducer.getInstatnce(this.config).send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (null != exception){
                    /*
                    如果发生异常，将开始状态设置为false，并且每次append的时候
                    都先check该状态
                     */
                    started = false;
                    addStatus(new ErrorStatus("kafka send error in appender", this, exception));
                    /*
                    发生异常，kakfaappender停止收集，
                    想借点写入数据（监控系统会感知进行报警）
                     */
                    if (flag.get()){
                        KafkaAppender.this.heartbeatSart();
                    }
                }
            }
        });


    }

    /**
     * 心跳检测开始
     */
    private void heartbeatSart() {
        //心跳检测定时器初始化
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                byte[] key = ByteBuffer.allocate(4).putInt(Constants.HEARTBEAT_KEY.hashCode()).array();
                final  ProducerRecord<byte[],String> record = new ProducerRecord<>(topic,key,Constants.HEARTBEAT_KEY);
                LazySingletonProducer.getInstatnce(config).send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception exception) {
                        if (null == exception){
                            //更新flag状态
                            flag.compareAndSet(false,true);
                            // 如果没有发生异常, 说明kafka从异常状态切换为正常状态, 将开始状态设置为true
                            started = true;
                            addStatus(new InfoStatus("kafka send normal in appender", this, exception));
                            // 关闭心跳检测机制
                            KafkaAppender.this.heartbeatStop();
                            zkRegister.write(Constants.SLASH + app + Constants.SLASH + host, NodeMode.EPHEMERAL,
                                    String.valueOf(Constants.APP_APPENDER_RESTART_KEY + Constants.SEMICOLON + System.currentTimeMillis()) + Constants.SEMICOLON + SysUtil.userDir);

                        }
                    }
                });
            }
        },10000,60000);
    }

    /**
     * 停止心跳检测
     */
    private void heartbeatStop() {
        if (null != this.timer){
            this.timer.cancel();
        }
    }

    /**
     * 校验最基本的配置是否在loback.xml进行配置
     * @return
     */
    private boolean checkNecessaryConfig() {
        boolean flag = true;
        //app配置
        if (StringUtils.isBlank(this.host)){
            //host未获取到
            addError("不能获取host");
            flag = false;
        }
        if (StringUtils.isBlank(this.app)) {
            // app name未设置
            addError("logback.xml is not set the <contextName></contextName> node");
            flag = false;
        }

        // zk配置
        if (StringUtils.isBlank(this.zkServers)) {
            // zk地址未设置
            addError("can't get zkServers");
            flag = false;
        }

        // kafka配置
        if (null == config.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            // kafka的bootstrap.servers未设置
            addError("kafka's " + ProducerConfig.BOOTSTRAP_SERVERS_CONFIG + " do not set, appender: " + name);
            flag = false;
        }

        if (StringUtils.isBlank(this.topic)) {
            // topic未设置（或者设置成了""），无法写入kafka
            addError("topic is not set, appender: " + name);
            flag = false;
        }

        if (StringUtils.isBlank(this.mail)) {
            // 报警mail未设置
            addError("mail is not set, appender: " + name);
            flag = false;
        }

        if (StringUtils.isBlank(this.rpc) || !this.checkRpcType(this.rpc)) {
            // rpc未设置或者rpc值不对
            addError("rpc is not set or value not right, appender: " + name);
            flag = false;
        }

        if (null == this.keyBuilder) {
            // key生成器为设置
            addError("key builder is not set, appender: " + name);
            flag = false;
        }

        if (null == this.encoder) {
            // 编码器未设置
            addError("encoder is not set, appender: " + name);
            flag = false;
        }
        return flag;
    }

    /**
     * 监察rpc type是否正确
     * @param rpcType
     * @return
     */
    private boolean checkRpcType(String rpcType){
        try{
            RpcType.valueOf(rpcType);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public void setContext(Context context) {
        super.setContext(context);

        this.host = SysUtil.host;
        this.app = context.getName();
    }

    @Override
    protected void append(E eventObject) {

    }

    public void checkAndSetConfig(String key,String value){
        if (!KafkaConfig.PRODUCER_CONFIG_KEYS.contains(key)){
            //当前kafka版本没有该配置项
            addWarn("in this kafka version don't has this config: " + key);
        }
        this.config.put(key,value);
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public KeyBuilder<? super E> getKeyBuilder() {
        return keyBuilder;
    }

    public void setKeyBuilder(KeyBuilder<? super E> keyBuilder) {
        this.keyBuilder = keyBuilder;
    }

    public KafkaLayoutEncoder<E> getEncoder() {
        return encoder;
    }

    public void setEncoder(KafkaLayoutEncoder<E> encoder) {
        this.encoder = encoder;
    }

    public String getZkServers() {
        return zkServers;
    }

    public void setZkServers(String zkServers) {
        this.zkServers = zkServers;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getRpc() {
        return rpc;
    }

    public void setRpc(String rpc) {
        this.rpc = rpc;
    }















}
