package com.vise.app;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.vise.base.net.api.ViseApi;
import com.vise.base.net.callback.ApiCallback;
import com.vise.base.net.exception.ApiException;
import com.vise.base.net.mode.CacheMode;
import com.vise.base.net.mode.CacheResult;
import com.vise.log.ViseLog;

import java.util.HashMap;

import rx.functions.Action1;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext;
    private ViseApi api;
    private Button mRequest_get;
    private Button mRequest_post;
    private TextView mShow_msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        api = new ViseApi.Builder(mContext).cache(true).build();
        init();
    }

    private void init() {
        bindViews();
        bindEvent();
    }

    private void bindEvent() {
        mRequest_get.setOnClickListener(this);
        mRequest_post.setOnClickListener(this);
    }

    private void bindViews() {
        mRequest_get = (Button) findViewById(R.id.request_get);
        mRequest_post = (Button) findViewById(R.id.request_post);
        mShow_msg = (TextView) findViewById(R.id.show_msg);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.request_get:
                mShow_msg.setText("");
                api.get("", new HashMap<String, String>(), GithubModel.class).subscribe(new Action1<GithubModel>() {
                    @Override
                    public void call(GithubModel githubModel) {
                        ViseLog.i(githubModel.toString());
                        mShow_msg.setText(githubModel.toString());
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        ViseLog.d("throwable:" + throwable);
                    }
                });

                break;
            case R.id.request_post:
                mShow_msg.setText("");
                api.get("", new HashMap<String, String>(), new ApiCallback<GithubModel>() {
                    @Override
                    public void onStart() {
                        ViseLog.d("onStart");
                    }

                    @Override
                    public void onError(ApiException e) {
                        ViseLog.d("onError:" + e);
                    }

                    @Override
                    public void onCompleted() {
                        ViseLog.d("onCompleted");
                    }

                    @Override
                    public void onNext(GithubModel githubModel) {
                        ViseLog.i(githubModel.toString());
                        mShow_msg.setText(githubModel.toString());
                    }
                });
                break;
        }
    }
}
