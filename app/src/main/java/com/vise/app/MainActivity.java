package com.vise.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vise.log.ViseLog;
import com.vise.tcp.TcpClient;
import com.vise.tcp.TcpConnection;
import com.vise.tcp.TcpServer;
import com.vise.tcp.listener.Listener;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private EditText mEdit_tcp;
    private Button mSend_tcp;
    private EditText mEdit_udp;
    private Button mSend_udp;
    private TextView mShow_msg;

    private TcpClient tcpClient;
    private TcpServer tcpServer;

    private boolean tcpFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        init();
    }

    private void init() {
        bindViews();
        try {
            initTcpServer();
            initTcpClient();
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
        mSend_tcp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mEdit_tcp.getText().toString() != null){
                    try {
                        tcpClient.getTcp().send(mEdit_tcp.getText().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                        ViseLog.e(e);
                    }
//                    if(tcpFlag){
//
//                    } else{
//                        Toast.makeText(mContext, "this is not connected!", Toast.LENGTH_SHORT).show();
//                        ViseLog.i("this is not connected!");
//                    }
                } else{
                    Toast.makeText(mContext, "this input msg is null!", Toast.LENGTH_SHORT).show();
                    ViseLog.i("this input msg is null!");
                }
            }
        });
        mSend_udp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void initTcpClient() {
        tcpClient = new TcpClient();
        tcpClient.addListener(new Listener(){
            @Override
            public void connected(TcpConnection connection) {
                ViseLog.i("TcpClient connected");
                Toast.makeText(mContext, "connect success!", Toast.LENGTH_SHORT).show();
                tcpFlag = true;
            }

            @Override
            public void disconnected(TcpConnection connection) {
                ViseLog.i("TcpClient disconnected");
            }

            @Override
            public void received(TcpConnection connection, Object object) {
                ViseLog.i("TcpClient received");
                ViseLog.i(object);
                if(object != null){
                    mShow_msg.setText(object.toString());
                }
            }

            @Override
            public void idle(TcpConnection connection) {
                ViseLog.i("TcpClient idle");
            }
        });
        tcpClient.start();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread("Connect") {
                    public void run () {
                        try {
                            tcpClient.connect(10000, "172.26.183.4", 8888);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            ViseLog.e(ex);
                        }
                    }
                }.start();
            }
        }, 3000);
    }

    private void initTcpServer() throws IOException {
        tcpServer = new TcpServer();
        tcpServer.addListener(new Listener(){
            @Override
            public void connected(TcpConnection connection) {
                ViseLog.i("TcpServer connected");
            }

            @Override
            public void disconnected(TcpConnection connection) {
                ViseLog.i("TcpServer disconnected");
            }

            @Override
            public void received(TcpConnection connection, Object object) {
                ViseLog.i("TcpServer received");
                ViseLog.i(object);
                if(object != null){
                    mShow_msg.setText(object.toString());
                }
            }

            @Override
            public void idle(TcpConnection connection) {
                ViseLog.i("TcpServer idle");
            }
        });
        tcpServer.bind(8888);
        tcpServer.start();
    }

    private void bindViews() {
        mEdit_tcp = (EditText) findViewById(R.id.edit_tcp);
        mSend_tcp = (Button) findViewById(R.id.send_tcp);
        mEdit_udp = (EditText) findViewById(R.id.edit_udp);
        mSend_udp = (Button) findViewById(R.id.send_udp);
        mShow_msg = (TextView) findViewById(R.id.show_msg);
    }

}
