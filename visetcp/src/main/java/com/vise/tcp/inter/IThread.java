package com.vise.tcp.inter;

import com.vise.tcp.listener.Listener;

import java.io.IOException;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2016-12-19 19:39
 */
public interface IThread extends Runnable {
    IDataDispose getDataDispose();

    void addListener(Listener listener);

    void removeListener(Listener listener);

    void run();

    void start();

    void stop();

    void close();

    void update(int timeout) throws IOException;

    Thread getUpdateThread();
}
