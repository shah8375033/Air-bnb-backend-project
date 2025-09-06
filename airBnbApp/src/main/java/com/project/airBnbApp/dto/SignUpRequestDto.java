package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.enums.Role;
import lombok.Data;

@Data
public class SignUpRequestDto {
    private String email ;
    private String password;
    private String name;
}
