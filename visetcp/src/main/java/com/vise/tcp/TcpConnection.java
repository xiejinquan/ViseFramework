package com.vise.tcp;

import com.vise.log.ViseLog;
import com.vise.tcp.command.Ping;
import com.vise.tcp.common.TcpConfig;
import com.vise.tcp.inter.IDataDispose;
import com.vise.tcp.inter.IThread;
import com.vise.tcp.listener.Listener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-20 10:18
 */
public class TcpConnection {
    public int id = -1;
    private volatile boolean isConnected;
    private String name;
    private Listener[] listeners = {};
    private Object listenerLock = new Object();
    private int lastPingID;
    private long lastPingSendTime;
    private int returnTripTime;

    private SocketChannel socketChannel;
    private SelectionKey selectionKey;
    private final IDataDispose dataDispose;
    private final ByteBuffer readBuffer, writeBuffer;
    private final Object writeLock = new Object();
    private int keepAliveMillis = TcpConfig.KEEP_ALIVE_MILLIS;
    private int timeoutMillis = TcpConfig.TIMEOUT_MILLIS;
    private float idleThreshold = 0.1f;
    private boolean bufferPositionFix;
    private int currentObjectLength;
    private volatile long lastWriteTime, lastReadTime;

    public TcpConnection (IDataDispose dataDispose, int writeBufferSize, int objectBufferSize) {
        this.dataDispose = dataDispose;
        writeBuffer = ByteBuffer.allocate(writeBufferSize);
        readBuffer = ByteBuffer.allocate(objectBufferSize);
        readBuffer.flip();
    }

    public SelectionKey accept (Selector selector, SocketChannel socketChannel) throws IOException {
        writeBuffer.clear();
        readBuffer.clear();
        readBuffer.flip();
        currentObjectLength = 0;
        try {
            this.socketChannel = socketChannel;
            socketChannel.configureBlocking(false);
            Socket socket = socketChannel.socket();
            socket.setTcpNoDelay(true);
            selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            ViseLog.d("Port " + socketChannel.socket().getLocalPort() + "/TCP connected to: "
                    + socketChannel.socket().getRemoteSocketAddress());
            lastReadTime = lastWriteTime = System.currentTimeMillis();
            return selectionKey;
        } catch (IOException ex) {
            close();
            throw ex;
        }
    }

    public void connect (Selector selector, SocketAddress remoteAddress, int timeout) throws IOException {
        close();
        writeBuffer.clear();
        readBuffer.clear();
        readBuffer.flip();
        currentObjectLength = 0;
        try {
            SocketChannel socketChannel = selector.provider().openSocketChannel();
            Socket socket = socketChannel.socket();
            socket.setTcpNoDelay(true);
            socket.setTrafficClass(TcpConfig.IPTOS_LOWDELAY);
            socket.connect(remoteAddress, timeout);
            socketChannel.configureBlocking(false);
            this.socketChannel = socketChannel;
            selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this);
            ViseLog.d("Port " + socketChannel.socket().getLocalPort() + "/TCP connected to: "
                    + socketChannel.socket().getRemoteSocketAddress());
            lastReadTime = lastWriteTime = System.currentTimeMillis();
        } catch (IOException ex) {
            close();
            IOException ioEx = new IOException("Unable to connect to: " + remoteAddress);
            ioEx.initCause(ex);//对异常进行包装，方便追查
            throw ioEx;
        }
    }

    public Object readObject () throws IOException {
        if (socketChannel == null) throw new SocketException("Connection is closed.");
        if (currentObjectLength == 0) {
            int lengthLength = dataDispose.getLengthLength();
            if (readBuffer.remaining() < lengthLength) {
                readBuffer.compact();
                int bytesRead = socketChannel.read(readBuffer);
                readBuffer.flip();
                if (bytesRead == -1) throw new SocketException("Connection is closed.");
                lastReadTime = System.currentTimeMillis();
                if (readBuffer.remaining() < lengthLength) return null;
            }
            currentObjectLength = dataDispose.readLength(readBuffer);
            if (currentObjectLength <= 0) throw new IOException("Invalid object length: " + currentObjectLength);
            if (currentObjectLength > readBuffer.capacity())
                throw new IOException("Unable to read object larger than read buffer: " + currentObjectLength);
        }
        int length = currentObjectLength;
        if (readBuffer.remaining() < length) {
            readBuffer.compact();
            int bytesRead = socketChannel.read(readBuffer);
            readBuffer.flip();
            if (bytesRead == -1) throw new SocketException("Connection is closed.");
            lastReadTime = System.currentTimeMillis();
            if (readBuffer.remaining() < length) return null;
        }
        currentObjectLength = 0;
        int startPosition = readBuffer.position();
        int oldLimit = readBuffer.limit();
        readBuffer.limit(startPosition + length);
        Object object;
        try {
            object = dataDispose.read(this, readBuffer);
        } catch (Exception ex) {
            throw new IOException("Error during deserialization.", ex);
        }
        readBuffer.limit(oldLimit);
        if (readBuffer.position() - startPosition != length)
            throw new IOException("Incorrect number of bytes (" + (startPosition + length - readBuffer.position())
                    + " remaining) used to deserialize object: " + object);
        return object;
    }

    public void writeOperation () throws IOException {
        synchronized (writeLock) {
            if (writeToSocket()) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
            lastWriteTime = System.currentTimeMillis();
        }
    }

    private boolean writeToSocket () throws IOException {
        if (socketChannel == null) throw new SocketException("Connection is closed.");
        ByteBuffer buffer = writeBuffer;
        buffer.flip();
        while (buffer.hasRemaining()) {
            if (bufferPositionFix) {
                buffer.compact();
                buffer.flip();
            }
            if (socketChannel.write(buffer) == 0) break;
        }
        buffer.compact();
        return buffer.position() == 0;
    }

    public int send (Object object) throws IOException {
        if (socketChannel == null) throw new SocketException("Connection is closed.");
        synchronized (writeLock) {
            int start = writeBuffer.position();
            int lengthLength = dataDispose.getLengthLength();
            writeBuffer.position(writeBuffer.position() + lengthLength);
            try {
                dataDispose.write(this, writeBuffer, object);
            } catch (Exception ex) {
                throw new IOException("Error serializing object of type: " + object.getClass().getName(), ex);
            }
            int end = writeBuffer.position();
            writeBuffer.position(start);
            dataDispose.writeLength(writeBuffer, end - lengthLength - start);
            writeBuffer.position(end);
            if (start == 0 && !writeToSocket()) {
                selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            } else {
                selectionKey.selector().wakeup();
            }
            float percentage = writeBuffer.position() / (float)writeBuffer.capacity();
            if (percentage > 0.75f) {
                ViseLog.d(this + " TCP write buffer is approaching capacity: " + percentage + "%");
            } else if (percentage > 0.25f) {
                ViseLog.d(this + " TCP write buffer utilization: " + percentage + "%");
            }
            lastWriteTime = System.currentTimeMillis();
            return end - start;
        }
    }

    public void close () {
        boolean wasConnected = isConnected;
        isConnected = false;
        try {
            if (socketChannel != null) {
                socketChannel.close();
                socketChannel = null;
                if (selectionKey != null) selectionKey.selector().wakeup();
            }
        } catch (IOException ex) {
            ViseLog.e("Unable to close TCP connection." + ex);
        }
        if (wasConnected) {
            notifyDisconnected();
            ViseLog.i(this + " disconnected.");
        }
        setConnected(false);
    }

    public void updateReturnTripTime () {
        Ping ping = new Ping();
        ping.setId(lastPingID++);
        lastPingSendTime = System.currentTimeMillis();
        try {
            send(ping);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addListener (Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
        synchronized (listenerLock) {
            Listener[] listeners = this.listeners;
            int n = listeners.length;
            for (int i = 0; i < n; i++) {
                if (listener == listeners[i]) return;
            }
            Listener[] newListeners = new Listener[n + 1];
            newListeners[0] = listener;
            System.arraycopy(listeners, 0, newListeners, 1, n);
            this.listeners = newListeners;
        }
        ViseLog.d("Connection listener added: " + listener.getClass().getName());
    }

    public void removeListener (Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
        synchronized (listenerLock) {
            Listener[] listeners = this.listeners;
            int n = listeners.length;
            if (n == 0) return;
            Listener[] newListeners = new Listener[n - 1];
            for (int i = 0, ii = 0; i < n; i++) {
                Listener copyListener = listeners[i];
                if (listener == copyListener) continue;
                if (ii == n - 1) return;
                newListeners[ii++] = copyListener;
            }
            this.listeners = newListeners;
        }
        ViseLog.d("Connection listener removed: " + listener.getClass().getName());
    }

    protected void notifyConnected () {
        if (socketChannel != null) {
            Socket socket = socketChannel.socket();
            if (socket != null) {
                InetSocketAddress remoteSocketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
                if (remoteSocketAddress != null) {
                    ViseLog.i(this + " connected: " + remoteSocketAddress.getAddress());
                }
            }
        }
        Listener[] listeners = this.listeners;
        for (int i = 0, n = listeners.length; i < n; i++)
            listeners[i].connected(this);
    }

    protected void notifyDisconnected () {
        Listener[] listeners = this.listeners;
        for (int i = 0, n = listeners.length; i < n; i++)
            listeners[i].disconnected(this);
    }

    protected void notifyIdle () {
        Listener[] listeners = this.listeners;
        for (int i = 0, n = listeners.length; i < n; i++) {
            listeners[i].idle(this);
            if (!isIdle()) break;
        }
    }

    protected void notifyReceived (Object object) {
        if (object instanceof Ping) {
            Ping ping = (Ping)object;
            if (ping.isReply()) {
                if (ping.getId() == lastPingID - 1) {
                    returnTripTime = (int)(System.currentTimeMillis() - lastPingSendTime);
                    ViseLog.d(this + " return trip time: " + returnTripTime);
                }
            } else {
                ping.setReply(true);
                try {
                    send(ping);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Listener[] listeners = this.listeners;
        for (int i = 0, n = listeners.length; i < n; i++) {
            listeners[i].received(this, object);
        }
    }

    public InetSocketAddress getRemoteAddressTCP () {
        if (socketChannel != null) {
            Socket socket = socketChannel.socket();
            if (socket != null) {
                return (InetSocketAddress)socket.getRemoteSocketAddress();
            }
        }
        return null;
    }

    public boolean needsKeepAlive (long time) {
        return socketChannel != null && keepAliveMillis > 0 && time - lastWriteTime > keepAliveMillis;
    }

    public boolean isTimedOut (long time) {
        return socketChannel != null && timeoutMillis > 0 && time - lastReadTime > timeoutMillis;
    }

    public void setBufferPositionFix (boolean bufferPositionFix) {
        this.bufferPositionFix = bufferPositionFix;
    }

    public void setName (String name) {
        this.name = name;
    }

    public int getTcpWriteBufferSize () {
        return writeBuffer.position();
    }

    /**
     * 判断是否空闲
     * @return
     */
    public boolean isIdle () {
        return writeBuffer.position() / (float)writeBuffer.capacity() < idleThreshold;
    }

    /**
     * 设置空闲比列
     * @param idleThreshold
     */
    public void setIdleThreshold (float idleThreshold) {
        this.idleThreshold = idleThreshold;
    }

    public void setConnected (boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected && name == null) name = "Connection " + id;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String toString () {
        if (name != null) return name;
        return "Connection " + id;
    }
}
