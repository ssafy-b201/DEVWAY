package com.ssafy.sagwa.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ssafy.sagwa.R;
import com.ssafy.sagwa.activity.MainActivity;

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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        Button btn_start = findViewById(R.id.btn_start);
        auth = FirebaseAuth.getInstance();

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser user = auth.getCurrentUser();

                if (user != null) {
                    String email = user.getEmail();
                    sendUserInfoToServer(email);
                } else {
                    Log.d("SignupActivity", "사용자 정보 없음");
                    Toast.makeText(SignupActivity.this, "로그인 되어 있지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void sendUserInfoToServer(String email) {
        try {

            OkHttpClient client = getSecureOkHttpClient();

            String url = "https://k10b201.p.ssafy.io/sagwa/api/signup";

            MediaType mediaType = MediaType.parse("application/json");
            String requestBody = "{\"email\":\"" + email + "\"}";
            System.out.println(requestBody);
            RequestBody body = RequestBody.create(requestBody, mediaType);

            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
            System.out.println(request.toString());
            client.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("SignupActivity", "서버 통신 실패", e);
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    Log.d("HTTP Status Code", String.valueOf(response.code()));
                    if (response.isSuccessful()) {
                        try {
                            String jsonResponse = response.body().string();
                            JSONObject jsonObject = null;
                            jsonObject = new JSONObject(jsonResponse);
                            long loginId = jsonObject.getLong("data");
                            System.out.println("1 " + loginId);
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putLong("loginId", loginId);
                            editor.apply();
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
//                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                        Long memberId = sharedPref.getLong("loginId", 0);
                        runOnUiThread(() -> {
                            Toast.makeText(SignupActivity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(SignupActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                            Log.d("HTTP Status Code", String.valueOf(response.code()));
                        });
                    }
                    response.close();
                }
            });
        } catch (Exception e) {
            Log.e("SignupActivity", "///// OkHttp Client Error /////", e);
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

}
