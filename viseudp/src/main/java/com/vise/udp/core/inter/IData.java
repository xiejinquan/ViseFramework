package com.vise.udp.core.inter;

import com.vise.udp.command.Command;
import com.vise.udp.core.UdpOperate;
import com.vise.udp.mode.PacketBuffer;
import com.vise.udp.mode.TargetInfo;

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
            PacketBuffer packetBuffer = new PacketBuffer();
            packetBuffer.setByteBuffer(buffer);
            packetBuffer.setCommand(new Command());
            packetBuffer.setTargetInfo(new TargetInfo());
            packetBuffer.setBytes(new byte[4]);
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
