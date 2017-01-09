package com.vise.app;

import android.content.Context;

import com.vise.base.database.DBManager;

import org.greenrobot.greendao.AbstractDao;

/**
 * @Description:
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-01-07 16:03
 */
public class DbHelper {

    private static final String DB_NAME = "vise.db";//数据库名称
    private static DbHelper instance;
    private static DBManager<GithubModel, Long> githubModelStringDBManager;
    private static DBManager<Person, Long> personStringDBManager;
    private DaoMaster.DevOpenHelper mHelper;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;

    private DbHelper() {

    }

    public static DbHelper getInstance() {
        if (instance == null) {
            synchronized (DbHelper.class) {
                if (instance == null) {
                    instance = new DbHelper();
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

    public void init(Context context, String dbName) {
        mHelper = new DaoMaster.DevOpenHelper(context, dbName, null);
        mDaoMaster = new DaoMaster(mHelper.getWritableDatabase());
        mDaoSession = mDaoMaster.newSession();
    }

    public DBManager<GithubModel, Long> gitHub() {
        if (githubModelStringDBManager == null) {
            githubModelStringDBManager = new DBManager<GithubModel, Long>(){
                @Override
                public AbstractDao<GithubModel, Long> getAbstractDao() {
                    return mDaoSession.getGithubModelDao();
                }
            };
        }
        return githubModelStringDBManager;
    }

    public DBManager<Person, Long> person(){
        if (personStringDBManager == null) {
            personStringDBManager = new DBManager<Person, Long>() {
                @Override
                public AbstractDao<Person, Long> getAbstractDao() {
                    return mDaoSession.getPersonDao();
                }
            };
        }
        return personStringDBManager;
    }

    public DaoMaster getDaoMaster() {
        return mDaoMaster;
    }

    public DaoSession getDaoSession() {
        return mDaoSession;
    }

    public void clear() {
        if (mDaoSession != null) {
            mDaoSession.clear();
            mDaoSession = null;
        }
    }

    public void close() {
        clear();
        if (mHelper != null) {
            mHelper.close();
            mHelper = null;
        }
    }

}
