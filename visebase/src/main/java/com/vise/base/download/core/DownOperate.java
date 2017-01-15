package com.vise.base.download.core;

import com.vise.base.download.ViseDownload;
import com.vise.base.download.db.DownDbManager;
import com.vise.base.download.mode.DownEvent;
import com.vise.base.download.mode.DownProgress;
import com.vise.base.download.mode.DownStatus;
import com.vise.log.ViseLog;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.Subject;

/**
 * @Description: 下载任务操作
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/1/15 17:49.
 */
public class DownOperate {
    private boolean canceled = false;
    private ViseDownload viseDownload;
    private String url;
    private String saveName;
    private String savePath;
    private DownProgress downProgress;
    private Subscription subscription;

    public boolean isCanceled() {
        return canceled;
    }

    public DownOperate setCanceled(boolean canceled) {
        this.canceled = canceled;
        return this;
    }

    public ViseDownload getViseDownload() {
        return viseDownload;
    }

    public DownOperate setViseDownload(ViseDownload viseDownload) {
        this.viseDownload = viseDownload;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public DownOperate setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getSaveName() {
        return saveName;
    }

    public DownOperate setSaveName(String saveName) {
        this.saveName = saveName;
        return this;
    }

    public String getSavePath() {
        return savePath;
    }

    public DownOperate setSavePath(String savePath) {
        this.savePath = savePath;
        return this;
    }

    public DownProgress getDownProgress() {
        return downProgress;
    }

    public DownOperate setDownProgress(DownProgress downProgress) {
        this.downProgress = downProgress;
        return this;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public DownOperate setSubscription(Subscription subscription) {
        this.subscription = subscription;
        return this;
    }

    public void start(final Map<String, DownOperate> nowDownloadMap,
                      final AtomicInteger count, final DownDbManager helper,
                      final Map<String, Subject<DownEvent, DownEvent>> subjectPool) {
        nowDownloadMap.put(url, this);
        count.incrementAndGet();
        final DownEventFactory eventFactory = DownEventFactory.getSingleton();
        subscription = viseDownload.download(url, saveName, savePath)
                .subscribeOn(Schedulers.io())
                .onBackpressureLatest()
                .subscribe(new Subscriber<DownProgress>() {
                    @Override
                    public void onStart() {
                        super.onStart();
                        helper.updateRecord(url, DownStatus.STARTED.getStatus());
                    }

                    @Override
                    public void onCompleted() {
                        subjectPool.get(url).onNext(eventFactory.factory(url, DownStatus.COMPLETED.getStatus(), downProgress));

                        helper.updateRecord(url, DownStatus.COMPLETED.getStatus());
                        count.decrementAndGet();
                        nowDownloadMap.remove(url);
                    }

                    @Override
                    public void onError(Throwable e) {
                        ViseLog.w("error:" + e);
                        subjectPool.get(url).onNext(eventFactory.factory(url, DownStatus.FAILED.getStatus(), downProgress, e));

                        helper.updateRecord(url, DownStatus.FAILED.getStatus());
                        count.decrementAndGet();
                        nowDownloadMap.remove(url);
                    }

                    @Override
                    public void onNext(DownProgress progress) {
                        subjectPool.get(url).onNext(eventFactory.factory(url, DownStatus.STARTED.getStatus(), progress));
                        helper.updateRecord(url, progress);
                        downProgress = progress;
                    }
                });
    }

    public static class Builder {
        ViseDownload viseDownload;
        String url;
        String saveName;
        String savePath;

        public Builder setViseDownload(ViseDownload viseDownload) {
            this.viseDownload = viseDownload;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setSaveName(String saveName) {
            this.saveName = saveName;
            return this;
        }

        public Builder setSavePath(String savePath) {
            this.savePath = savePath;
            return this;
        }

        public DownOperate build() {
            DownOperate task = new DownOperate();
            task.viseDownload = viseDownload;
            task.url = url;
            task.saveName = saveName;
            task.savePath = savePath;
            return task;
        }
    }
}
