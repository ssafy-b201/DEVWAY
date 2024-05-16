package com.ssafy.sagwa.api.PhoneBook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class PhoneBookList {
    private Long id;
    private String number;
    private String name;
}
