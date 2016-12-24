package com.vise.udp.core.inter;

import com.vise.udp.core.UdpOperate;
import com.vise.udp.command.Command;
import com.vise.udp.core.Connection;
import com.vise.udp.mode.PacketBuffer;

import java.nio.ByteBuffer;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2016-12-21 16:28
 */
public interface IData {
    void write(Connection connection, PacketBuffer packetBuffer);

    PacketBuffer read(Connection connection, ByteBuffer buffer);

    int getLengthLength();

    void writeLength(ByteBuffer buffer, int length);

    int readLength(ByteBuffer buffer);

    IData DEFAULT = new IData() {
        @Override
        public void write(Connection connection, PacketBuffer packetBuffer) {

        }

        @Override
        public PacketBuffer read(Connection connection, ByteBuffer buffer) {
            PacketBuffer packetBuffer = new PacketBuffer();
            packetBuffer.setByteBuffer(buffer);
            return packetBuffer;
        }

        @Override
        public int getLengthLength() {
            return 4;
        }

        @Override
        public void writeLength(ByteBuffer buffer, int length) {
            buffer.putInt(length);
        }

        @Override
        public int readLength(ByteBuffer buffer) {
            return buffer.getInt();
        }
    };
}
