package ai.berticloud.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CloudSqlPostgresTest {
    private static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void setup() {
        // Configura il DataSource per connettersi al Cloud SQL Auth Proxy locale
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://127.0.0.1:5432/subscript_manage_db"); // L'URL punta al proxy locale
        dataSource.setUsername("postgres"); // Il tuo utente DB
        dataSource.setPassword("\\,`~o@|mQ*dy#35;"); // La tua password DB

        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS test");

        // Assicurati che la tabella esista per il test
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS test.test_records (id SERIAL PRIMARY KEY, message VARCHAR(255), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    @Test
    void testInsertRecord() {
        String message = "Test record from JUnit " + System.currentTimeMillis();
        String sql = "INSERT INTO test.test_records (message) VALUES (?)";

        assertDoesNotThrow(() -> {
            jdbcTemplate.update(sql, message);
        });

        // Opzionale: verifica che il record sia stato inserito
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test.test_records WHERE message = ?", Integer.class, message);
        assertNotNull(count);
        // Asserisci che almeno un record con quel messaggio sia stato trovato
        assert count > 0;
    }
}
