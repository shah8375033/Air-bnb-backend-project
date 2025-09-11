package com.project.airBnbApp.utils;
import com.project.airBnbApp.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

public class AppUtils {
    public static User getCurrentUser () {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
