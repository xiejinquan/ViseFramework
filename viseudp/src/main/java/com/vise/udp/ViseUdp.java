package com.vise.udp;

import com.vise.udp.config.UdpConfig;
import com.vise.udp.core.Client;
import com.vise.udp.core.Server;
import com.vise.udp.core.inter.IListener;
import com.vise.udp.mode.PacketBuffer;

import java.io.IOException;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-21 16:07
 */
public class ViseUdp {

    private static ViseUdp instance;
    private UdpConfig udpConfig = UdpConfig.getInstance();
    private Client client;
    private Server server;

    private ViseUdp(){
        init();
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

    private void init(){
        client = new Client();
        server = new Server();
    }

    public ViseUdp send(PacketBuffer packetBuffer) throws IOException {
        client.getUdpOperate().send(packetBuffer);
        return instance;
    }

    public ViseUdp startClient() {
        client.start();
        return instance;
    }

    public ViseUdp startServer() {
        server.start();
        return instance;
    }

    public ViseUdp connect() throws IOException {
        client.connect(udpConfig.getIp(), udpConfig.getPort());
        return instance;
    }

    public ViseUdp bindServer() throws IOException {
        server.bind(udpConfig.getPort());
        return instance;
    }

    public ViseUdp addClientListener(IListener listener){
        client.addListener(listener);
        return instance;
    }

    public ViseUdp addServerListener(IListener listener){
        server.addListener(listener);
        return instance;
    }

    public ViseUdp removeClientListener(IListener listener){
        client.removeListener(listener);
        return instance;
    }

    public ViseUdp removeServerListener(IListener listener){
        server.removeListener(listener);
        return instance;
    }

    public UdpConfig getUdpConfig() {
        return udpConfig;
    }

    public Client getClient() {
        return client;
    }

    public Server getServer() {
        return server;
    }
}
