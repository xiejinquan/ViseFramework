package com.vise.base.net.subscriber;

import android.content.Context;
import android.text.TextUtils;

import com.vise.base.net.callback.IDownCallback;
import com.vise.base.net.exception.ApiException;
import com.vise.base.net.mode.ApiCode;
import com.vise.log.ViseLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-01-03 14:16
 */
public class DownloadSubscriber<T extends ResponseBody> extends ApiSubscriber<T> {
    private IDownCallback callBack;
    private Context context;
    private String path;
    private String name;

    private static String APK_CONTENT_TYPE = "application/vnd.android.package-archive";
    private static String PNG_CONTENT_TYPE = "image/png";
    private static String JPG_CONTENT_TYPE = "image/jpg";
    private static String fileSuffix = "";

    public DownloadSubscriber(Context context, String path, String name, IDownCallback callBack) {
        super(context);
        this.context = context;
        this.path = path;
        this.name = name;
        this.callBack = callBack;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (callBack != null) {
            callBack.onStart();
        }
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(final ApiException e) {
        ViseLog.d("DownSubscriber:>>>> onError:" + e.getMessage());
        callBack.onFailed(e);
    }

    @Override
    public void onNext(ResponseBody responseBody) {
        ViseLog.d("DownSubscriber:>>>> onNext");
        writeResponseBodyToDisk(path, name, context, responseBody);
    }

    private boolean writeResponseBodyToDisk(String path, String name, Context context, okhttp3.ResponseBody body) {
        ViseLog.d("contentType:>>>>" + body.contentType().toString());
        if (!TextUtils.isEmpty(name)) {
            String type = "";
            if (!name.contains(".")) {
                type = body.contentType().toString();
                if (type.equals(APK_CONTENT_TYPE)) {
                    fileSuffix = ".apk";
                } else if (type.equals(PNG_CONTENT_TYPE)) {
                    fileSuffix = ".png";
                } else if (type.equals(JPG_CONTENT_TYPE)) {
                    fileSuffix = ".jpg";
                } else {
                    fileSuffix = body.contentType().subtype();
                }
                name = name + fileSuffix;
            }
        } else {
            name = System.currentTimeMillis() + fileSuffix;
        }

        if (path == null) {
            path = context.getExternalFilesDir(null) + File.separator + name;
        }
        ViseLog.i("path:-->" + path);
        try {
            File futureStudioIconFile = new File(path);
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                byte[] fileReader = new byte[4096];

                final long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;
                ViseLog.d("file length: " + fileSize);
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;
                    ViseLog.i("file download: " + fileSizeDownloaded + " of " + fileSize);
                    if (callBack != null) {
                        if (callBack != null) {
                            final long finalFileSizeDownloaded = fileSizeDownloaded;
                            Observable.just(finalFileSizeDownloaded).observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new Action1<Long>() {
                                @Override
                                public void call(Long finalFileSizeDownloaded) {
                                    callBack.update(finalFileSizeDownloaded, fileSize, finalFileSizeDownloaded ==
                                            fileSize);
                                }
                            });
                        }
                    }
                }
                outputStream.flush();
                ViseLog.i("file downloaded: " + fileSizeDownloaded + " of " + fileSize);
                if (callBack != null) {
                    Observable.just(path).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {
                        @Override
                        public void call(String finalPath) {
                            callBack.onComplete(finalPath);
                        }
                    });
                    ViseLog.i("file downloaded: " + fileSizeDownloaded + " of " + fileSize);
                    ViseLog.i("file downloaded: is success");
                }

                return true;
            } catch (IOException e) {
                finalOnError(e);
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            finalOnError(e);
            return false;
        }
    }

    private void finalOnError(final Exception e) {
        if (callBack == null) {
            return;
        }
        Observable.just(new ApiException(e, ApiCode.NETWORK_ERROR)).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ApiException>() {
            @Override
            public void call(ApiException e) {
                callBack.onFailed(e);
            }
        });
    }
}
