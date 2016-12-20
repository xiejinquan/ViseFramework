package com.vise.udp;

import com.vise.log.ViseLog;
import com.vise.udp.command.BaseCmd;
import com.vise.udp.command.DiscoverHost;
import com.vise.udp.inter.IDataDispose;
import com.vise.udp.inter.IThread;
import com.vise.udp.inter.ServerDiscoveryHandler;
import com.vise.udp.listener.Listener;

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
 * @date: 2016-12-19 19:16
 */
public class UdpServer implements IThread {
    private final IDataDispose dataDispose;
    private final int writeBufferSize, objectBufferSize;
    private final Selector selector;
    private int emptySelects;
    private ServerSocketChannel serverChannel;
    private UdpConnection udp;
    private UdpConnection[] connections = {};
    private ArrayList<UdpConnection> pendingConnections = new ArrayList<>();
    private Listener[] listeners = {};
    private Object listenerLock = new Object();
    private int nextConnectionID = 1;
    private volatile boolean shutdown;
    private Object updateLock = new Object();
    private Thread updateThread;
    private ServerDiscoveryHandler discoveryHandler;

    /*private Listener dispatchListener = new Listener() {
        public void connected (UdpConnection connection) {
            Listener[] listeners = UdpServer.this.listeners;
            for (int i = 0, n = listeners.length; i < n; i++)
                listeners[i].connected(connection);
        }

        public void disconnected (UdpConnection connection) {
            removeConnection(connection);
            Listener[] listeners = UdpServer.this.listeners;
            for (int i = 0, n = listeners.length; i < n; i++)
                listeners[i].disconnected(connection);
        }

        public void received (UdpConnection connection, Object object) {
            Listener[] listeners = UdpServer.this.listeners;
            for (int i = 0, n = listeners.length; i < n; i++)
                listeners[i].received(connection, object);
        }

        public void idle (UdpConnection connection) {
            Listener[] listeners = UdpServer.this.listeners;
            for (int i = 0, n = listeners.length; i < n; i++)
                listeners[i].idle(connection);
        }
    };*/

    public UdpServer () {
        this(16384, 2048);
    }

    public UdpServer (int writeBufferSize, int objectBufferSize) {
        this(writeBufferSize, objectBufferSize, IDataDispose.DEFAULT);
    }

    public UdpServer (int writeBufferSize, int objectBufferSize, IDataDispose dataDispose) {
        this.writeBufferSize = writeBufferSize;
        this.objectBufferSize = objectBufferSize;
        this.dataDispose = dataDispose;
        this.discoveryHandler = ServerDiscoveryHandler.DEFAULT;
        try {
            selector = Selector.open();
        } catch (IOException ex) {
            throw new RuntimeException("Error opening selector.", ex);
        }
    }

    public void setDiscoveryHandler (ServerDiscoveryHandler newDiscoveryHandler) {
        discoveryHandler = newDiscoveryHandler;
    }

    public IDataDispose getDataDispose () {
        return dataDispose;
    }

    public void bind (InetSocketAddress udpPort) throws IOException {
        close();
        synchronized (updateLock) {
            selector.wakeup();
            try {
                if (udpPort != null) {
                    udp = new UdpConnection(dataDispose, objectBufferSize);
                    udp.bind(selector, udpPort);
                    ViseLog.d("Accepting connections on port: " + udpPort + "/UDP");
                }
            } catch (IOException ex) {
                close();
                throw ex;
            }
        }
        ViseLog.i("Server opened.");
    }

    public void update (int timeout) throws IOException {
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
                UdpConnection udp = this.udp;
                outer:
                for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();) {
                    keepAlive();
                    SelectionKey selectionKey = iter.next();
                    iter.remove();
                    UdpConnection fromConnection = (UdpConnection)selectionKey.attachment();
                    try {
                        int ops = selectionKey.readyOps();
                        if (fromConnection != null) {
                            if (udp != null && fromConnection.getConnectedAddress() == null) {
                                fromConnection.close();
                                continue;
                            }
                        }
                        if (udp == null) {
                            selectionKey.channel().close();
                            continue;
                        }
                        InetSocketAddress fromAddress;
                        try {
                            fromAddress = udp.readFromAddress();
                        } catch (IOException ex) {
                            ViseLog.e("Error reading UDP data.", ex);
                            continue;
                        }
                        if (fromAddress == null) continue;
                        UdpConnection[] connections = this.connections;
                        for (int i = 0, n = connections.length; i < n; i++) {
                            UdpConnection connection = connections[i];
                            if (fromAddress.equals(connection.getConnectedAddress())) {
                                fromConnection = connection;
                                break;
                            }
                        }
                        Object object;
                        try {
                            object = udp.readObject();
                        } catch (IOException ex) {
                            if (fromConnection != null) {
                                ViseLog.e("Error reading UDP from connection: " + fromConnection, ex);
                            } else {
                                ViseLog.e("Error reading UDP from unregistered address: " + fromAddress, ex);
                            }
                            continue;
                        }
                        if (object instanceof BaseCmd) {
                            if (object instanceof DiscoverHost) {
                                try {
                                    boolean responseSent = discoveryHandler
                                            .onDiscoverHost(udp.getDatagramChannel(), fromAddress, dataDispose);
                                    if (responseSent) ViseLog.d("Responded to host discovery from: " + fromAddress);
                                } catch (IOException ex) {
                                    ViseLog.e("Error replying to host discovery from: " + fromAddress, ex);
                                }
                                continue;
                            }
                        }

                        ViseLog.d("Ignoring UDP from unregistered address: " + fromAddress);
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
    }

    private void keepAlive () {
        long time = System.currentTimeMillis();
        UdpConnection[] connections = this.connections;
        for (int i = 0, n = connections.length; i < n; i++) {
            UdpConnection connection = connections[i];
//            if (connection.needsKeepAlive(time)) connection.send();
        }
    }

    public void run () {
        ViseLog.d("Server thread started.");
        shutdown = false;
        while (!shutdown) {
            try {
                update(250);
            } catch (IOException ex) {
                ViseLog.e("Error updating server connections.", ex);
                close();
            }
        }
        ViseLog.d("Server thread stopped.");
    }

    public void start () {
        new Thread(this, "Server").start();
    }

    public void stop () {
        if (shutdown) return;
        close();
        ViseLog.d("Server thread stopping.");
        shutdown = true;
    }

    private void addConnection (UdpConnection connection) {
        UdpConnection[] newConnections = new UdpConnection[connections.length + 1];
        newConnections[0] = connection;
        System.arraycopy(connections, 0, newConnections, 1, connections.length);
        connections = newConnections;
    }

    public void removeConnection (UdpConnection connection) {
        ArrayList<UdpConnection> temp = new ArrayList(Arrays.asList(connections));
        temp.remove(connection);
        connections = temp.toArray(new UdpConnection[temp.size()]);
        pendingConnections.remove(connection.id);
    }

    public void addListener (Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
        synchronized (listenerLock) {
            Listener[] listeners = this.listeners;
            int n = listeners.length;
            for (int i = 0; i < n; i++)
                if (listener == listeners[i]) return;
            Listener[] newListeners = new Listener[n + 1];
            newListeners[0] = listener;
            System.arraycopy(listeners, 0, newListeners, 1, n);
            this.listeners = newListeners;
        }
        ViseLog.d("Server listener added: " + listener.getClass().getName());
    }

    public void removeListener (Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
        synchronized (listenerLock) {
            Listener[] listeners = this.listeners;
            int n = listeners.length;
            Listener[] newListeners = new Listener[n - 1];
            for (int i = 0, ii = 0; i < n; i++) {
                Listener copyListener = listeners[i];
                if (listener == copyListener) continue;
                if (ii == n - 1) return;
                newListeners[ii++] = copyListener;
            }
            this.listeners = newListeners;
        }
        ViseLog.d("Server listener removed: " + listener.getClass().getName());
    }

    public void close () {
        UdpConnection[] connections = this.connections;
        if (connections.length > 0) ViseLog.i("Closing server connections...");
        for (int i = 0, n = connections.length; i < n; i++) {
            connections[i].close();
        }
        connections = new UdpConnection[0];
        ServerSocketChannel serverChannel = this.serverChannel;
        if (serverChannel != null) {
            try {
                serverChannel.close();
                ViseLog.i("Server closed.");
            } catch (IOException ex) {
                ViseLog.e("Unable to close server.", ex);
            }
            this.serverChannel = null;
        }
        UdpConnection udp = this.udp;
        if (udp != null) {
            udp.close();
            this.udp = null;
        }
        synchronized (updateLock) {
        }
        selector.wakeup();
        try {
            selector.selectNow();
        } catch (IOException ignored) {
        }
    }

    public void dispose () throws IOException {
        close();
        selector.close();
    }

    public Thread getUpdateThread () {
        return updateThread;
    }

    public UdpConnection[] getConnections () {
        return connections;
    }
}
