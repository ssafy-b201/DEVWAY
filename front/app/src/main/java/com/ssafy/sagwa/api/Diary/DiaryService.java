package com.ssafy.sagwa.api.Diary;

import com.ssafy.sagwa.api.Member.Member;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DiaryService {

    @GET("diary/{date}")
    Call<DiaryDetailResDto> getDiaryByDate(@Path("date") String date, @Query("memberId") Long memberId);

}
