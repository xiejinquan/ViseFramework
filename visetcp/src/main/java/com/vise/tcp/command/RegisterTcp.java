package com.vise.tcp.command;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-20 11:16
 */
public class RegisterTcp extends BaseCmd {
    private int connectionID;

    public int getConnectionID() {
        return connectionID;
    }

    public RegisterTcp setConnectionID(int connectionID) {
        this.connectionID = connectionID;
        return this;
    }
}
