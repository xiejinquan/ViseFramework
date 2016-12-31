package com.vise.base.net.func;

import com.vise.base.net.mode.ApiCode;
import com.vise.base.net.mode.ApiResult;
import com.vise.base.net.exception.ApiException;

import rx.functions.Func1;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-30 17:55
 */
public class DataFunc<T> implements Func1<T, T> {
    public DataFunc() {
    }

    @Override
    public T call(T result) {
        if (result instanceof ApiResult) {
            ApiResult value = (ApiResult) result;
            return (T) value.getData();
        } else {
            java.lang.Throwable throwable = new java.lang.Throwable("Please call(Observable<T> " +
                    "observable) , < T > is not ApiResult object");
            new ApiException(throwable, ApiCode.INVOKE_ERROR);
            return (T) result;
        }
    }
}
