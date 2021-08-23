package org.yangxin.socket.client.bean;

import lombok.Data;

/**
 * @author yangxin
 * 2021/8/12 17:35
 */
@Data
public class ServerInfo {

    private String sn;
    private Integer port;
    private String address;

    public ServerInfo(Integer port, String ip, String sn) {
        this.port = port;
        this.address = ip;
        this.sn = sn;
    }
}
