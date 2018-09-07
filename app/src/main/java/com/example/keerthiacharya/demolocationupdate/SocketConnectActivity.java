package com.example.keerthiacharya.demolocationupdate;

import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.keerthiacharya.demolocationupdate.service.LocationUpdatesService;

import org.json.JSONObject;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import okhttp3.OkHttpClient;

public class SocketConnectActivity extends AppCompatActivity {


    private MyApp myApp;
    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket_connect);

        myApp = (MyApp) getApplication();
        mSocket = myApp.getSocket();
        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                onMessage("EVENT_CONNECT");

//                testing_location

                for (int i = 0; i < 100; i++) {
                    mSocket.emit("testing_location", "hi");
                }

            }
        }).on(Socket.EVENT_PING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessage("EVENT_PING");
            }
        }).on(Socket.EVENT_PONG, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessage("EVENT_PONG");
            }
        }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessage("EVENT_MESSAGE");
            }
        }).on(Socket.EVENT_CONNECTING, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessage("EVENT_CONNECTING");
            }
        }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {

                Log.e("Data-->", "" + args[0].toString());

                onMessage("EVENT_CONNECT_ERROR");
            }
        }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessage("EVENT_CONNECT_TIMEOUT");
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                onMessage("EVENT_DISCONNECT");
            }
        });

//        mSocket.emit("testing_location", "hello Rajdip", new Ack() {
//            @Override
//            public void call(Object... args) {
//
//                Log.e("data", "" + args.length);
//
//            }
//        });

        // Receiving an object
        mSocket.on("testing_location", new Emitter.Listener() {
            @Override
            public void call(Object... args) {

//                JSONObject obj = (JSONObject) args[0];
                Log.e("data", "" + args[args.length - 1]);
            }
        });
        if (!mSocket.connected())
            mSocket.connect();
    }

    private void onMessage(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                Toast.makeText(SocketConnectActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSocket != null && mSocket.connected()) {
            mSocket.disconnect();


            mSocket.off(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onMessage("onDestroy -- >EVENT_CONNECT");
                }
            });
            mSocket.off(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onMessage("onDestroy -- >EVENT_DISCONNECT");
                }
            });
            mSocket.off(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onMessage("onDestroy -- >EVENT_CONNECT_ERROR");
                }
            });
            mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    onMessage("onDestroy -- >EVENT_CONNECT_TIMEOUT");
                }
            });
        }
    }
}
