package com.project.airBnbApp.dto;

import com.project.airBnbApp.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignUpRequestDto {
    private String email ;
    private String password;
    private String name;
}
