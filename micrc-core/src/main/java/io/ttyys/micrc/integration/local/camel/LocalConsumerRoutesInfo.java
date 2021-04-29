package io.ttyys.micrc.integration.local.camel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地消费路由信息
 *
 * @author tengwang
 * @version 1.0.0
 * @date 2021/4/26 3:00 下午
 */
public class LocalConsumerRoutesInfo {

    /**
     * 所有用于构建路由的信息
     */
    private List<Map<String, Object>> routesInfo;

    private LocalConsumerRoutesInfo() {}

    public LocalConsumerRoutesInfo(List<Map<String, Object>> info) {
        this.routesInfo = info;
    }

    public List<Map<String, Object>> getRoutesInfo() {
        return routesInfo;
    }
}
