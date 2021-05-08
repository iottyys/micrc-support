package io.ttyys.micrc.sad.gradle.plugin.common;

/**
 * 请求类型
 */
public enum RequestEnum {
    LOCAL("local", "called", "calling", "本地同步请求"),
    RPC("rpc", "called", "calling", "远程同步请求"),
    INFORMATION("msg", "messaged", "messaging", "异步消息"),
    ;
    /**
     * 目录名称
     */
    private String dirName;
    /**
     * 定义目录
     */
    private String defDirName;
    /**
     * 调用目录
     */
    private String transferDirName;
    /**
     * 类型描述
     */
    private String label;

    RequestEnum(String dirName, String defDirName, String transferDirName, String label) {
        this.dirName = dirName;
        this.defDirName = defDirName;
        this.transferDirName = transferDirName;
        this.label = label;
    }

    public String getDirName() {
        return dirName;
    }

    public String getLabel() {
        return label;
    }

    public String getDefDirName() {
        return defDirName;
    }

    public String getTransferDirName() {
        return transferDirName;
    }
}
