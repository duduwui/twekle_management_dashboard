package com.twekl.dashboard.service.admin;

import com.twekl.dashboard.dto.role.RoleDto;
import com.twekl.dashboard.model.role.Role;
import com.twekl.dashboard.repository.role.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private RoleService roleService;
    private Role testRole;

    @BeforeEach
    public void setUp() {
        roleService = new RoleService(roleRepository);
        testRole = new Role("Support Specialist", false, true, false, false);
        testRole.setRoleId(1L);
    }

    @Test
    @DisplayName("Should return all role templates")
    public void testFindAll() {
        when(roleRepository.findAll()).thenReturn(List.of(testRole));

        List<Role> roles = roleService.findAll();

        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("Support Specialist", roles.get(0).getName());
        verify(roleRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should find role template by ID")
    public void testFindById_Success() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

        Optional<Role> found = roleService.findById(1L);

        assertTrue(found.isPresent());
        assertEquals("Support Specialist", found.get().getName());
        verify(roleRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should create a new role template successfully")
    public void testCreateRole_Success() {
        RoleDto dto = new RoleDto("Sales Executive", true, true, true, false);

        when(roleRepository.existsByName("Sales Executive")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role r = invocation.getArgument(0);
            r.setRoleId(2L);
            return r;
        });

        Role created = roleService.createRole(dto);

        assertNotNull(created);
        assertEquals("Sales Executive", created.getName());
        assertTrue(created.isCanCreate());
        assertTrue(created.isCanRead());
        assertTrue(created.isCanUpdate());
        assertFalse(created.isCanDelete());
        verify(roleRepository, times(1)).save(any(Role.class));
    }
}
