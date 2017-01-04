package com.vise.base.net.subscriber;

import android.accounts.NetworkErrorException;
import android.content.Context;

import com.vise.base.net.exception.ApiException;
import com.vise.base.net.mode.ApiCode;
import com.vise.log.ViseLog;
import com.vise.utils.assist.Network;

import java.lang.ref.WeakReference;

import rx.Subscriber;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-01-03 14:07
 */
public abstract class ApiSubscriber<T> extends Subscriber<T> {
    public WeakReference<Context> contextWeakReference;

    public ApiSubscriber(Context context) {
        contextWeakReference = new WeakReference<Context>(context);
    }

    @Override
    public void onError(Throwable e) {
        if (e instanceof ApiException) {
            ViseLog.e("e instanceof ApiException" + e.getMessage());
            onError((ApiException) e);
        } else {
            ViseLog.e("e !instanceof ApiException" + e.getMessage());
            onError(new ApiException(e, ApiCode.CONVERT_ERROR));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!Network.isConnected(contextWeakReference.get())) {
            onCompleted();
        } else {
            onError(new ApiException(new NetworkErrorException(), ApiCode.NETWORK_ERROR));
        }
    }

    public abstract void onError(ApiException e);
}
