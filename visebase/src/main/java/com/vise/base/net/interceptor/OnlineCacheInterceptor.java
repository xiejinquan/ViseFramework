package com.vise.base.net.interceptor;

import android.content.Context;
import android.text.TextUtils;

import com.vise.log.ViseLog;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 22:23.
 */
public class OnlineCacheInterceptor implements Interceptor {
    private static final int MAX_AGE_ONLINE = 60;
    private String cacheControlValue;

    public OnlineCacheInterceptor() {
        this(MAX_AGE_ONLINE);
    }

    public OnlineCacheInterceptor(int cacheControlValue) {
        this.cacheControlValue = String.format("max-age=%d", cacheControlValue);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        okhttp3.Response originalResponse = chain.proceed(chain.request());
        String cacheControl = originalResponse.header("Cache-Control");
        ViseLog.i(cacheControlValue + "s load cache:" + cacheControl);
        if (TextUtils.isEmpty(cacheControl) || cacheControl.contains("no-store") || cacheControl
                .contains("no-cache") || cacheControl.contains("must-revalidate") || cacheControl
                .contains("max-age") || cacheControl.contains("max-stale")) {
            return originalResponse.newBuilder()
                    .removeHeader("Pragma")
                    .header("Cache-Control", "public, " + cacheControlValue)
                    .build();

        } else {
            return originalResponse;
        }
    }
}
