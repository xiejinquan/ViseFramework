package com.vise.base.net.strategy;

import com.vise.base.net.ApiCache;
import com.vise.base.net.mode.CacheResult;

import rx.Observable;
import rx.functions.Func1;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 14:33.
 */
public class CacheAndRemoteStrategy<T> extends CacheStrategy<T> {
    @Override
    public <T> Observable<CacheResult<T>> execute(ApiCache apiCache, String cacheKey, Observable<T> source) {
        Observable<CacheResult<T>> cache = loadCache(apiCache, cacheKey);
        Observable<CacheResult<T>> remote = loadRemote(apiCache, cacheKey, source);
        return Observable.concat(cache, remote)
                .filter(new Func1<CacheResult<T>, Boolean>() {
                    @Override
                    public Boolean call(CacheResult<T> result) {
                        return result.getCacheData() != null;
                    }
                });
    }
}
