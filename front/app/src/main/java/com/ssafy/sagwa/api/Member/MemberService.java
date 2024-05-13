package com.ssafy.sagwa.api.Member;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MemberService {

    @GET("signin")
    Call<Member> getMemberByEmail(@Query("email") String email);

}
