package com.project.airBnbApp.security;

import com.project.airBnbApp.dto.LoginDto;
import com.project.airBnbApp.dto.SignUpRequestDto;
import com.project.airBnbApp.dto.UserDto;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.entity.enums.Role;
import com.project.airBnbApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public UserDto signUp(SignUpRequestDto signUpRequestDto) {
        User user = userRepository.findByEmail(signUpRequestDto.getEmail()).orElse(null);
        if(user  != null){
            throw new RuntimeException("Username already exists!");
        }
        User newUser=modelMapper.map(signUpRequestDto,User.class);
        newUser.setRoles(Set.of(Role.GUEST));
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        newUser=userRepository.save(newUser);
        return modelMapper.map(newUser,UserDto.class);
    }
    public String[] login(LoginDto loginDto) {
        Authentication authenticate=authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(), loginDto.getPassword()
        ));
        User user=(User)authenticate.getPrincipal();
        String[] arr=new String[2];
        arr[0]=jwtService.generateAccessToken(user);
        arr[1]=jwtService.generateRefreshToken(user);
        return arr;
    }
}
