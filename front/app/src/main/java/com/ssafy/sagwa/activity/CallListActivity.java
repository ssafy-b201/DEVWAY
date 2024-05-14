package com.ssafy.sagwa.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ssafy.sagwa.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

        TextView leftBtn = findViewById(R.id.btn_left);
        TextView rightBtn = findViewById(R.id.btn_right);
        headerMonth = findViewById(R.id.calendar_header_date);
        headerMonth.setText(month + "월");

        headerYear = findViewById(R.id.calendar_header_year);
        headerYear.setText(year + "년");
        listBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphBtn.setBackgroundTintList(getResources().getColorStateList(R.color.white));
                graphBtn.setImageTintList(getResources().getColorStateList(R.color.black));
                listBtn.setBackgroundTintList(getResources().getColorStateList(R.color.point));
                listBtn.setImageTintList(getResources().getColorStateList(R.color.white));
                getCallHistory(year,month);

                addBtn.setVisibility(View.GONE);
            }
        });

        graphBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listBtn.setBackgroundTintList(getResources().getColorStateList(R.color.white));
                listBtn.setImageTintList(getResources().getColorStateList(R.color.black));
                graphBtn.setBackgroundTintList(getResources().getColorStateList(R.color.point));
                graphBtn.setImageTintList(getResources().getColorStateList(R.color.white));
                getCallGraph();

                addBtn.setVisibility(View.VISIBLE);
            }
        });

        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCallAdd();

                addBtn.setVisibility(View.GONE);
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
            }
        });

        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (month==calendar.get(Calendar.MONTH) + 1 && year==calendar.get(Calendar.YEAR)){
                    Toast.makeText(CallListActivity.this, "현재 날짜 이후는 확인이 불가합니다.", Toast.LENGTH_SHORT).show();
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
            }
        });


        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, REQUEST_READ_CALL_LOG);
        } else {
            getCallHistory(year, month);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_CALL_LOG) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCallHistory(year, month);
            }
        }
    }


    @SuppressLint("SetTextI18n")
    public void getCallHistory(int year, int month) {
        callListContainer.removeAllViews();
        String[] callSet = new String[]{CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.NUMBER, CallLog.Calls.DURATION};

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1); // Calendar의 월은 0부터 시작하므로 1을 빼줍니다.
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long startOfMonth = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, 1);
        long startOfNextMonth = calendar.getTimeInMillis();

        // 쿼리를 위한 선택 및 매개변수 설정
        String selection = CallLog.Calls.DATE + " >= ? AND " + CallLog.Calls.DATE + " < ?";
        String[] selectionArgs = {String.valueOf(startOfMonth), String.valueOf(startOfNextMonth)};

        // getContentResolver().query()를 사용하여 선택한 월의 전화 로그를 쿼리
        Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, callSet, selection, selectionArgs, null);

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
                View callView = inflater.inflate(R.layout.sample_call_list_view, callListContainer, false);
                nameView = callView.findViewById(R.id.call_name);
                TextView timeView = callView.findViewById(R.id.call_time);
                TextView dateView = callView.findViewById(R.id.call_date);
                TextView doneView = callView.findViewById(R.id.call_done);

                long callDate = c.getLong(0);
                SimpleDateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd");
                String date_str = datePattern.format(new Date(callDate));

                nameView.setText(c.getString(2));
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

    public void getCallGraph() {
        callListContainer.removeAllViews();
        String[] callSet = new String[]{CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.NUMBER, CallLog.Calls.DURATION};
        int selectedMonth = 5;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, selectedMonth - 1); // Calendar의 월은 0부터 시작하므로 1을 빼줍니다.
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        long startOfMonth = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, 1);
        long startOfNextMonth = calendar.getTimeInMillis();

        // 쿼리를 위한 선택 및 매개변수 설정
        String selection = CallLog.Calls.DATE + " >= ? AND " + CallLog.Calls.DATE + " < ?";
        String[] selectionArgs = {String.valueOf(startOfMonth), String.valueOf(startOfNextMonth)};

        // getContentResolver().query()를 사용하여 선택한 월의 전화 로그를 쿼리
        Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, callSet, selection, selectionArgs, null);

        LayoutInflater inflater = LayoutInflater.from(this);

        TextView nameView;
        if (c == null || c.getCount() == 0) {
            View callView = inflater.inflate(R.layout.sample_call_graph_view, callListContainer, false);
            nameView = callView.findViewById(R.id.call_name);
            nameView.setText("통화 내역이 없습니다.");
            callListContainer.addView(callView);
            return;
        }

        if (c.moveToFirst()) {
            do {
                View callView = inflater.inflate(R.layout.sample_call_graph_view, callListContainer, false);
                nameView = callView.findViewById(R.id.call_name);

                TextView cntView = callView.findViewById(R.id.call_cnt);

                long callDate = c.getLong(0);
                SimpleDateFormat datePattern = new SimpleDateFormat("yyyy-MM-dd");
                String date_str = datePattern.format(new Date(callDate));

                nameView.setText(c.getString(2));

                cntView.setText("5번");
                callListContainer.addView(callView);
            } while (c.moveToNext());
        }
        c.close();
    }

    public void getCallAdd() {
        callListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        View callView = inflater.inflate(R.layout.sample_call_add_view, callListContainer, false);

        callListContainer.addView(callView);

    }
}