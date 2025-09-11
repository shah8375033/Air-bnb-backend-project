package com.project.airBnbApp.security;

import com.project.airBnbApp.dto.LoginResponseDto;
import com.project.airBnbApp.dto.SignUpRequestDto;
import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.entity.enums.AuthProviderType;
import com.project.airBnbApp.entity.enums.Role;
import com.project.airBnbApp.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Set;
@Service
@RequiredArgsConstructor
public class OAuth2Service {
    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ResponseEntity<LoginResponseDto> handleOAuth2LoginRequest(OAuth2User oAuth2User, String registrationId) {


        AuthProviderType providerType=jwtService.getProviderTypeFromRegistrationId(registrationId);
        String providerId=jwtService.determineProviderIdFromOAuth2User(oAuth2User,registrationId);


        User user=userRepository.findByProviderIdAndProviderType(providerId,providerType).orElse(null);
        String email=oAuth2User.getAttribute("email");

        User emailUser=userRepository.findByEmail(email).orElse(null);

        if (user==null && emailUser==null){
            //signUp flow:
            String signUpEmail=jwtService.determineEmailFromOAuth2User(oAuth2User,registrationId,providerId);
            String name=oAuth2User.getAttribute("name");
            user =oauth2SignUp(new SignUpRequestDto(signUpEmail,null,name),providerType,providerId);
        } else if (user != null) {
            if (email != null && !email.isBlank() && !email.equals(user.getEmail()) ) {
                user.setEmail(email);
                userRepository.save(user);
            }
        }
        else {
            throw new BadCredentialsException("This Email is Already register with provider "+emailUser.getProviderType());
        }

        LoginResponseDto loginResponseDto= new LoginResponseDto(jwtService.generateAccessToken(user));
        return ResponseEntity.ok(loginResponseDto);

        //fetch providerType and providerId
        //save the providerType and provider id info with user
        //if the user has an account :directly
        //otherwise ,first signUp and then login
    }
    public User oauth2SignUp(SignUpRequestDto signUpRequestDto,AuthProviderType authProviderType,String providerId) {
        User user = userRepository.findByEmail(signUpRequestDto.getEmail()).orElse(null);
        if(user  != null){
            throw new RuntimeException("Username already exists!");
        }
        User newUser=modelMapper.map(signUpRequestDto,User.class);
        newUser.setProviderId(providerId);
        newUser.setProviderType(authProviderType);
        newUser.setRoles(Set.of(Role.GUEST));
        return userRepository.save(newUser);
    }

}
