package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.ProfileUpdateRequestDto;
import com.project.airBnbApp.dto.UserDto;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.repository.UserRepository;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

public interface UserService{
    User getUserById(Long userId) ;

    void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto);

    UserDto getMyProfile();
}
