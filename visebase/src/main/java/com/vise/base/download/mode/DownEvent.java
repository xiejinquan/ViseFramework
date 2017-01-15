package com.vise.base.download.mode;

/**
 * @Description: 下载事件
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/1/15 17:48.
 */
public class DownEvent {
    private int status = DownStatus.NORMAL.getStatus();
    private DownProgress downProgress = new DownProgress();
    private Throwable error;

    public int getStatus() {
        return status;
    }

    public DownEvent setStatus(int status) {
        this.status = status;
        return this;
    }

    public DownProgress getDownProgress() {
        return downProgress;
    }

    public DownEvent setDownProgress(DownProgress downProgress) {
        this.downProgress = downProgress;
        return this;
    }

    public Throwable getError() {
        return error;
    }

    public DownEvent setError(Throwable error) {
        this.error = error;
        return this;
    }

    @Override
    public String toString() {
        return "DownEvent{" +
                "status=" + status +
                ", downProgress=" + downProgress +
                ", error=" + error +
                '}';
    }
}

