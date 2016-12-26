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
import com.vise.udp.ViseUdp;
import com.vise.udp.command.DiscoverHost;
import com.vise.udp.core.UdpOperate;
import com.vise.udp.core.inter.IListener;
import com.vise.udp.exception.UdpException;
import com.vise.udp.mode.PacketBuffer;
import com.vise.udp.mode.TargetInfo;
import com.vise.udp.utils.HexUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

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
        ViseUdp.getInstance().getUdpConfig().setIp("192.168.1.106").setPort(8888);
        try {
            initUdpServer();
            initUdpClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSend_tcp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        mSend_udp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PacketBuffer packetBuffer = new PacketBuffer();
                packetBuffer.setTargetInfo(new TargetInfo().setIp("192.168.1.106").setPort(8888));
                String data = mEdit_udp.getText().toString();
                packetBuffer.setBytes(HexUtil.decodeHex(data.toCharArray()));
                    new Thread(){
                        @Override
                        public void run() {
                            try {
                                ViseUdp.getInstance().send(packetBuffer);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
            }
        });
    }

    private void initUdpClient() throws IOException {
        ViseUdp.getInstance().startClient(new IListener() {
            @Override
            public void onStart(UdpOperate udpOperate) {

            }

            @Override
            public void onStop(UdpOperate udpOperate) {

            }

            @Override
            public void onSend(UdpOperate udpOperate, PacketBuffer packetBuffer) {
                ViseLog.i(packetBuffer);
            }

            @Override
            public void onReceive(UdpOperate udpOperate, PacketBuffer packetBuffer) {
                ViseLog.i(packetBuffer);
            }

            @Override
            public void onError(UdpOperate udpOperate, UdpException e) {
                ViseLog.i(e);
            }
        });
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
        ViseUdp.getInstance().startServer(new IListener() {
            @Override
            public void onStart(UdpOperate udpOperate) {

            }

            @Override
            public void onStop(UdpOperate udpOperate) {

            }

            @Override
            public void onSend(UdpOperate udpOperate, PacketBuffer packetBuffer) {
                ViseLog.i(packetBuffer);
            }

            @Override
            public void onReceive(UdpOperate udpOperate, final PacketBuffer packetBuffer) {
                ViseLog.i(packetBuffer);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mShow_msg.setText(packetBuffer.toString());
                    }
                });
            }

            @Override
            public void onError(UdpOperate udpOperate, UdpException e) {
                ViseLog.i(e);
            }
        });
    }

    private void bindViews() {
        mEdit_tcp = (EditText) findViewById(R.id.edit_tcp);
        mSend_tcp = (Button) findViewById(R.id.send_tcp);
        mEdit_udp = (EditText) findViewById(R.id.edit_udp);
        mSend_udp = (Button) findViewById(R.id.send_udp);
        mShow_msg = (TextView) findViewById(R.id.show_msg);
    }

    @Override
    protected void onDestroy() {
        ViseUdp.getInstance().stop();
        super.onDestroy();
    }
}
