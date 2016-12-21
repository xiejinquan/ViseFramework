package com.vise.udp.mode;

import com.vise.udp.command.Command;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-21 16:14
 */
public class PacketBuffer {

    private TargetInfo targetInfo;
    private ByteBuffer byteBuffer;
    private byte[] bytes;
    private Command command;

    public TargetInfo getTargetInfo() {
        return targetInfo;
    }

    public void setTargetInfo(TargetInfo targetInfo) {
        this.targetInfo = targetInfo;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return "PacketBuffer{" +
                "targetInfo=" + targetInfo +
                ", byteBuffer=" + byteBuffer +
                ", bytes=" + Arrays.toString(bytes) +
                ", command=" + command +
                '}';
    }
}
