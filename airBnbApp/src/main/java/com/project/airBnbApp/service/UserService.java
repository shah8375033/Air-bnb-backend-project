package com.project.airBnbApp.service;

import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.repository.UserRepository;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

public interface UserService{
    User getUserById(Long userId) ;

}
