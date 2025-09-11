package com.project.airBnbApp.repository;

import com.project.airBnbApp.entity.User;
import com.project.airBnbApp.entity.enums.AuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
   Optional<User> findByEmail(String email);

    Optional<User> findByProviderIdAndProviderType(String providerId, AuthProviderType providerType);
}
