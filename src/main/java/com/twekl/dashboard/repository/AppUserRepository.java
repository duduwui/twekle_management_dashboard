package com.twekl.dashboard.repository;

import com.twekl.dashboard.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameEn(String usernameEn);
    Optional<AppUser> findByUsernameEnIgnoreCase(String usernameEn);
    boolean existsByUsernameEn(String usernameEn);
    boolean existsByUsernameEnIgnoreCase(String usernameEn);
    boolean existsByUsernameEnAndIdNot(String usernameEn, Long id);
    boolean existsByUsernameEnIgnoreCaseAndIdNot(String usernameEn, Long id);

    @Query("SELECT u FROM AppUser u WHERE " +
           "LOWER(u.usernameEn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.usernameAr) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.usernameKu) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "u.phoneNumber LIKE CONCAT('%', :query, '%')")
    List<AppUser> searchUsers(@Param("query") String query);
}
