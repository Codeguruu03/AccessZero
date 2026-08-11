package com.accesszero.service;

import com.accesszero.domain.entity.UserEntity;
import com.accesszero.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IdentitySyncServiceTest {

    @Autowired
    private IdentitySyncService identitySyncService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSyncIdentity() {
        Map<String, Object> result = identitySyncService.syncIdentity("rahul.sharma");
        assertNotNull(result);
        assertEquals("SUCCESS", result.get("status"));
        assertEquals("rahul.sharma", result.get("username"));

        Optional<UserEntity> user = userRepository.findByUsername("rahul.sharma");
        assertTrue(user.isPresent());
        assertEquals("rahul.sharma@company.com", user.get().getEmail());
    }
}
