package com.vise.base.net;

import android.content.Context;

import com.vise.base.net.exception.ApiException;
import com.vise.base.net.func.ApiDataFunc;
import com.vise.base.net.func.ApiErrFunc;
import com.vise.base.net.func.ApiResultFunc;
import com.vise.base.net.inter.ApiService;
import com.vise.base.net.inter.INet;
import com.vise.base.net.interceptor.GzipRequestInterceptor;
import com.vise.base.net.interceptor.HeadersInterceptor;
import com.vise.base.net.interceptor.OfflineCacheInterceptor;
import com.vise.base.net.interceptor.OnlineCacheInterceptor;
import com.vise.base.net.mode.ApiCode;
import com.vise.base.net.mode.ApiResult;
import com.vise.base.net.mode.CacheMode;
import com.vise.base.net.mode.CacheResult;
import com.vise.log.ViseLog;

import java.io.File;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.FieldMap;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-30 16:48
 */
public class ViseNet implements INet {
    private static Context context;
    private static ApiService apiService;
    private static Map<String, String> headers;
    private static Map<String, String> parameters;
    private static Retrofit retrofit;
    private static Retrofit.Builder retrofitBuilder;
    private static OkHttpClient okHttpClient;
    private static OkHttpClient.Builder okHttpBuilder;
    private static ApiCache apiCache;
    private static ApiCache.Builder apiCacheBuilder;

    private final okhttp3.Call.Factory callFactory;
    private final List<Converter.Factory> converterFactories;
    private final List<CallAdapter.Factory> adapterFactories;
    private final Executor callbackExecutor;
    private final boolean validateEagerly;
    private CacheMode cacheMode = CacheMode.ONLY_REMOTE;

    private ViseNet(okhttp3.Call.Factory callFactory, Map<String, String> headers, Map<String, String> parameters,
                    ApiService apiService, List<Converter.Factory> converterFactories, List<CallAdapter.Factory>
                            adapterFactories, Executor callbackExecutor, boolean validateEagerly, CacheMode cacheMode) {
        this.callFactory = callFactory;
        this.headers = headers;
        this.parameters = parameters;
        this.apiService = apiService;
        this.converterFactories = converterFactories;
        this.adapterFactories = adapterFactories;
        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
        this.cacheMode = cacheMode;
    }

    public <T> T create(final Class<T> service) {
        return retrofit.create(service);
    }

    public <T> Observable<T> call(Observable<T> observable) {
        return observable.compose(new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> tObservable) {
                return tObservable
                        .subscribeOn(Schedulers.io())
                        .unsubscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorResumeNext(new ApiErrFunc<T>());
            }
        });
    }

    public <T> Observable<T> apiCall(Observable<T> observable) {
        return observable.map(new Func1<T, T>() {
            @Override
            public T call(T result) {
                if(result instanceof ApiResult){
                    ApiResult value= (ApiResult)result;
                    return (T) value.getData();
                }else{
                    Throwable throwable = new Throwable("Please call(Observable<T> observable) , < T > is not ApiResult object");
                    new ApiException(throwable, ApiCode.INVOKE_ERROR);
                    return (T)result;
                }
            }
        }).compose(new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> tObservable) {
                return tObservable
                        .subscribeOn(Schedulers.io())
                        .unsubscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorResumeNext(new ApiErrFunc<T>());
            }
        });
    }

    public <T> Observable<T> get(String url, Map<String, String> maps) {
        return apiService.get(url, maps).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> post(final String url, final Map<String, String> parameters) {
        return apiService.post(url, parameters).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> form(final String url, final @FieldMap(encoded = true) Map<String, Object> fields) {
        return apiService.postForm(url, fields).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> body(final String url, final Object body) {
        return apiService.postBody(url, body).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> delete(final String url, final Map<String, String> maps) {
        return apiService.delete(url, maps).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> put(final String url, final Map<String, String> maps) {
        return apiService.put(url, maps).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> uploadImage(String url, RequestBody requestBody) {
        return apiService.uploadImage(url, requestBody).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> uploadImage(String url, File file) {
        return apiService.uploadImage(url, RequestBody.create(okhttp3.MediaType.parse("image/jpg; " +
                "charset=utf-8"), file)).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> uploadFile(String url, RequestBody requestBody, MultipartBody.Part file) {
        return apiService.uploadFile(url, requestBody, file).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> uploadFlies(String url, Map<String, RequestBody> files) {
        return apiService.uploadFiles(url, files).compose(this.<T>norTransformer());
    }

    public <T> Observable<T> apiGet(final String url, final Map<String, String> maps, Class<T> clazz) {
        return apiService.get(url, maps).map(new ApiResultFunc<T>(clazz)).compose(this.<T>apiTransformer());
    }

    public <T> Observable<CacheResult<T>> apiCacheGet(final String url, final Map<String, String> maps, Class<T>
            clazz) {
        return this.apiGet(url, maps, clazz).compose(apiCache.<T>transformer(cacheMode));
    }

    public <T> Observable<T> apiPost(final String url, final Map<String, String> parameters, Class<T> clazz) {
        return apiService.post(url, parameters).map(new ApiResultFunc<T>(clazz)).compose(this.<T>apiTransformer());
    }

    public <T> Observable<CacheResult<T>> apiCachePost(final String url, final Map<String, String> parameters,
                                                       Class<T> clazz) {
        return this.apiPost(url, parameters, clazz).compose(apiCache.<T>transformer(cacheMode));
    }

    public Observable<Boolean> clearCache() {
        return apiCache.clear();
    }

    public void removeCache(String key) {
        apiCache.remove(key);
    }

    public Builder newBuilder(Context context) {
        return new Builder(context);
    }

    private <T> Observable.Transformer<ResponseBody, T> norTransformer() {
        return new Observable.Transformer<ResponseBody, T>() {
            @Override
            public Observable<T> call(Observable<ResponseBody> apiResultObservable) {
                return apiResultObservable
                        .subscribeOn(Schedulers.io())
                        .unsubscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(new Func1<ResponseBody, T>() {
                            @Override
                            public T call(ResponseBody responseBody) {
                                return (T) responseBody;
                            }
                        })
                        .onErrorResumeNext(new ApiErrFunc<T>());
            }
        };
    }

    private <T> Observable.Transformer<ApiResult<T>, T> apiTransformer() {
        return new Observable.Transformer<ApiResult<T>, T>() {
            @Override
            public Observable<T> call(Observable<ApiResult<T>> apiResultObservable) {
                return apiResultObservable
                        .subscribeOn(Schedulers.io())
                        .unsubscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .map(new ApiDataFunc<T>())
                        .onErrorResumeNext(new ApiErrFunc<T>());
            }
        };
    }

    private static <T> T checkNotNull(T t, String message) {
        if (t == null) {
            throw new NullPointerException(message);
        }
        return t;
    }

    public static final class Builder {
        private static final int DEFAULT_TIMEOUT = 60;
        private static final int DEFAULT_MAX_IDLE_CONNECTIONS = 5;
        private static final long DEFAULT_KEEP_ALIVE_DURATION = 8;
        private static final long CACHE_MAX_SIZE = 10 * 1024 * 1024;
        private okhttp3.Call.Factory callFactory;
        private Boolean isCookie = false;
        private Boolean isCache = true;
        private HostnameVerifier hostnameVerifier;
        private List<Converter.Factory> converterFactories = new ArrayList<>();
        private List<CallAdapter.Factory> adapterFactories = new ArrayList<>();
        private Executor callbackExecutor;
        private boolean validateEagerly;
        private Context mContext;
        private ApiCookie apiCookie;
        private Cache cache;
        private Proxy proxy;
        private File httpCacheDirectory;
        private SSLSocketFactory sslSocketFactory;
        private ConnectionPool connectionPool;
        private Converter.Factory converterFactory;
        private CallAdapter.Factory callAdapterFactory;
        private Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR;
        private Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR_OFFLINE;
        private CacheMode cacheMode;
        private String baseUrl;

        public Builder(Context context) {
            this.mContext = context;
            okHttpBuilder = new OkHttpClient.Builder();
            retrofitBuilder = new Retrofit.Builder();
            apiCacheBuilder = new ApiCache.Builder(context);
        }

        public Builder client(OkHttpClient client) {
            retrofitBuilder.client(checkNotNull(client, "client == null"));
            return this;
        }

        public Builder callFactory(okhttp3.Call.Factory factory) {
            this.callFactory = checkNotNull(factory, "factory == null");
            return this;
        }

        public Builder connectTimeout(int timeout) {
            return connectTimeout(timeout, TimeUnit.SECONDS);
        }

        public Builder readTimeout(int timeout) {
            return readTimeout(timeout, TimeUnit.SECONDS);
        }

        public Builder writeTimeout(int timeout) {
            return writeTimeout(timeout, TimeUnit.SECONDS);
        }

        public Builder cookie(boolean isCookie) {
            this.isCookie = isCookie;
            return this;
        }

        public Builder cache(boolean isCache) {
            this.isCache = isCache;
            return this;
        }

        public Builder proxy(Proxy proxy) {
            okHttpBuilder.proxy(checkNotNull(proxy, "proxy == null"));
            return this;
        }

        public Builder writeTimeout(int timeout, TimeUnit unit) {
            if (timeout != -1) {
                okHttpBuilder.writeTimeout(timeout, unit);
            } else {
                okHttpBuilder.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            }
            return this;
        }

        public Builder connectionPool(ConnectionPool connectionPool) {
            if (connectionPool == null) throw new NullPointerException("connectionPool == null");
            this.connectionPool = connectionPool;
            return this;
        }

        public Builder connectTimeout(int timeout, TimeUnit unit) {
            if (timeout != -1) {
                okHttpBuilder.connectTimeout(timeout, unit);
            } else {
                okHttpBuilder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            }
            return this;
        }

        public Builder readTimeout(int timeout, TimeUnit unit) {
            if (timeout != -1) {
                okHttpBuilder.readTimeout(timeout, unit);
            } else {
                okHttpBuilder.readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            }
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = checkNotNull(baseUrl, "baseUrl == null");
            return this;
        }

        public Builder converterFactory(Converter.Factory factory) {
            this.converterFactory = factory;
            return this;
        }

        public Builder callAdapterFactory(CallAdapter.Factory factory) {
            this.callAdapterFactory = factory;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            okHttpBuilder.addInterceptor(new HeadersInterceptor(headers));
            return this;
        }

        public Builder parameters(Map<String, String> parameters) {
            okHttpBuilder.addInterceptor(new HeadersInterceptor(parameters));
            return this;
        }

        public Builder interceptor(Interceptor interceptor) {
            okHttpBuilder.addInterceptor(checkNotNull(interceptor, "interceptor == null"));
            return this;
        }

        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = checkNotNull(executor, "executor == null");
            return this;
        }

        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }

        public Builder cookieManager(ApiCookie cookie) {
            if (cookie == null) throw new NullPointerException("cookieManager == null");
            this.apiCookie = cookie;
            return this;
        }


        public Builder SSLSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
            return this;
        }

        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public Builder networkInterceptor(Interceptor interceptor) {
            okHttpBuilder.addNetworkInterceptor(interceptor);
            return this;
        }

        public Builder postGzipInterceptor() {
            interceptor(new GzipRequestInterceptor());
            return this;
        }

        public Builder cacheKey(String cacheKey) {
            apiCacheBuilder.cacheKey(checkNotNull(cacheKey, "cacheKey == null"));
            return this;
        }

        public Builder cacheTime(long cacheTime) {
            apiCacheBuilder.cacheTime(Math.max(-1, cacheTime));
            return this;
        }


        public Builder cacheMode(CacheMode cacheMode) {
            this.cacheMode = cacheMode;
            return this;
        }

        public Builder cacheOnline(Cache cache) {
            networkInterceptor(new OnlineCacheInterceptor());
            this.cache = cache;
            return this;
        }

        public Builder cacheOnline(Cache cache, final int cacheControlValue) {
            networkInterceptor(new OnlineCacheInterceptor(cacheControlValue));
            this.cache = cache;
            return this;
        }

        public Builder cacheOffline(Cache cache) {
            interceptor(new OfflineCacheInterceptor(mContext));
            this.cache = cache;
            return this;
        }

        public Builder cacheOffline(Cache cache, final int cacheControlValue) {
            interceptor(new OfflineCacheInterceptor(mContext, cacheControlValue));
            this.cache = cache;
            return this;
        }

        public ViseNet build() {
            context = mContext;
            if (baseUrl == null) {
                throw new IllegalStateException("Base URL required.");
            }

            if (okHttpBuilder == null) {
                throw new IllegalStateException("okHttpBuilder required.");
            }

            if (retrofitBuilder == null) {
                throw new IllegalStateException("retrofitBuilder required.");
            }

            if (apiCacheBuilder == null) {
                throw new IllegalStateException("apiCacheBuilder required.");
            }

            retrofitBuilder.baseUrl(baseUrl);
            if (converterFactory == null) {
                converterFactory = GsonConverterFactory.create();
            }
            retrofitBuilder.addConverterFactory(converterFactory);

            if (callAdapterFactory == null) {
                callAdapterFactory = RxJavaCallAdapterFactory.create();
            }
            retrofitBuilder.addCallAdapterFactory(callAdapterFactory);

            if (sslSocketFactory != null) {
                okHttpBuilder.sslSocketFactory(sslSocketFactory);
            }

            if (hostnameVerifier != null) {
                okHttpBuilder.hostnameVerifier(hostnameVerifier);
            }

            if (httpCacheDirectory == null) {
                httpCacheDirectory = new File(mContext.getCacheDir(), "http_cache");
            }

            if (isCache) {
                try {
                    if (cache == null) {
                        cache = new Cache(httpCacheDirectory, CACHE_MAX_SIZE);
                    }
                    cacheOnline(cache);
                    cacheOffline(cache);
                } catch (Exception e) {
                    ViseLog.e("Could not create http cache" + e);
                }
            }

            if (cache != null) {
                okHttpBuilder.cache(cache);
            }

            if (connectionPool == null) {
                connectionPool = new ConnectionPool(DEFAULT_MAX_IDLE_CONNECTIONS, DEFAULT_KEEP_ALIVE_DURATION,
                        TimeUnit.SECONDS);
            }
            okHttpBuilder.connectionPool(connectionPool);

            if (proxy == null) {
                okHttpBuilder.proxy(proxy);
            }

            if (isCookie && apiCookie == null) {
                okHttpBuilder.cookieJar(new ApiCookie(mContext));
            }

            if (apiCookie != null) {
                okHttpBuilder.cookieJar(apiCookie);
            }
            if (callFactory != null) {
                retrofitBuilder.callFactory(callFactory);
            }

            okHttpClient = okHttpBuilder.build();
            retrofitBuilder.client(okHttpClient);
            retrofit = retrofitBuilder.build();
            apiCache = apiCacheBuilder.build();
            apiService = retrofit.create(ApiService.class);

            return new ViseNet(callFactory, headers, parameters, apiService, converterFactories, adapterFactories,
                    callbackExecutor, validateEagerly, cacheMode);
        }
    }
}
