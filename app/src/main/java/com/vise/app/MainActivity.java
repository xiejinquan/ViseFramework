package com.vise.app;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vise.log.ViseLog;
import com.vise.udp.ViseUdp;
import com.vise.udp.command.DiscoverHost;
import com.vise.udp.core.Connection;
import com.vise.udp.core.inter.IListener;
import com.vise.udp.exception.UdpException;
import com.vise.udp.mode.PacketBuffer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private EditText mEdit_tcp;
    private Button mSend_tcp;
    private EditText mEdit_udp;
    private Button mSend_udp;
    private TextView mShow_msg;

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
            ViseUdp.getInstance().getUdpConfig().setIp("172.26.183.4").setPort(8888);
            initTcpServer();
            initTcpClient();
            initUdpServer();
            initUdpClient();
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
        mSend_tcp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mEdit_tcp.getText().toString() != null){
                } else{
                    Toast.makeText(mContext, "this input msg is null!", Toast.LENGTH_SHORT).show();
                    ViseLog.i("this input msg is null!");
                }
            }
        });
        mSend_udp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mEdit_udp.getText().toString() != null){
                    final PacketBuffer packetBuffer = new PacketBuffer();
                    packetBuffer.setCommand(new DiscoverHost());
                        new Thread(){
                            @Override
                            public void run() {
                                ViseUdp.getInstance().getClient().send(packetBuffer);
                            }
                        }.start();
                } else{
                    Toast.makeText(mContext, "this input msg is null!", Toast.LENGTH_SHORT).show();
                    ViseLog.i("this input msg is null!");
                }
            }
        });
    }

    private void initTcpClient() {

    }

    private void initTcpServer() {

    }

    private void initUdpClient() throws IOException {
        ViseUdp.getInstance().addClientListener(new IListener() {
            @Override
            public void onStart(Connection connection) {

            }

            @Override
            public void onStop(Connection connection) {

            }

            @Override
            public void onSend(Connection connection, PacketBuffer packetBuffer) {
                ViseLog.i(packetBuffer);
            }

            @Override
            public void onReceive(Connection connection, PacketBuffer packetBuffer) {
                ViseLog.i(packetBuffer);
            }

            @Override
            public void onError(Connection connection, UdpException e) {
                ViseLog.i(e);
            }
        });
        ViseUdp.getInstance().startClient();
        new Thread(){
            @Override
            public void run() {
                try {
                    ViseUdp.getInstance().connect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void initUdpServer() throws IOException {
        ViseUdp.getInstance().addServerListener(new IListener() {
            @Override
            public void onStart(Connection connection) {

            }

            @Override
            public void onStop(Connection connection) {

            }

            @Override
            public void onSend(Connection connection, PacketBuffer packetBuffer) {
                ViseLog.i(packetBuffer);
            }

            @Override
            public void onReceive(Connection connection, final PacketBuffer packetBuffer) {
                ViseLog.i(packetBuffer);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mShow_msg.setText(packetBuffer.toString());
                    }
                });
            }

            @Override
            public void onError(Connection connection, UdpException e) {
                ViseLog.i(e);
            }
        });
        ViseUdp.getInstance().bindServer();
        ViseUdp.getInstance().startServer();
    }

    private void bindViews() {
        mEdit_tcp = (EditText) findViewById(R.id.edit_tcp);
        mSend_tcp = (Button) findViewById(R.id.send_tcp);
        mEdit_udp = (EditText) findViewById(R.id.edit_udp);
        mSend_udp = (Button) findViewById(R.id.send_udp);
        mShow_msg = (TextView) findViewById(R.id.show_msg);
    }

}
