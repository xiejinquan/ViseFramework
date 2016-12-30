package com.vise.base.net.convert;

import com.vise.base.net.ApiCode;
import com.vise.base.net.ApiResult;
import com.vise.base.net.exception.ApiException;

import rx.functions.Func1;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-30 17:55
 */
public class ToData<T> implements Func1<T, T> {
    public ToData(){
    }
    @Override
    public T call(T result) {
        if(result instanceof ApiResult){
            ApiResult value= (ApiResult)result;
            return (T) value.getData();
        }else{
            java.lang.Throwable throwable = new java.lang.Throwable("Please call(Observable<T> observable) , < T > is not ApiResult object");
            new ApiException(throwable, ApiCode.INVOKE_ERROR);
            return (T)result;
        }
    }
}
