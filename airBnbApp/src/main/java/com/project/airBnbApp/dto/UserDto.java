package com.project.airBnbApp.dto;


import com.project.airBnbApp.entity.User;
import lombok.Data;
@Data
public class UserDto {
    private Long id;
    private String email;
    private String name;
    public  UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        return dto;
    }
}
