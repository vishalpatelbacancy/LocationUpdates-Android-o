package com.example.keerthiacharya.demolocationupdate;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;
import okhttp3.OkHttpClient;

/**
 * Created by siddharth on 6/9/18.
 */

public class MyApp extends Application {
    private static Context context;

    private Socket mSocket;

    @Override
    public void onCreate() {
        super.onCreate();

        //@formatter:off
        context = getApplicationContext();
        //@formatter:on


        HostnameVerifier myHostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        SSLContext mySSLContext = null;
        try {
            mySSLContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType) throws CertificateException {
            }
        }};

        try {
            mySSLContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .hostnameVerifier(myHostnameVerifier)

                .sslSocketFactory(mySSLContext.getSocketFactory(), new X509TrustManager() {

                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) throws CertificateException {

                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }

                })
                .build();


        // HttpsURLConnection.setDefaultHostnameVerifier(myHostnameVerifier);
        IO.Options options = new IO.Options();
        options.webSocketFactory = okHttpClient;
        options.secure = true;
        options.transports = new String[]{WebSocket.NAME};
        options.reconnection = true;
        options.forceNew = true;

        options.callFactory = okHttpClient;
        options.webSocketFactory = okHttpClient;

// default settings for all sockets
        IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
        IO.setDefaultOkHttpCallFactory(okHttpClient);

//
//        IO.Options opts = new IO.Options();
//        opts.callFactory = okHttpClient;
//        opts.webSocketFactory = okHttpClient;
//        opts.timeout = 6000;
//        opts.forceNew = true;
//        opts.reconnection = true;

        try {
            mSocket = IO.socket(getResources().getString(R.string.Socket_URL_NEW), options);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }


    public static Context getContext() {
        return context;
    }

    public Socket getSocket() {
        return mSocket;
    }
}
