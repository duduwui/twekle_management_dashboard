package com.twekl.dashboard.repository;

import com.twekl.dashboard.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
    Optional<Admin> findByUsernameIgnoreCase(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    @Query("SELECT a FROM Admin a WHERE " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "a.phoneNumber LIKE CONCAT('%', :query, '%')")
    List<Admin> searchAdmins(@Param("query") String query);
}
