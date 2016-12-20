package com.vise.tcp;

import com.vise.log.ViseLog;
import com.vise.tcp.command.KeepAlive;
import com.vise.tcp.command.RegisterTcp;
import com.vise.tcp.common.TcpConfig;
import com.vise.tcp.inter.IDataDispose;
import com.vise.tcp.inter.IThread;
import com.vise.tcp.listener.Listener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-20 10:18
 */
public class TcpServer implements IThread {
    private final IDataDispose dataDispose;
    private final int writeBufferSize, objectBufferSize;
    private final Selector selector;
    private TcpConnection tcp;
    private int emptySelects;
    private ServerSocketChannel serverChannel;
    private TcpConnection[] connections = {};
    private Listener[] listeners = {};
    private Object listenerLock = new Object();
    private int nextConnectionID = 1;
    private volatile boolean shutdown;
    private Object updateLock = new Object();
    private Thread updateThread;

    private Listener dispatchListener = new Listener() {
        public void connected(TcpConnection connection) {
            Listener[] listeners = TcpServer.this.listeners;
            for (int i = 0, n = listeners.length; i < n; i++) {
                listeners[i].connected(connection);
            }
        }

        public void disconnected(TcpConnection connection) {
            removeConnection(connection);
            Listener[] listeners = TcpServer.this.listeners;
            for (int i = 0, n = listeners.length; i < n; i++) {
                listeners[i].disconnected(connection);
            }
        }

        public void received(TcpConnection connection, Object object) {
            Listener[] listeners = TcpServer.this.listeners;
            for (int i = 0, n = listeners.length; i < n; i++) {
                listeners[i].received(connection, object);
            }
        }

        public void idle(TcpConnection connection) {
            Listener[] listeners = TcpServer.this.listeners;
            for (int i = 0, n = listeners.length; i < n; i++) {
                listeners[i].idle(connection);
            }
        }
    };

    public TcpServer() {
        this(TcpConfig.SERVER_WRITE_BUFFER_SIZE, TcpConfig.SERVER_READ_BUFFER_SIZE);
    }

    public TcpServer(int writeBufferSize, int objectBufferSize) {
        this(writeBufferSize, objectBufferSize, IDataDispose.DEFAULT);
    }

    public TcpServer(int writeBufferSize, int objectBufferSize, IDataDispose dataDispose) {
        this.writeBufferSize = writeBufferSize;
        this.objectBufferSize = objectBufferSize;
        this.dataDispose = dataDispose;
        try {
            selector = Selector.open();
        } catch (IOException ex) {
            throw new RuntimeException("Error opening selector.", ex);
        }
    }

    public IDataDispose getDataDispose() {
        return dataDispose;
    }

    public void bind(int tcpPort) throws IOException {
        bind(new InetSocketAddress(tcpPort));
    }

    public void bind(InetSocketAddress tcpPort) throws IOException {
        close();
        synchronized (updateLock) {
            selector.wakeup();
            try {
                serverChannel = selector.provider().openServerSocketChannel();
                serverChannel.socket().bind(tcpPort);
                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                ViseLog.d("Accepting connections on port: " + tcpPort + "/TCP");
            } catch (IOException ex) {
                close();
                throw ex;
            }
        }
        ViseLog.i("Server opened.");
    }

    public void update(int timeout) throws IOException {
        updateThread = Thread.currentThread();
        synchronized (updateLock) {
        }
        long startTime = System.currentTimeMillis();
        int select = 0;
        if (timeout > 0) {
            select = selector.select(timeout);
        } else {
            select = selector.selectNow();
        }
        if (select == 0) {
            emptySelects++;
            if (emptySelects == 100) {
                emptySelects = 0;
                long elapsedTime = System.currentTimeMillis() - startTime;
                try {
                    if (elapsedTime < 25) Thread.sleep(25 - elapsedTime);
                } catch (InterruptedException ex) {
                }
            }
        } else {
            emptySelects = 0;
            Set<SelectionKey> keys = selector.selectedKeys();
            synchronized (keys) {
                outer:
                for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext(); ) {
                    keepAlive();
                    SelectionKey selectionKey = iter.next();
                    iter.remove();
                    TcpConnection fromConnection = (TcpConnection) selectionKey.attachment();
                    try {
                        int ops = selectionKey.readyOps();
                        if (fromConnection != null) {
                            if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                                try {
                                    while (true) {
                                        Object object = fromConnection.readObject();
                                        if (object == null) break;
                                        String objectString = object == null ? "null" : object.getClass()
                                                .getSimpleName();
                                        ViseLog.d(fromConnection + " received TCP: " + objectString);
                                        fromConnection.notifyReceived(object);
                                    }
                                } catch (IOException ex) {
                                    ViseLog.e("Unable to read TCP from: " + fromConnection + ex);
                                    fromConnection.close();
                                }
                            }
                            if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                                try {
                                    fromConnection.writeOperation();
                                } catch (IOException ex) {
                                    ViseLog.e("Unable to write TCP to connection: " + fromConnection + ex);
                                    fromConnection.close();
                                }
                            }
                            continue;
                        }
                        if ((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                            if (serverChannel == null) continue;
                            try {
                                SocketChannel socketChannel = serverChannel.accept();
                                if (socketChannel != null) acceptOperation(socketChannel);
                            } catch (IOException ex) {
                                ViseLog.e("Unable to accept new connection." + ex);
                            }
                            continue;
                        }
                    } catch (CancelledKeyException ex) {
                        if (fromConnection != null) {
                            fromConnection.close();
                        } else {
                            selectionKey.channel().close();
                        }
                    }
                }
            }
        }
        long time = System.currentTimeMillis();
        for (int i = 0, n = connections.length; i < n; i++) {
            TcpConnection connection = connections[i];
            if (connection.isTimedOut(time)) {
                ViseLog.d(connection + " timed out.");
                connection.close();
            } else {
                if (connection.needsKeepAlive(time)) connection.send(new KeepAlive());
            }
            if (connection.isIdle()) connection.notifyIdle();
        }
    }

    private void keepAlive() throws IOException {
        long time = System.currentTimeMillis();
        for (int i = 0, n = connections.length; i < n; i++) {
            TcpConnection connection = connections[i];
            if (connection.needsKeepAlive(time)) connection.send(new KeepAlive());
        }
    }

    public void run() {
        ViseLog.d("Server thread started.");
        shutdown = false;
        while (!shutdown) {
            try {
                update(250);
            } catch (IOException ex) {
                ViseLog.e("Error updating server connections." + ex);
                close();
            }
        }
        ViseLog.d("Server thread stopped.");
    }

    public void start() {
        new Thread(this, "Server").start();
    }

    public void stop() {
        if (shutdown) return;
        close();
        ViseLog.d("Server thread stopping.");
        shutdown = true;
    }

    private void acceptOperation(SocketChannel socketChannel) {
        tcp = new TcpConnection(dataDispose, writeBufferSize, objectBufferSize);
        TcpConnection connection = tcp;
        try {
            SelectionKey selectionKey = connection.accept(selector, socketChannel);
            selectionKey.attach(connection);

            int id = nextConnectionID++;
            if (nextConnectionID == -1) nextConnectionID = 1;
            connection.id = id;
            connection.setConnected(true);
            connection.addListener(dispatchListener);
            addConnection(connection);

            RegisterTcp registerConnection = new RegisterTcp();
            registerConnection.setConnectionID(id);
            connection.send(registerConnection);
            connection.notifyConnected();
        } catch (IOException ex) {
            connection.close();
            ViseLog.e("Unable to accept TCP connection." + ex);
        }
    }

    private void addConnection(TcpConnection connection) {
        TcpConnection[] newConnections = new TcpConnection[connections.length + 1];
        newConnections[0] = connection;
        System.arraycopy(connections, 0, newConnections, 1, connections.length);
        connections = newConnections;
    }

    public void removeConnection(TcpConnection connection) {
        ArrayList<TcpConnection> temp = new ArrayList(Arrays.asList(connections));
        temp.remove(connection);
        connections = temp.toArray(new TcpConnection[temp.size()]);
    }

    public void sendToAllTCP(Object object) {
        TcpConnection[] connections = this.connections;
        for (int i = 0, n = connections.length; i < n; i++) {
            TcpConnection connection = connections[i];
            try {
                connection.send(object);
            } catch (IOException e) {
                e.printStackTrace();
                ViseLog.e(e);
            }
        }
    }

    public void sendToAllExceptTCP(int connectionID, Object object) {
        TcpConnection[] connections = this.connections;
        for (int i = 0, n = connections.length; i < n; i++) {
            TcpConnection connection = connections[i];
            if (connection.id != connectionID) {
                try {
                    connection.send(object);
                } catch (IOException e) {
                    e.printStackTrace();
                    ViseLog.e(e);
                }
            }
        }
    }

    public void sendToTCP(int connectionID, Object object) {
        TcpConnection[] connections = this.connections;
        for (int i = 0, n = connections.length; i < n; i++) {
            TcpConnection connection = connections[i];
            if (connection.id == connectionID) {
                try {
                    connection.send(object);
                } catch (IOException e) {
                    e.printStackTrace();
                    ViseLog.e(e);
                }
                break;
            }
        }
    }

    public void addListener(Listener listener) {
        if(tcp != null){
            tcp.addListener(listener);
        }
        ViseLog.d("Server listener added: " + listener.getClass().getName());
    }

    public void removeListener(Listener listener) {
        if(tcp != null){
            tcp.removeListener(listener);
        }
        ViseLog.d("Server listener removed: " + listener.getClass().getName());
    }

    public void close() {
        if (connections.length > 0) ViseLog.i("Closing server connections...");
        for (int i = 0, n = connections.length; i < n; i++) {
            connections[i].close();
        }
        connections = new TcpConnection[0];
        if (serverChannel != null) {
            try {
                serverChannel.close();
                ViseLog.i("Server closed.");
            } catch (IOException ex) {
                ViseLog.e("Unable to close server." + ex);
            }
            this.serverChannel = null;
        }
        synchronized (updateLock) {
        }
        selector.wakeup();
        try {
            selector.selectNow();
        } catch (IOException ignored) {
        }
    }

    public void dispose() throws IOException {
        close();
        selector.close();
    }

    public Thread getUpdateThread() {
        return updateThread;
    }

    public TcpConnection[] getConnections() {
        return connections;
    }
}
