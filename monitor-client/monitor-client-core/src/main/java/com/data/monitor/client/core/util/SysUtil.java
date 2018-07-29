package com.data.monitor.client.core.util;

import com.data.monitor.base.constant.Constants;
import com.data.monitor.base.util.DockerHostUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * @author lk
 * 系统相关的util
 */
public class SysUtil {
    public static String host;
    public static String userDir;

    static {
        // host的读取，为了配合容器部署和rancher部署，首先是需要从环境变量里面
        // 取，如果取不到再从.skyeye/host这个文件取，最后再取不到从运行机器中取
        host = System.getenv(Constants.COMPUTERNAME);
        if (StringUtils.isBlank(host)){
            // 未获取到, 从docker设置的环境变量取
            host = System.getenv(Constants.SKYEYE_HOST_TO_REGISTRY);
            if (StringUtils.isBlank(host)){
                // 未获取到，从.skyeye/host获取
                host = DockerHostUtil.readFromFile();
                if (StringUtils.isBlank(host)) {
                    // 未获取到，从运行机器中取
                    host = DockerHostUtil.getHostFromLocal();
                }
            }
        }
        userDir = System.getProperty("user.dir","<NA>");
    }





}
