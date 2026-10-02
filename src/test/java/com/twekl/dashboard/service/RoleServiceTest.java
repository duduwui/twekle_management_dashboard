package com.twekl.dashboard.service;

import com.twekl.dashboard.modules.role.dto.RoleDto;
import com.twekl.dashboard.modules.role.entity.Role;
import com.twekl.dashboard.modules.role.repository.RoleRepository;
import com.twekl.dashboard.modules.role.service.RoleService;
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
        testRole = new Role("Support Specialist", false, true, false, false);
        testRole.setId(1L);
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
            r.setId(2L);
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
