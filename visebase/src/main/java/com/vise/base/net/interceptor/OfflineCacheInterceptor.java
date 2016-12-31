package com.vise.base.net.interceptor;

import android.content.Context;

import com.vise.log.ViseLog;
import com.vise.utils.assist.Network;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 22:36.
 */
public class OfflineCacheInterceptor implements Interceptor {
    private static final int MAX_AGE_OFFLINE = 3 * 60 * 60;
    private Context context;
    private String cacheControlValue;

    public OfflineCacheInterceptor(Context context) {
        this(context, MAX_AGE_OFFLINE);
    }

    public OfflineCacheInterceptor(Context context, int cacheControlValue) {
        this.context = context;
        this.cacheControlValue = String.format("max-age=%d", cacheControlValue);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (!Network.isConnected(context)) {
            ViseLog.i(" no network load cache:"+ request.cacheControl().toString());
            request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build();
            Response response = chain.proceed(request);
            return response.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, only-if-cached, " + cacheControlValue)
                    .build();
        }
        return chain.proceed(request);
    }
}
