package com.vise.udp.core.inter;

import com.vise.udp.core.Connection;
import com.vise.udp.exception.UdpException;
import com.vise.udp.mode.PacketBuffer;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2016-12-21 16:23
 */
public interface IListener {
    void onStart(Connection connection);

    void onStop(Connection connection);

    void onSend(Connection connection, PacketBuffer packetBuffer);

    void onReceive(Connection connection, PacketBuffer packetBuffer);

    void onError(Connection connection, UdpException e);
}
