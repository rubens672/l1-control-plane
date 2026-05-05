package ai.berticloud.admin.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import ai.berticloud.admin.api.AdminController;
import ai.berticloud.admin.security.BootstrapTokenIssuer;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class AdminDatabaseTest {

    @MockBean
    AdminController adminController;

    @MockBean
    BootstrapTokenIssuer bootstrapTokenIssuer;

    @MockBean
    MongoTemplate mongoTemplate;

    @Autowired
    AdminRepository adminRepository;

    @Test
    void testRepositoryLoads() {
        assertNotNull(adminRepository);
    }
}
