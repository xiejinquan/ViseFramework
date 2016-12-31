package com.vise.base.net.strategy;

import com.vise.base.net.ApiCache;
import com.vise.base.net.mode.CacheResult;

import rx.Observable;
import rx.functions.Func1;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 14:31.
 */
public class FirstCacheStrategy<T> extends CacheStrategy<T> {
    @Override
    public <T> Observable<CacheResult<T>> execute(ApiCache apiCache, String cacheKey, Observable<T> source) {
        Observable<CacheResult<T>> cache = loadCache(apiCache, cacheKey);
        cache.onErrorReturn(new Func1<Throwable, CacheResult<T>>() {
            @Override
            public CacheResult<T> call(Throwable throwable) {
                return null;
            }
        });
        Observable<CacheResult<T>> remote = loadRemote(apiCache, cacheKey, source);
        return Observable.concat(cache, remote)
                .firstOrDefault(null, new Func1<CacheResult<T>, Boolean>() {
                    @Override
                    public Boolean call(CacheResult<T> tResultData) {
                        return tResultData != null && tResultData.getCacheData() != null;
                    }
                });
    }
}
