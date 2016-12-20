package com.vise.tcp.common;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-20 10:21
 */
public class TcpConfig {

    public static final int KEEP_ALIVE_MILLIS = 8000;

    public static final int TIMEOUT_MILLIS = 12000;

    public static final int IPTOS_LOWDELAY = 0x10;

    public static final int CLIENT_WRITE_BUFFER_SIZE = 8192;
    public static final int CLIENT_READ_BUFFER_SIZE = 2048;
    public static final int SERVER_WRITE_BUFFER_SIZE = 16384;
    public static final int SERVER_READ_BUFFER_SIZE = 2048;
}
