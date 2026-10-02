package com.twekl.dashboard.modules.user.repository;

import com.twekl.dashboard.modules.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameEn(String usernameEn);
    boolean existsByUsernameEn(String usernameEn);
}
