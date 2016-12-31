package com.vise.base.net.strategy;

import com.vise.base.net.ApiCache;
import com.vise.base.net.mode.CacheResult;
import com.vise.log.ViseLog;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 14:28.
 */
public abstract class CacheStrategy<T> implements ICacheStrategy<T> {
    <T> Observable<CacheResult<T>> loadCache(final ApiCache apiCache, final String key) {
        return apiCache
                .<T>get(key)
                .map(new Func1<T, CacheResult<T>>() {
                    @Override
                    public CacheResult<T> call(T t) {
                        ViseLog.i("loadCache result=" + t);
                        return new CacheResult<>(true, t);
                    }
                });
    }

    <T> Observable<CacheResult<T>> loadRemote(final ApiCache apiCache, final String key, Observable<T> source) {
        return source
                .map(new Func1<T, CacheResult<T>>() {
                     @Override
                     public CacheResult<T> call(T t) {
                         ViseLog.i("loadRemote result=" + t);
                         apiCache.put(key, t).subscribeOn(Schedulers.io())
                                 .subscribe(new Action1<Boolean>() {
                                     @Override
                                     public void call(Boolean status) {
                                         ViseLog.i("save status => " + status);
                                     }
                                 });
                         return new CacheResult<>(false, t);
                     }
                });
    }
}
