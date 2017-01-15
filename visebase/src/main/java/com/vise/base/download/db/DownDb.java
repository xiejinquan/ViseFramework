package com.vise.base.download.db;

import android.content.ContentValues;
import android.database.Cursor;

import com.vise.base.download.core.DownOperate;
import com.vise.base.download.mode.DownProgress;
import com.vise.base.download.mode.DownRecord;
import com.vise.base.download.mode.DownStatus;

import java.util.Date;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/1/15 21:06.
 */
public class DownDb {
    public static final class RecordTable {
        static final String TABLE_NAME = "download_record";

        static final String COLUMN_ID = "id";
        static final String COLUMN_URL = "url";
        static final String COLUMN_SAVE_NAME = "save_name";
        static final String COLUMN_SAVE_PATH = "save_path";
        static final String COLUMN_DOWNLOAD_SIZE = "download_size";
        static final String COLUMN_TOTAL_SIZE = "total_size";
        static final String COLUMN_IS_CHUNKED = "is_chunked";
        static final String COLUMN_DOWNLOAD_STATUS = "download_status";
        static final String COLUMN_DATE = "date";

        static final String CREATE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_URL + " TEXT NOT NULL," +
                        COLUMN_SAVE_NAME + " TEXT," +
                        COLUMN_SAVE_PATH + " TEXT," +
                        COLUMN_TOTAL_SIZE + " INTEGER," +
                        COLUMN_DOWNLOAD_SIZE + " INTEGER," +
                        COLUMN_IS_CHUNKED + " INTEGER," +
                        COLUMN_DOWNLOAD_STATUS + " INTEGER," +
                        COLUMN_DATE + " INTEGER NOT NULL " +
                        " )";

        static ContentValues insertOperate(DownOperate operate) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_URL, operate.getUrl());
            values.put(COLUMN_SAVE_NAME, operate.getSaveName());
            values.put(COLUMN_SAVE_PATH, operate.getSavePath());
            values.put(COLUMN_DOWNLOAD_STATUS, DownStatus.WAITING.getStatus());
            values.put(COLUMN_DATE, new Date().getTime());
            return values;
        }

        static ContentValues updateProgress(DownProgress progress) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_IS_CHUNKED, progress.isChunked());
            values.put(COLUMN_DOWNLOAD_SIZE, progress.getDownloadSize());
            values.put(COLUMN_TOTAL_SIZE, progress.getTotalSize());
            return values;
        }

        static ContentValues updateStatus(int status) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_DOWNLOAD_STATUS, status);
            return values;
        }

        static DownProgress readProgress(Cursor cursor) {
            boolean isChunked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CHUNKED)) > 0;
            long downloadSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_SIZE));
            long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE));
            return new DownProgress(isChunked, downloadSize, totalSize);
        }

        static DownRecord readRecord(Cursor cursor) {
            DownRecord record = new DownRecord();
            record.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL)));
            record.setSaveName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_NAME)));
            record.setSavePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SAVE_PATH)));

            boolean isChunked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_CHUNKED)) > 0;
            long downloadSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_SIZE));
            long totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE));
            record.setDownProgress(new DownProgress(isChunked, downloadSize, totalSize));

            record.setStatus(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DOWNLOAD_STATUS)));
            record.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE)));
            return record;
        }
    }
}
