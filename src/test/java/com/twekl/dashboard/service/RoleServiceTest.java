package com.twekl.dashboard.service;

import com.twekl.dashboard.model.Role;
import com.twekl.dashboard.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleService roleService;

    private Role testRole;

    @BeforeEach
    public void setUp() {
        testRole = Role.builder()
                .id(1L)
                .name("Support Specialist")
                .canRead(true)
                .canCreate(false)
                .canUpdate(true)
                .canDelete(false)
                .build();
    }

    @Test
    @DisplayName("getAllRoles should return list of roles from repository")
    public void testGetAllRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(testRole));

        List<Role> roles = roleService.getAllRoles();

        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("Support Specialist", roles.get(0).getName());
        verify(roleRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("createRole should save and return new role")
    public void testCreateRole() {
        when(roleRepository.save(any(Role.class))).thenReturn(testRole);

        Role created = roleService.createRole(testRole);

        assertNotNull(created);
        assertEquals("Support Specialist", created.getName());
        verify(roleRepository, times(1)).save(testRole);
    }

    @Test
    @DisplayName("toggleRolePermission should update specified boolean field")
    public void testToggleRolePermission() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Role updated = roleService.toggleRolePermission(1L, "canDelete", true);

        assertNotNull(updated);
        assertTrue(updated.isCanDelete());
        verify(roleRepository, times(1)).findById(1L);
        verify(roleRepository, times(1)).save(testRole);
    }
}
