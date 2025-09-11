package com.project.airBnbApp.security;

import com.project.airBnbApp.dto.LoginDto;
import com.project.airBnbApp.dto.SignUpRequestDto;
import com.project.airBnbApp.dto.UserDto;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.entity.enums.AuthProviderType;
import com.project.airBnbApp.entity.enums.Role;
import com.project.airBnbApp.exception.ResourceNotFoundException;
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

    public User oauth2SignUp(SignUpRequestDto signUpRequestDto,AuthProviderType authProviderType,String providerId) {
        User user = userRepository.findByEmail(signUpRequestDto.getEmail()).orElse(null);
        if(user  != null){
            throw new RuntimeException("Username already exists!");
        }
        User newUser=modelMapper.map(signUpRequestDto,User.class);
        newUser.setProviderId(providerId);
        newUser.setProviderType(authProviderType);
        newUser.setRoles(Set.of(Role.GUEST));
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));

        return userRepository.save(newUser);
    }
//SignUp Controller
    public UserDto signUp(SignUpRequestDto signUpRequestDto) {
        User user=oauth2SignUp(signUpRequestDto,AuthProviderType.EMAIL,null);
        return modelMapper.map(user,UserDto.class);
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

    public String refreshToken(String refreshToken) {
        Long userId=jwtService.getUserIdFromToken(refreshToken);
        User user=userRepository.findById(userId)
                .orElseThrow(()->new ResourceNotFoundException("User not found with id "+userId));
        return jwtService.generateAccessToken(user);
    }


}
