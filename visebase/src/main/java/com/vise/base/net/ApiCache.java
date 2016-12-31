package com.vise.base.net;

import android.content.Context;

import com.vise.base.cache.DiskCache;
import com.vise.base.net.mode.CacheMode;
import com.vise.base.net.mode.CacheResult;
import com.vise.base.net.strategy.ICacheStrategy;
import com.vise.log.ViseLog;

import java.io.File;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 14:27.
 */
public class ApiCache {
    private final DiskCache diskCache;
    private String cacheKey;

    private static abstract class SimpleSubscribe<T> implements Observable.OnSubscribe<T> {
        @Override
        public final void call(Subscriber<? super T> subscriber) {
            try {
                T data = execute();
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(data);
                }
            } catch (Throwable e) {
                ViseLog.e(e);
                Exceptions.throwIfFatal(e);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(e);
                }
                return;
            }

            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        }

        abstract T execute() throws Throwable;
    }

    private ApiCache(Context context, File diskDir, long diskMaxSize, String cacheKey, long time) {
        this.cacheKey = cacheKey;
        diskCache = DiskCache.getInstance(context, diskDir, diskMaxSize).setCacheTime(time);
    }

    public <T> Observable.Transformer<T, CacheResult<T>> transformer(CacheMode cacheMode) {
        final ICacheStrategy strategy = loadStrategy(cacheMode);//获取缓存策略
        return new Observable.Transformer<T, CacheResult<T>>() {
            @Override
            public Observable<CacheResult<T>> call(Observable<T> apiResultObservable) {
                ViseLog.i("cacheKey=" + ApiCache.this.cacheKey);
                return strategy.execute(ApiCache.this, ApiCache.this.cacheKey, apiResultObservable);
            }
        };
    }


    public <T> Observable<T> get(final String key) {
        return Observable.create(new SimpleSubscribe<T>() {
            @Override
            T execute() {
                return (T) diskCache.get(key);
            }
        });
    }

    public <T> Observable<Boolean> put(final String key, final T value) {
        return Observable.create(new SimpleSubscribe<Boolean>() {
            @Override
            Boolean execute() throws Throwable {
                diskCache.put(key, value);
                return true;
            }
        });
    }

    public boolean containsKey(final String key) {
        return diskCache.contains(key);
    }

    public void remove(final String key) {
        diskCache.remove(key);
    }

    public Observable<Boolean> clear() {
        return Observable.create(new SimpleSubscribe<Boolean>() {
            @Override
            Boolean execute() throws Throwable {
                diskCache.clear();
                return true;
            }
        });
    }

    public ICacheStrategy loadStrategy(CacheMode cacheMode) {
        try {
            String pkName = ICacheStrategy.class.getPackage().getName();
            return (ICacheStrategy) Class.forName(pkName + "." + cacheMode.getClassName())
                    .newInstance();
        } catch (Exception e) {
            throw new RuntimeException("loadStrategy(" + cacheMode + ") err!!" + e.getMessage());
        }
    }

    public static final class Builder {
        private final Context context;
        private final File diskDir;
        private final long diskMaxSize;
        private long cacheTime;
        private String cacheKey;

        public Builder(Context context, File diskDir, long diskMaxSize) {
            this.context = context;
            this.diskDir = diskDir;
            this.diskMaxSize = diskMaxSize;
        }

        public Builder cacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
            return this;
        }

        public Builder cacheTime(long cacheTime) {
            this.cacheTime = cacheTime;
            return this;
        }

        public ApiCache build() {
            return new ApiCache(context, diskDir, diskMaxSize, cacheKey, cacheTime);
        }

    }
}
