package com.vise.tcp.listener;

import com.vise.tcp.TcpConnection;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-20 10:20
 */
public class Listener {
    public void connected (TcpConnection connection) {
    }

    public void disconnected (TcpConnection connection) {
    }

    public void received (TcpConnection connection, Object object) {
    }

    public void idle (TcpConnection connection) {
    }
}
