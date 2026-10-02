package com.twekl.dashboard.modules.user.service;

import com.twekl.dashboard.modules.user.dto.CreateUserDto;
import com.twekl.dashboard.modules.user.dto.UpdateUserDto;
import com.twekl.dashboard.modules.user.entity.AppUser;
import com.twekl.dashboard.modules.user.entity.ModulePermission;
import com.twekl.dashboard.modules.user.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AppUserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public AppUser createUser(CreateUserDto dto) {
        if (userRepository.existsByUsernameEn(dto.getUsernameEn())) {
            throw new IllegalArgumentException("Username '" + dto.getUsernameEn() + "' is already taken");
        }

        AppUser user = new AppUser();
        user.setUsernameEn(dto.getUsernameEn().trim());
        user.setUsernameAr(dto.getUsernameAr() != null ? dto.getUsernameAr().trim() : null);
        user.setUsernameKu(dto.getUsernameKu() != null ? dto.getUsernameKu().trim() : null);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        if (dto.getPermissions() != null) {
            for (CreateUserDto.PermissionDto pDto : dto.getPermissions()) {
                ModulePermission perm = new ModulePermission(
                        pDto.getModuleKey(),
                        pDto.getModuleNameEn(),
                        pDto.getModuleNameAr(),
                        pDto.getModuleNameKu(),
                        pDto.isCanCreate(),
                        pDto.isCanRead(),
                        pDto.isCanUpdate(),
                        pDto.isCanDelete()
                );
                user.addPermission(perm);
            }
        }

        return userRepository.save(user);
    }

    public AppUser updateUser(Long id, UpdateUserDto dto) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        if (dto.getUsernameEn() != null && !dto.getUsernameEn().trim().isEmpty()) {
            String newUsername = dto.getUsernameEn().trim();
            if (!newUsername.equalsIgnoreCase(user.getUsernameEn()) && userRepository.existsByUsernameEn(newUsername)) {
                throw new IllegalArgumentException("Username '" + newUsername + "' is already taken");
            }
            user.setUsernameEn(newUsername);
        }

        if (dto.getUsernameAr() != null) user.setUsernameAr(dto.getUsernameAr().trim());
        if (dto.getUsernameKu() != null) user.setUsernameKu(dto.getUsernameKu().trim());
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());

        if (dto.getPermissions() != null) {
            user.getPermissions().clear();
            for (CreateUserDto.PermissionDto pDto : dto.getPermissions()) {
                ModulePermission perm = new ModulePermission(
                        pDto.getModuleKey(),
                        pDto.getModuleNameEn(),
                        pDto.getModuleNameAr(),
                        pDto.getModuleNameKu(),
                        pDto.isCanCreate(),
                        pDto.isCanRead(),
                        pDto.isCanUpdate(),
                        pDto.isCanDelete()
                );
                user.addPermission(perm);
            }
        }

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public long count() {
        return userRepository.count();
    }
}
