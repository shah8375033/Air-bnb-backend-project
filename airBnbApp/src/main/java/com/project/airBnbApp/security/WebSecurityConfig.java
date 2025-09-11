package com.project.airBnbApp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.airBnbApp.advice.ApiError;
import com.project.airBnbApp.advice.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.time.LocalDateTime;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j

public class WebSecurityConfig {

    private final JWTAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

//    @Autowired
//    @Qualifier("handlerExceptionResolver")
//    private HandlerExceptionResolver  handlerExceptionResolver;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(csrfConfig -> csrfConfig.disable())
                .sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2Config->oauth2Config
                        .failureHandler(
                                (request, response, exception) -> {
                                    log.error("Oath2 error",exception.getMessage(), exception);
                                }
                        )
                        .successHandler(oAuth2SuccessHandler)

                )
                .authorizeHttpRequests(auth->auth
                        .requestMatchers("/admin/**").hasRole("HOTEL_MANAGER")
                        .requestMatchers("/bookings/**").authenticated()
                        .requestMatchers("/users/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(exHandlingConfig -> exHandlingConfig.accessDeniedHandler(accessDeniedHandler()));
        return httpSecurity.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    }
    @Bean
    public AccessDeniedHandler accessDeniedHandler(){
        return (request, response, accessDeniedException) -> {
            try {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");

                // Use your existing classes
                ApiError apiError = ApiError.builder()
                        .status(HttpStatus.FORBIDDEN)
                        .message(accessDeniedException.getMessage())
                        .build();

                ApiResponse<?> apiResponse = new ApiResponse<>();
                apiResponse.setTimeStamp(LocalDateTime.now());
                apiResponse.setData(null);
                apiResponse.setError(apiError);

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());

                String jsonResponse = objectMapper.writeValueAsString(apiResponse);
                response.getWriter().write(jsonResponse);
                response.getWriter().flush();

            } catch (Exception e) {
                System.out.println("Error in AccessDeniedHandler: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }


//    @Bean
//    public AccessDeniedHandler  accessDeniedHandler(){
//       return (request, response, accessDeniedException) ->
//               handlerExceptionResolver.resolveException(request , response , null , accessDeniedException);
//    }
    //    @Bean
//    public AccessDeniedHandler accessDeniedHandler(){
//        return new AccessDeniedHandler() {
//            @Override
//            public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
//                handlerExceptionResolver.resolveException(request,response,null,accessDeniedException);
//            }
//        };
//    }

}

