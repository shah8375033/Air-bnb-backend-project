package com.project.airBnbApp.dto;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.entity.enums.Gender;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import static com.project.airBnbApp.utils.AppUtils.getCurrentUser;

@Data
@RequiredArgsConstructor
public class GuestDto {
    private Long id;
    private UserDto userDto=new UserDto().from(getCurrentUser());
    private String name;
    private Gender gender;
    private Integer age;
}
