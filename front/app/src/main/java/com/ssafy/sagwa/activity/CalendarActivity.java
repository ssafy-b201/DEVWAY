package com.ssafy.sagwa.activity;

import static com.ssafy.sagwa.R.color.point;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ssafy.sagwa.R;
import com.ssafy.sagwa.api.Diary.DiaryDetailResDto;
import com.ssafy.sagwa.api.Diary.DiaryService;
import com.ssafy.sagwa.api.TrustOkHttpClientUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@RequiresApi(api = VERSION_CODES.O)
public class CalendarActivity extends AppCompatActivity {

    private String API_URL;
    private Long memberId;
    private int clickYear;
    private int clickMonth;
    private int clickDay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar);
        API_URL = getString(R.string.APIURL);

        // 로그인 정보
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
            getApplicationContext());
        memberId = sharedPref.getLong("loginId", 0);
        System.out.println("memberId: " + memberId);

        // 날짜 선택 안 할 경우 오늘 날짜 출력
        CalendarView calendarView = findViewById(R.id.calendar_calendar_calendar);
        long maxDate = System.currentTimeMillis();
        calendarView.setMaxDate(maxDate);

        calendarView.setFocusedMonthDateColor(getColor(R.color.point));
        LinearLayout headerView = findViewById(R.id.calendar_headerLayout);
        TextView yearView = headerView.findViewById(R.id.calendar_header_year);
        TextView dateView = headerView.findViewById(R.id.calendar_header_date);

        String today = String.valueOf(LocalDate.now());

        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        String day = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN);

        yearView.setText(today.substring(0, 4) + "년");
        dateView.setText(
            Integer.parseInt(today.substring(5, 7)) + "월 " + today.substring(8, 10) + "일 (" + day
                + ")");
        getDiaryOfClickDate(today);

        // 날짜 선택 시 해당 날짜 인증 출력
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month,
                                            int dayOfMonth) {
                clickYear = year;
                clickMonth = month + 1;
                clickDay = dayOfMonth;
                DayOfWeek dayOfWeekPrev = LocalDate.of(clickYear, clickMonth, clickDay)
                    .getDayOfWeek();
                String clickDayOfWeek = dayOfWeekPrev.getDisplayName(TextStyle.SHORT,
                    Locale.KOREAN);

                yearView.setText(clickYear + "년");
                dateView.setText(clickMonth + "월 " + clickDay + "일 (" + clickDayOfWeek + ")");

                getDiaryOfClickDate(String.valueOf(LocalDate.of(clickYear, clickMonth, clickDay)));
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.calendarActivity),
            (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });


    }

    public void getDiaryOfClickDate(String date) {
        ViewGroup diaryLayout = findViewById(R.id.calendar_diaryLayout);

        LayoutInflater inflater = getLayoutInflater();
        diaryLayout.removeAllViews();

        View calendardiaryView = inflater.inflate(R.layout.sample_calendar_diary, diaryLayout,
            false);
        TextView titleView = calendardiaryView.findViewById(R.id.calendarDiary_title);
        TextView contentView = calendardiaryView.findViewById(R.id.calendarDiary_content);
        TextView nullView = calendardiaryView.findViewById(R.id.calendarDiary_null);

        OkHttpClient client = TrustOkHttpClientUtil.getUnsafeOkHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build();

        Call<DiaryDetailResDto> call = retrofit.create(DiaryService.class)
            .getDiaryByDate(date, memberId);
        call.enqueue(new Callback<DiaryDetailResDto>() {
            @Override
            public void onResponse(Call<DiaryDetailResDto> call,
                                   Response<DiaryDetailResDto> response) {
                DiaryDetailResDto dto = response.body();
                runOnUiThread((new Runnable() {
                    @Override
                    public void run() {
                        if (dto != null) {
                            titleView.setText(dto.getDiaryTitle());
                            contentView.setText(dto.getDiaryContent());
                            nullView.setVisibility(View.GONE);
                        } else {
                            titleView.setVisibility(View.GONE);
                            contentView.setVisibility(View.GONE);
                        }
                        diaryLayout.addView(calendardiaryView);
                    }
                }));
            }

            @Override
            public void onFailure(Call<DiaryDetailResDto> call, Throwable t) {
                Toast.makeText(CalendarActivity.this, "다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

    }


}