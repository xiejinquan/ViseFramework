package com.vise.tcp;

import com.vise.log.ViseLog;
import com.vise.tcp.command.BaseCmd;
import com.vise.tcp.command.KeepAlive;
import com.vise.tcp.command.RegisterTcp;
import com.vise.tcp.common.TcpConfig;
import com.vise.tcp.inter.IDataDispose;
import com.vise.tcp.inter.IThread;
import com.vise.tcp.listener.Listener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.AccessControlException;
import java.util.Iterator;
import java.util.Set;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-20 10:18
 */
public class TcpClient implements IThread {
    private TcpConnection tcp;
    private final IDataDispose dataDispose;
    private Selector selector;
    private int emptySelects;
    private volatile boolean tcpRegistered;
    private Object tcpRegistrationLock = new Object();
    private volatile boolean shutdown;
    private final Object updateLock = new Object();
    private Thread updateThread;
    private int connectTimeout;
    private InetAddress connectHost;
    private int connectTcpPort;
    private boolean isClosed;

    static {
        try {
            // Needed for NIO selectors on Android 2.2.
            System.setProperty("java.net.preferIPv6Addresses", "false");
        } catch (AccessControlException ignored) {
        }
    }

    public TcpClient () {
        this(TcpConfig.CLIENT_WRITE_BUFFER_SIZE, TcpConfig.CLIENT_READ_BUFFER_SIZE);
    }

    public TcpClient (int writeBufferSize, int objectBufferSize) {
        this(writeBufferSize, objectBufferSize, IDataDispose.DEFAULT);
    }

    public TcpClient (int writeBufferSize, int objectBufferSize, IDataDispose dataDispose) {
        super();
        this.dataDispose = dataDispose;
        this.tcp = new TcpConnection(dataDispose, writeBufferSize, objectBufferSize);
        try {
            selector = Selector.open();
        } catch (IOException ex) {
            throw new RuntimeException("Error opening selector.", ex);
        }
    }

    public IDataDispose getDataDispose () {
        return dataDispose;
    }

    public void connect (int timeout, String host, int tcpPort) throws IOException {
        connect(timeout, InetAddress.getByName(host), tcpPort);
    }

    public void connect (int timeout, InetAddress host, int tcpPort) throws IOException {
        if (host == null) throw new IllegalArgumentException("host cannot be null.");
        if (Thread.currentThread() == getUpdateThread())
            throw new IllegalStateException("Cannot connect on the connection's update thread.");
        this.connectTimeout = timeout;
        this.connectHost = host;
        this.connectTcpPort = tcpPort;
        close();
        ViseLog.i("Connecting: " + host + ":" + tcpPort);
        tcp.id = -1;
        try {
            long endTime;
            synchronized (updateLock) {
                tcpRegistered = false;
                selector.wakeup();
                endTime = System.currentTimeMillis() + timeout;
                tcp.connect(selector, new InetSocketAddress(host, tcpPort), timeout);
            }

            // Wait for RegisterTCP.
            synchronized (tcpRegistrationLock) {
                while (!tcpRegistered && System.currentTimeMillis() < endTime) {
                    try {
                        tcpRegistrationLock.wait(100);
                    } catch (InterruptedException ignored) {
                    }
                }
                if (!tcpRegistered) {
                    throw new SocketTimeoutException("Connected, but timed out during TCP registration.\n"
                            + "Note: Client#update must be called in a separate thread during connect.");
                }
            }

        } catch (IOException ex) {
            close();
            throw ex;
        }
    }

    public void reconnect () throws IOException {
        reconnect(connectTimeout);
    }

    public void reconnect (int timeout) throws IOException {
        if (connectHost == null) throw new IllegalStateException("This client has never been connected.");
        connect(timeout, connectHost, connectTcpPort);
    }

    public void update (int timeout) throws IOException {
        updateThread = Thread.currentThread();
        synchronized (updateLock) { // Blocks to avoid a select while the selector is used to bind the server connection.
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
                // NIO freaks and returns immediately with 0 sometimes, so try to keep from hogging the CPU.
                long elapsedTime = System.currentTimeMillis() - startTime;
                try {
                    if (elapsedTime < 25) Thread.sleep(25 - elapsedTime);
                } catch (InterruptedException ex) {
                }
            }
        } else {
            emptySelects = 0;
            isClosed = false;
            Set<SelectionKey> keys = selector.selectedKeys();
            synchronized (keys) {
                for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();) {
                    keepAlive();
                    SelectionKey selectionKey = iter.next();
                    iter.remove();
                    try {
                        int ops = selectionKey.readyOps();
                        if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                            while (true) {
                                Object object = tcp.readObject();
                                if (object == null) break;
                                if (!tcpRegistered) {
                                    if (object instanceof RegisterTcp) {
                                        tcp.id = ((RegisterTcp)object).getConnectionID();
                                        synchronized (tcpRegistrationLock) {
                                            tcpRegistered = true;
                                            tcpRegistrationLock.notifyAll();
                                            ViseLog.d(this + " received TCP: RegisterTCP");
                                            tcp.setConnected(true);
                                        }
                                        tcp.notifyConnected();
                                    }
                                    continue;
                                }
                                if (!tcp.isConnected()) continue;
                                String objectString = object == null ? "null" : object.getClass().getSimpleName();
                                if (!(object instanceof BaseCmd)) {
                                    ViseLog.d(this + " received TCP: " + objectString);
                                }
                                tcp.notifyReceived(object);
                            }
                        }
                        if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) tcp.writeOperation();
                    } catch (CancelledKeyException ignored) {
                        // Connection is closed.
                    }
                }
            }
        }
        if (tcp.isConnected()) {
            long time = System.currentTimeMillis();
            if (tcp.isTimedOut(time)) {
                ViseLog.d(this + " timed out.");
                close();
            } else
                keepAlive();
            if (tcp.isIdle()) tcp.notifyIdle();
        }
    }

    protected void keepAlive () throws IOException {
        if (!tcp.isConnected()) return;
        long time = System.currentTimeMillis();
        if (tcp.needsKeepAlive(time)) tcp.send(new KeepAlive());
    }

    public void run () {
        ViseLog.d("Client thread started.");
        shutdown = false;
        while (!shutdown) {
            try {
                update(250);
            } catch (IOException ex) {
                if (tcp.isConnected()) {
                    ViseLog.d(this + " update: " + ex.getMessage());
                } else {
                    ViseLog.d("Unable to update connection: " + ex.getMessage());
                }
                close();
            }
        }
        ViseLog.d("Client thread stopped.");
    }

    public void start () {
        if (updateThread != null) {
            shutdown = true;
            try {
                updateThread.join(5000);
            } catch (InterruptedException ignored) {
            }
        }
        updateThread = new Thread(this, "Client");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    public void stop () {
        if (shutdown) return;
        close();
        ViseLog.d("Client thread stopping.");
        shutdown = true;
        selector.wakeup();
    }

    public void close () {
        if(tcp != null){
            tcp.close();
        }
        synchronized (updateLock) {
        }
        if (!isClosed) {
            isClosed = true;
            selector.wakeup();
            try {
                selector.selectNow();
            } catch (IOException ignored) {
            }
        }
    }

    public void dispose () throws IOException {
        close();
        selector.close();
    }

    public void addListener (Listener listener) {
        if(tcp != null){
            tcp.addListener(listener);
        }
        ViseLog.d("Client listener added.");
    }

    public void removeListener (Listener listener) {
        if(tcp != null){
            tcp.removeListener(listener);
        }
        ViseLog.d("Client listener removed.");
    }

    public TcpConnection getTcp() {
        return tcp;
    }

    public Thread getUpdateThread () {
        return updateThread;
    }
}
