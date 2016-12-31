package com.vise.base.cache;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import com.jakewharton.disklrucache.DiskLruCache;
import com.vise.base.common.ViseConfig;
import com.vise.base.kit.Codec;
import com.vise.base.kit.Kits;
import com.vise.log.ViseLog;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-19 15:10
 */
public class DiskCache implements ICache {
    public static final String TAG_CACHE =
            "=====createTime{createTime_v}expireMills{expireMills_v}";
    public static final String REGEX = "=====createTime\\{(\\d{1,})\\}expireMills\\{(\\d{1,})\\}";

    private DiskLruCache cache;
    private Pattern compile;
    private long cacheTime;

    private DiskCache(Context context, long diskMaxSize, File diskDir, long time) {
        this.cacheTime = time;
        compile = Pattern.compile(REGEX);
        try {
            cache = DiskLruCache.open(diskDir, Kits.Package.getVersionCode(context), 1,
                    diskMaxSize);
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
    }

    public void put(String key, String value) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) return;

        String name = getMd5Key(key);
        try {
            if (!TextUtils.isEmpty(get(name))) {
                cache.remove(name);
            }

            DiskLruCache.Editor editor = cache.edit(name);
            StringBuilder content = new StringBuilder(value);
            content.append(TAG_CACHE.replace("createTime_v", "" + Calendar.getInstance()
                    .getTimeInMillis()).replace("expireMills_v", "" + cacheTime));
            editor.set(0, content.toString());
            editor.commit();
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
    }

    @Override
    public void put(String key, Object value) {
        put(key, value != null ? value.toString() : null);
    }

    public String get(String key) {
        try {
            String md5Key = getMd5Key(key);
            DiskLruCache.Snapshot snapshot = cache.get(md5Key);
            if (snapshot != null) {
                String content = snapshot.getString(0);

                if (!TextUtils.isEmpty(content)) {
                    Matcher matcher = compile.matcher(content);
                    long createTime = 0;
                    long expireMills = 0;
                    while (matcher.find()) {
                        createTime = Long.parseLong(matcher.group(1));
                        expireMills = Long.parseLong(matcher.group(2));
                    }
                    int index = content.indexOf("=====createTime");

                    if ((createTime + expireMills > Calendar.getInstance().getTimeInMillis())
                            || expireMills == Builder.CACHE_NEVER_EXPIRE) {
                        return content.substring(0, index);
                    } else {
                        cache.remove(md5Key);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
        return null;
    }

    public void remove(String key) {
        try {
            cache.remove(getMd5Key(key));
        } catch (Exception e) {
        }
    }

    public boolean contains(String key) {
        try {
            DiskLruCache.Snapshot snapshot = cache.get(getMd5Key(key));
            return snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
        return false;
    }

    public void clear() {
        try {
            cache.delete();
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
    }

    public static String getMd5Key(String key) {
        return Codec.MD5.getMessageDigest(key.getBytes());
    }

    public static final class Builder {
        private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
        private static final int MAX_DISK_CACHE_SIZE = 20 * 1024 * 1024; // 20MB
        private static final long CACHE_NEVER_EXPIRE = -1;//永久不过期
        private final Context context;
        private long cacheTime = CACHE_NEVER_EXPIRE;
        private long diskMaxSize;
        private File diskDir;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder diskDir(File directory) {
            this.diskDir = directory;
            return this;
        }

        public Builder diskMax(long maxSize) {
            this.diskMaxSize = maxSize;
            return this;
        }

        public Builder cacheTime(long cacheTime) {
            this.cacheTime = cacheTime;
            return this;
        }

        public DiskCache build() {
            if (this.diskDir == null) {
                diskDir = getDiskCacheDir(context, ViseConfig.CACHE_DISK_DIR);
            }
            if (!this.diskDir.exists()) {
                this.diskDir.mkdirs();
            }
            if (diskMaxSize <= 0) {
                diskMaxSize = calculateDiskCacheSize(diskDir);
            }
            cacheTime = Math.max(CACHE_NEVER_EXPIRE, this.cacheTime);

            return new DiskCache(context, diskMaxSize, diskDir, cacheTime);
        }

        private static File getDiskCacheDir(Context context, String dirName) {
            String cachePath;
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !Environment.isExternalStorageRemovable()) {
                cachePath = context.getExternalCacheDir().getPath();
            } else {
                cachePath = context.getCacheDir().getPath();
            }
            return new File(cachePath + File.separator + dirName);
        }

        private static long calculateDiskCacheSize(File dir) {
            long size = 0;
            try {
                StatFs statFs = new StatFs(dir.getAbsolutePath());
                long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
                size = available / 50;
            } catch (IllegalArgumentException ignored) {
            }
            return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
        }
    }
}
