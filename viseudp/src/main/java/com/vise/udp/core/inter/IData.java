package com.vise.udp.core.inter;

import com.vise.udp.core.UdpOperate;
import com.vise.udp.mode.PacketBuffer;

import java.nio.ByteBuffer;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2016-12-21 16:28
 */
public interface IData {
    void write(UdpOperate udpOperate, PacketBuffer packetBuffer);

    PacketBuffer read(UdpOperate udpOperate, ByteBuffer buffer);

    int getLengthLength();

    void writeLength(ByteBuffer buffer, int length);

    int readLength(ByteBuffer buffer);

    IData DEFAULT = new IData() {
        @Override
        public void write(UdpOperate udpOperate, PacketBuffer packetBuffer) {

        }

        @Override
        public PacketBuffer read(UdpOperate udpOperate, ByteBuffer buffer) {
            return null;
        }

        @Override
        public int getLengthLength() {
            return 0;
        }

        @Override
        public void writeLength(ByteBuffer buffer, int length) {

        }

        @Override
        public int readLength(ByteBuffer buffer) {
            return 0;
        }
    };
}
