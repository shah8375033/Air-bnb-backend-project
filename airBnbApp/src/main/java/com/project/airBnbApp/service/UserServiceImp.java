package com.project.airBnbApp.service;

import com.project.airBnbApp.dto.ProfileUpdateRequestDto;
import com.project.airBnbApp.dto.UserDto;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.exception.ResourceNotFoundException;
import com.project.airBnbApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.project.airBnbApp.utils.AppUtils.getCurrentUser;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImp implements UserService , UserDetailsService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User not found with id:"+userId));
    }

    @Override
    public void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto) {
        User user=getCurrentUser();
        if(profileUpdateRequestDto.getDateOfbirth()!=null)user.setDateOfBirth(profileUpdateRequestDto.getDateOfbirth());
        if (profileUpdateRequestDto.getGender()!=null)user.setGender(profileUpdateRequestDto.getGender());
        if (profileUpdateRequestDto.getName()!=null)user.setName(profileUpdateRequestDto.getName());
        userRepository.save(user);
    }

    @Override
    public UserDto getMyProfile() {
        User user=getCurrentUser();
        log.info("Getting the profile for user with id:{}",user.getId());
        return modelMapper.map(user,UserDto.class);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmail(username).orElse(null);
    }
}
