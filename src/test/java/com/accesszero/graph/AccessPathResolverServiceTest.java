package com.accesszero.graph;

import com.accesszero.domain.entity.AccessPathEntity;
import com.accesszero.domain.entity.UserEntity;
import com.accesszero.repository.UserRepository;
import com.accesszero.service.AccessPathResolverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AccessPathResolverServiceTest {

    @Autowired
    private AccessPathResolverService accessPathResolverService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testResolveAndPersistAccessPaths() {
        Optional<UserEntity> userOpt = userRepository.findByUsername("rahul.sharma");
        assertTrue(userOpt.isPresent());

        UserEntity user = userOpt.get();
        List<AccessPathEntity> paths = accessPathResolverService.resolveAndPersistAccessPaths(user.getId());

        assertNotNull(paths);
        assertFalse(paths.isEmpty());

        boolean hasGroupPath = paths.stream().anyMatch(p -> p.getPathType().name().equals("GROUP_INHERITED"));
        assertTrue(hasGroupPath, "Resolved paths should contain group inherited access paths");

        boolean hasPrivilegedPath = paths.stream().anyMatch(AccessPathEntity::isPrivileged);
        assertTrue(hasPrivilegedPath, "Resolved paths should surface privileged access paths");
    }
}
