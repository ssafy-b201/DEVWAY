package com.ssafy.sagwa;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SplashActivity extends AppCompatActivity {

    // Firebase 인증 객체
    private FirebaseAuth auth;
    private static final int SPLASH_TIME_OUT = 1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser != null) {
                // 이미 로그인된 사용자
                checkUserRegistration(currentUser);
            } else {
                // 로그인 되어있지 않은 사용자
                promptLogin();
            }

        }, SPLASH_TIME_OUT);
    }

    private void checkUserRegistration(FirebaseUser user) {
        String userEmail = user.getEmail();
        sendEmailToServer(userEmail);
    }

    private void sendEmailToServer(String email) {
        try {

            OkHttpClient client = getSecureOkHttpClient();

            HttpUrl.Builder urlBuilder = HttpUrl.parse("https://k10b201.p.ssafy.io/sagwa/api/valid").newBuilder();
            urlBuilder.addQueryParameter("memberEmail", email);
            String url = urlBuilder.build().toString();

            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("SplashActivity", "Email 검증 실패", e);
                    promptLogin();
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> promptLogin());
                        return;
                    }
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        boolean isRegistered = jsonObject.getBoolean("data");
                        System.out.println(isRegistered);
                        runOnUiThread(() -> {
                            if (isRegistered) {
                                proceedToMain();
                            } else {
                                promptLogin();
                            }
                        });
                    } catch (JSONException e) {
                        Log.e("SplashActivity", "JSON 파싱 오류", e);
                        runOnUiThread(() -> promptLogin());
                    }
                }
            });
        } catch (Exception e) {
            Log.e("SplashActivity", "///// OkHttp Client Error /////", e);
        }
    }

    private OkHttpClient getSecureOkHttpClient() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream caInput = getResources().openRawResource(R.raw.k10b201);
        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
        } finally {
            caInput.close();
        }

        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        X509TrustManager trustManager = (X509TrustManager) tmf.getTrustManagers()[0];

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new javax.net.ssl.TrustManager[]{trustManager}, null);

        return new OkHttpClient.Builder()
            .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
            .build();
    }

    private void proceedToMain() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    private void promptLogin() {
        startActivity(new Intent(SplashActivity.this, SigninActivity.class));
        finish();
    }
}