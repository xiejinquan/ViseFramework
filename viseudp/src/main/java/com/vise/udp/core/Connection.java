package com.vise.udp.core;

import com.vise.log.ViseLog;
import com.vise.udp.core.inter.IData;
import com.vise.udp.core.inter.IListener;
import com.vise.udp.exception.UdpException;
import com.vise.udp.mode.PacketBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-23 15:11
 */
public class Connection {

    protected int id;
    protected String name;
    protected UdpOperate udpOperate;
    protected InetSocketAddress udpRemoteAddress;
    private List<IListener> listenerList = new ArrayList<>();

    protected Connection () {
    }

    void initialize (IData dataDispose, int bufferSize) {
        udpOperate = new UdpOperate(dataDispose, bufferSize);
    }

    public int send (PacketBuffer packetBuffer) {
        if (packetBuffer == null) throw new IllegalArgumentException("object cannot be null.");
        SocketAddress address = udpRemoteAddress;
        if (address == null && udpOperate != null) address = udpOperate.getConnectedAddress();
        if (address == null) throw new IllegalStateException("Connection is not connected via UDP.");
        try {
            if (address == null) throw new SocketException("Connection is closed.");
            int length = udpOperate.send(this, packetBuffer, address);
            if (length == 0) {
                ViseLog.d(this + " UDP had nothing to send.");
            } else {
                if (length != -1) {
                    String objectString = packetBuffer == null ? "null" : packetBuffer.getClass().getSimpleName();
                    ViseLog.d(this + " sent UDP: " + objectString + " (" + length + ")");
                } else {
                    ViseLog.d(this + " was unable to send, UDP socket buffer full.");
                }
            }
            return length;
        } catch (IOException ex) {
            ViseLog.e("Unable to send UDP with connection: " + this + ex);
            close();
            return 0;
        }
    }

    public void close () {
        if (udpOperate != null) {
            udpOperate.close();
        }
    }

    public void addListener(IListener listener) {
        if (listenerList.contains(listener)) {
            return;
        }
        this.listenerList.add(listener);
    }

    public void removeListener(IListener listener) {
        this.listenerList.remove(listener);
    }

    private void notifyReceiveListener(final PacketBuffer packetBuffer) {
        for (IListener listener : listenerList) {
            if (listener != null) {
                listener.onReceive(this, packetBuffer);
            }
        }
    }

    private void notifyStartListener() {
        for (IListener listener : listenerList) {
            if (listener != null) {
                listener.onStart(this);
            }
        }
    }

    private void notifyStopListener() {
        for (IListener listener : listenerList) {
            if (listener != null) {
                listener.onStop(this);
            }
        }
    }

    private void notifySendListener(final PacketBuffer packetBuffer) {
        for (IListener listener : listenerList) {
            if (listener != null) {
                listener.onSend(this, packetBuffer);
            }
        }
    }

    private void notifyErrorListener(final UdpException e) {
        for (IListener listener : listenerList) {
            if (listener != null) {
                listener.onError(this, e);
            }
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
