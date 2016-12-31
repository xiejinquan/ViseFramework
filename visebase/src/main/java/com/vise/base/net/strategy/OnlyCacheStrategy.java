package com.vise.base.net.strategy;

import com.vise.base.net.ApiCache;
import com.vise.base.net.mode.CacheResult;

import rx.Observable;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 14:29.
 */
public class OnlyCacheStrategy<T> extends CacheStrategy<T> {
    @Override
    public <T> Observable<CacheResult<T>> execute(ApiCache apiCache, String cacheKey, Observable<T> source) {
        return loadCache(apiCache, cacheKey);
    }
}
