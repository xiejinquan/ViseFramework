package com.vise.base.download.mode;

/**
 * @Description: 下载范围
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/1/15 17:46.
 */
public class DownRange {
    private long start;
    private long end;

    public DownRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public DownRange setStart(long start) {
        this.start = start;
        return this;
    }

    public long getEnd() {
        return end;
    }

    public DownRange setEnd(long end) {
        this.end = end;
        return this;
    }

    @Override
    public String toString() {
        return "DownRange{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
