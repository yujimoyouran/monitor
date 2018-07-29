package com.data.monitor.base.constant;

public class Constants {
    //标点符号
    public static final String SLASH = "/";
    public  static final String JING_HAO = "#";
    public static final String SEMICOLON = ";";
    public static final String COLON = ":";
    public static final String MIDDLE_LINE = "-";

    //zk节点
    public static final String ROOT_PATH_EPHEMERAL = "/skyeye/monitor/scroll";
    public static final String ROOT_PATH_PERSISTENT = "/skyeye/monitor/query";
    public static final String APPENDER_INIT_DATA = "appender_init_data";

    public static final String EMPTY_STR = "";



    //docker容器相关
    public static final String COMPUTERNAME = "COMPUTERNAME";
    public static final String SKYEYE_HOST_TO_REGISTRY = "SKYEYE_HOST_TO_REGISTRY";
    public static final String UNKNOWN_HOST = "UnknownHost";
    public static final String SKYEYE_HOST_FILE = "SKYEYE_HOST_FILE";


    public static final String RPC_TYPE_NONE = "none";
    public static final String RPC_TYPE_DUBBO = "dubbo";
    public static final String RPC_TYPE_SC = "sc";

    //rpc服务注册中心相关
    public static final String ZK_REGISTRY_SERVICE_ROOT_PATH = "/skyeye/registry/service";
    public static final String ZK_REGISTRY_ID_ROOT_PATH = "/skyeye/registry/id";
    public static final String ZK_REGISTRY_SEQ = "/skyeye/seq";

    //log4j参数获取
    public static final String HOSTNAME = "APP_NAME";

    //心跳检测相关
    public static final String HEARTBEAT_KEY = "heart beat key";

    //微信报警
    public static final String APP_APPENDER_RESTART_KEY = "restart";
}
