package com.ssafy.sagwa.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ssafy.sagwa.R;
import com.ssafy.sagwa.api.PhoneBook.PhoneBookList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CallListActivity extends AppCompatActivity {
    private static final int REQUEST_READ_CALL_LOG = 101;
    private ImageView listBtn;
    private ImageView graphBtn;
    private ViewGroup callListContainer;
    private TextView addBtn;
    private static int month;
    private static int year;
    private TextView headerMonth;
    private TextView headerYear;
    private static boolean isList;
    private List<PhoneBookList> phoneBookList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_call_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Calendar calendar = Calendar.getInstance();
        month = calendar.get(Calendar.MONTH) + 1;
        year = calendar.get(Calendar.YEAR);

        callListContainer = findViewById(R.id.call_item);
        listBtn = findViewById(R.id.call_list_btn);
        graphBtn = findViewById(R.id.call_graph_btn);
        addBtn = findViewById(R.id.call_add);

        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_READ_CALL_LOG);
        } else {
            getPhoneBookList(() -> {
                getCallHistory();
            });
        }

        TextView leftBtn = findViewById(R.id.btn_left);
        TextView rightBtn = findViewById(R.id.btn_right);
        headerMonth = findViewById(R.id.calendar_header_date);
        headerMonth.setText(month + "월");

        headerYear = findViewById(R.id.calendar_header_year);
        headerYear.setText(year + "년");

        listBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCallHistory();
            }
        });

        graphBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCallGraph();
            }
        });

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCallAdd();
            }
        });

        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (month == 1) {
                    month = 12;
                    headerMonth.setText(month + "월");
                    year--;
                    headerYear.setText(year + "월");
                } else {
                    month--;
                    headerMonth.setText(month + "월");
                }

                if (isList) {
                    getCallHistory();
                } else {
                    getCallGraph();
                }
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (month == calendar.get(Calendar.MONTH) + 1 && year == calendar.get(Calendar.YEAR)) {
                    Toast.makeText(CallListActivity.this, "현재 이후의 날짜는 확인이 불가합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (month == 12) {
                    month = 1;
                    headerMonth.setText(month + "월");
                    year++;
                    headerYear.setText(year + "월");
                } else {
                    month++;
                    headerMonth.setText(month + "월");
                }
                if (isList) {
                    getCallHistory();
                } else {
                    getCallGraph();
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CALL_LOG) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCallHistory();
            }
        }
    }


    @SuppressLint("SetTextI18n")
    public void getCallHistory() {
        graphBtn.setBackgroundTintList(getResources().getColorStateList(R.color.white));
        graphBtn.setImageTintList(getResources().getColorStateList(R.color.black));
        listBtn.setBackgroundTintList(getResources().getColorStateList(R.color.point));
        listBtn.setImageTintList(getResources().getColorStateList(R.color.white));
        addBtn.setVisibility(View.GONE);
        isList = true;

        callListContainer.removeAllViews();
        Cursor c = getCall();
        LayoutInflater inflater = LayoutInflater.from(this);

        TextView nameView;
        if (c == null || c.getCount() == 0) {
            View callView = inflater.inflate(R.layout.sample_call_list_view, callListContainer, false);
            nameView = callView.findViewById(R.id.call_name);
            nameView.setText("통화 내역이 없습니다.");
            callListContainer.addView(callView);
            return;
        }

        if (c.moveToFirst()) {
            do {
                String phoneNumber = c.getString(2);
                boolean found = false;
                PhoneBookList matchedPhoneBook = null;
                for (PhoneBookList pb : phoneBookList) {
                    if (pb.getNumber().equals(phoneNumber)) {
                        found = true;
                        matchedPhoneBook = pb;
                        break;
                    }
                }

                if (!found) {
                    continue;
                }
                View callView = inflater.inflate(R.layout.sample_call_list_view, callListContainer, false);
                nameView = callView.findViewById(R.id.call_name);
                TextView timeView = callView.findViewById(R.id.call_time);
                TextView dateView = callView.findViewById(R.id.call_date);
                TextView doneView = callView.findViewById(R.id.call_done);

                long callDate = c.getLong(0);
                SimpleDateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd");
                String date_str = datePattern.format(new Date(callDate));

                nameView.setText(matchedPhoneBook.getName());
                int callSeconds = Integer.parseInt(c.getString(3));
                if (callSeconds >= 60) {
                    timeView.setText(callSeconds / 60 + "분 " + callSeconds % 60 + "초");
                } else {
                    timeView.setText(callSeconds + "초");
                }
                dateView.setText(date_str);
                doneView.setText(" 통화했습니다.");
                callListContainer.addView(callView);
            } while (c.moveToNext());
        }
        c.close();
    }

    @SuppressLint("SetTextI18n")
    public void getCallGraph() {
        listBtn.setBackgroundTintList(getResources().getColorStateList(R.color.white));
        listBtn.setImageTintList(getResources().getColorStateList(R.color.black));
        graphBtn.setBackgroundTintList(getResources().getColorStateList(R.color.point));
        graphBtn.setImageTintList(getResources().getColorStateList(R.color.white));
        addBtn.setVisibility(View.VISIBLE);
        isList = false;

        callListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        TextView nameView;
        TextView xBtn;
        ProgressBar progressBar;
        if (phoneBookList.isEmpty()) {
            View callView = inflater.inflate(R.layout.sample_call_graph_view, callListContainer, false);
            nameView = callView.findViewById(R.id.call_name);
            nameView.setText("등록된 번호가 없습니다.");
            xBtn = callView.findViewById(R.id.call_x);
            xBtn.setVisibility(View.GONE);
            progressBar = callView.findViewById(R.id.call_graph);
            progressBar.setVisibility(View.GONE);
            callListContainer.addView(callView);
            return;
        }

        for (int i = 0; i < phoneBookList.size(); i++) {
            View callView = inflater.inflate(R.layout.sample_call_graph_view, callListContainer, false);
            Cursor selectC = getCallByNum(phoneBookList.get(i).getNumber());
            nameView = callView.findViewById(R.id.call_name);
            TextView numView = callView.findViewById(R.id.call_num);
            TextView cntView = callView.findViewById(R.id.call_cnt);
            xBtn = callView.findViewById(R.id.call_x);
            progressBar = callView.findViewById(R.id.call_graph);

            int number = Math.toIntExact(phoneBookList.get(i).getId());
            xBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CallListActivity.this);
                    builder.setTitle("삭제 확인");
                    builder.setMessage("정말 삭제하시겠습니까?");

                    builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteCall(number);
                        }
                    });

                    builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });

                    // 다이얼로그를 화면에 표시
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });

            nameView.setText(phoneBookList.get(i).getName());
            numView.setText(phoneBookList.get(i).getNumber());
            if (selectC != null) {
                cntView.setText("총 " + selectC.getCount() + "번 통화했어요");
                progressBar.setProgress((selectC.getCount()));
            } else {
                cntView.setText("총 0번 통화했어요");
                progressBar.setProgress(0);
            }
            callListContainer.addView(callView);
        }

    }

    public void getCallAdd() {
        addBtn.setVisibility(View.GONE);
        callListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        View callView = inflater.inflate(R.layout.sample_call_add_view, callListContainer, false);

        TextView realAddBtn = callView.findViewById(R.id.call_add_real);
        EditText addNum = callView.findViewById(R.id.call_add_num);
        EditText addName = callView.findViewById(R.id.call_add_name);
        callListContainer.addView(callView);
        realAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String num = addNum.getText().toString();
                String name = addName.getText().toString();
                addCall(num, name);
            }
        });

    }


    private void addCall(String num, String name) {
        if (num.length() > 11 || num.length() < 9 || num.charAt(0) != '0') {
            runOnUiThread(() -> {
                Toast.makeText(CallListActivity.this, "올바른 번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        try {
            OkHttpClient client = getSecureOkHttpClient();

            String url = "https://k10b201.p.ssafy.io/sagwa/api/phoneBook";

            MediaType mediaType = MediaType.parse("application/json");
            String requestBody = "{\"memberId\":\"" + 11 + "\", \"phoneBookNumber\":\"" + num + "\", \"phoneBookName\":\"" + name + "\"}";
            RequestBody body = RequestBody.create(requestBody, mediaType);

            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
            client.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("CallListActivity", "번호 저장 실패", e);
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        return;
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(CallListActivity.this, "추가되었습니다.", Toast.LENGTH_SHORT).show();
                        Log.d("HTTP Status Code", String.valueOf(response.code()));
                        getPhoneBookList(() -> {
                            getCallGraph();
                        });
                    });

                }
            });
        } catch (Exception e) {
            Log.e("CallListActivity", "///// OkHttp Client Error /////", e);
        }
        return;
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

    private void getPhoneBookList(Runnable onComplete) {
        phoneBookList = new ArrayList<>();
        try {
            OkHttpClient client = getSecureOkHttpClient();
            String url = "https://k10b201.p.ssafy.io/sagwa/api/phoneBook/" + 11;
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("CallListActivity", "불러오기 실패", e);
                    runOnUiThread(() -> Toast.makeText(CallListActivity.this, "전화번호부 로드 실패", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> Toast.makeText(CallListActivity.this, "응답 실패: " + response.code(), Toast.LENGTH_SHORT).show());
                        return;
                    }
                    try {
                        String jsonResponse = response.body().string();
                        JSONArray jsonArray = new JSONArray(jsonResponse);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Long id = jsonObject.getLong("id");
                            String num = jsonObject.getString("number");
                            String name = jsonObject.getString("name");
                            phoneBookList.add(new PhoneBookList(id, num, name));
                        }
                        runOnUiThread(() -> {
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        });
                    } catch (JSONException e) {
                        Log.e("CallListActivity", "JSON 파싱 오류", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("CallListActivity", "HTTPS 설정 오류", e);
        }
    }

    private Cursor getCall() {
        String[] callSet = new String[]{CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.NUMBER, CallLog.Calls.DURATION};

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long startOfMonth = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, 1);
        long startOfNextMonth = calendar.getTimeInMillis();

        // 쿼리를 위한 선택 및 매개변수 설정
        String selection = CallLog.Calls.DATE + " >= ? AND " + CallLog.Calls.DATE + " < ?";
        String[] selectionArgs = {String.valueOf(startOfMonth), String.valueOf(startOfNextMonth)};

        // getContentResolver().query()를 사용하여 선택한 월의 전화 로그를 쿼리
        Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, callSet, selection, selectionArgs, null);
        return c;
    }

    private Cursor getCallByNum(String number) {
        String[] callSet = new String[]{CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.NUMBER, CallLog.Calls.DURATION};

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long startOfMonth = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, 1);
        long startOfNextMonth = calendar.getTimeInMillis();

        String selection = CallLog.Calls.DATE + " >= ? AND " + CallLog.Calls.DATE + " < ? AND " + CallLog.Calls.NUMBER + " = ?";
        String[] selectionArgs = {String.valueOf(startOfMonth), String.valueOf(startOfNextMonth), number};

        Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, callSet, selection, selectionArgs, null);
        return c;
    }

    private void deleteCall(int phoneBookId) {
        try {
            OkHttpClient client = getSecureOkHttpClient();
            HttpUrl.Builder urlBuilder = HttpUrl.parse("https://k10b201.p.ssafy.io/sagwa/api/phoneBook").newBuilder();
            urlBuilder.addPathSegment(String.valueOf(phoneBookId));
            String url = urlBuilder.build().toString();
            Request request = new Request.Builder().delete().url(url).build();
            client.newCall(request).enqueue(new okhttp3.Callback() {

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e("CallListActivity", "삭제 실패", e);
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        getPhoneBookList(() -> {
                            getCallGraph();
                        });
                        Toast.makeText(CallListActivity.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e("CallListActivity", "///// OkHttp Client Error /////", e);
        }
    }
}