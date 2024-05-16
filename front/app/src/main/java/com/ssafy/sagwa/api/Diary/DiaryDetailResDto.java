package com.ssafy.sagwa.api.Diary;


import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DiaryDetailResDto {
    private Long diaryId;
    private String diaryTitle;
    private String diaryImg;
    private String diaryContent;
    private String diaryWeather;

}
