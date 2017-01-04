package com.vise.base.net.callback;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017-01-03 14:18
 */
public interface IDownCallback {
    void onStart();

    void update(long bytesRead, long contentLength, boolean done);

    void onFailed(Throwable throwable);

    void onComplete(String path);
}
