package com.vise.tcp.command;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-19 19:37
 */
public class Ping extends BaseCmd {
    private int id;
    private boolean isReply;//是否答复

    public int getId() {
        return id;
    }

    public Ping setId(int id) {
        this.id = id;
        return this;
    }

    public boolean isReply() {
        return isReply;
    }

    public Ping setReply(boolean reply) {
        isReply = reply;
        return this;
    }
}
