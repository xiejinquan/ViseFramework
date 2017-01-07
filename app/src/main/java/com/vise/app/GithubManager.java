package com.vise.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.vise.base.database.DBManager;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.query.QueryBuilder;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-01-07 16:03
 */
public class GithubManager<M, K> extends DBManager<M, K> {

    private static final String DB_NAME = "vise.db";//数据库名称
    private static GithubManager instance;
    private DaoMaster.DevOpenHelper mHelper;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;

    private GithubManager() {

    }

    public static GithubManager getInstance() {
        if (instance == null) {
            synchronized (GithubManager.class) {
                if (instance == null) {
                    instance = new GithubManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        mHelper = new DaoMaster.DevOpenHelper(context, DB_NAME, null);
        mDaoMaster = new DaoMaster(mHelper.getWritableDatabase());
        mDaoSession = mDaoMaster.newSession();
    }

    public DaoMaster getDaoMaster() {
        return mDaoMaster;
    }

    public DaoSession getDaoSession() {
        return mDaoSession;
    }

    @Override
    public AbstractDao getAbstractDao() {
        return mDaoSession.getGithubModelDao();
    }

    @Override
    public void clearDaoSession() {
        if (null != mDaoSession) {
            mDaoSession.clear();
            mDaoSession = null;
        }
    }

    @Override
    public boolean closeDataBase() {
        closeHelper();
        clearDaoSession();
        return false;
    }

    private void closeHelper() {
        if (mHelper != null) {
            mHelper.close();
            mHelper = null;
        }
    }

}
