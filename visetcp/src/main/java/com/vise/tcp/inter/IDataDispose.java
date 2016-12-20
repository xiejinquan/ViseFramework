package com.vise.tcp.inter;

import com.vise.tcp.TcpConnection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2016-12-19 19:35
 */
public interface IDataDispose {
    IDataDispose DEFAULT = new IDataDispose() {

        @Override
        public void write(TcpConnection connection, ByteBuffer buffer, Object object) {

        }

        @Override
        public Object read(TcpConnection connection, ByteBuffer buffer) {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(new ByteArrayInputStream(buffer.array()));
                return ois.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
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

    void write(TcpConnection connection, ByteBuffer buffer, Object object);

    Object read(TcpConnection connection, ByteBuffer buffer);

    int getLengthLength();

    void writeLength(ByteBuffer buffer, int length);

    int readLength(ByteBuffer buffer);
}
