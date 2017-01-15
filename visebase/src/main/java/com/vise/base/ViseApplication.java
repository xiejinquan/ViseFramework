package com.vise.base;

import android.app.Application;

import com.vise.base.common.ViseContext;

/**
 * @Description: Application基类
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-19 14:49
 */
public class ViseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ViseContext.getInstance(this).init();
    }
}
