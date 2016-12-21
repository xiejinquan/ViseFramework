package com.vise.udp;

import com.vise.udp.config.UdpConfig;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-21 16:07
 */
public class ViseUdp {

    private static ViseUdp instance;
    private UdpConfig udpConfig;

    private ViseUdp(){
        udpConfig = new UdpConfig();
    }

    public static ViseUdp getInstance(){
        if (instance == null) {
            synchronized (ViseUdp.class) {
                if (instance == null) {
                    instance = new ViseUdp();
                }
            }
        }
        return instance;
    }

    public UdpConfig getUdpConfig() {
        return udpConfig;
    }
}
