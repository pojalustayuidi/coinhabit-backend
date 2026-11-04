package com.coinhabit.auth_service

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID
import javax.sql.DataSource

@SpringBootTest
class AuthMigrationIntegrationTest {
    @Autowired lateinit var dataSource: DataSource

    private fun legacySchema(collision: Boolean, check: (String) -> Unit) {
        val schema = "migration_" + UUID.randomUUID().toString().replace("-", "")
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE SCHEMA $schema")
                try {
                    statement.execute("""CREATE TABLE $schema.users (
                        id uuid PRIMARY KEY, email varchar(255) NOT NULL UNIQUE,
                        nickname varchar(255) NOT NULL, password_hash varchar(255) NOT NULL,
                        role varchar(255) NOT NULL, email_verified boolean)""")
                    statement.execute("""INSERT INTO $schema.users VALUES
                        ('00000000-0000-0000-0000-000000000001','Legacy@Example.com','Legacy','original-hash','USER',true),
                        ('00000000-0000-0000-0000-000000000002','unverified@example.com','Other','other-hash','USER',null)""")
                    if (collision) statement.execute("""INSERT INTO $schema.users VALUES
                        ('00000000-0000-0000-0000-000000000003','legacy@example.com','Duplicate','hash','USER',false)""")
                    check(schema)
                } finally {
                    statement.execute("DROP SCHEMA $schema CASCADE")
                }
            }
        }
    }

    private fun migrate(schema: String) = Flyway.configure().dataSource(dataSource)
        .schemas(schema).defaultSchema(schema).baselineOnMigrate(true).baselineVersion("0").load().migrate()

    @Test
    fun `existing users survive adoption and nullable verification becomes false`() {
        legacySchema(false) { schema ->
            migrate(schema)
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT * FROM $schema.users ORDER BY id").use { result ->
                        assertTrue(result.next())
                        assertEquals("00000000-0000-0000-0000-000000000001", result.getString("id"))
                        assertEquals("legacy@example.com", result.getString("email"))
                        assertEquals("original-hash", result.getString("password_hash"))
                        assertTrue(result.getBoolean("email_verified"))
                        assertFalse(result.getBoolean("is_guest"))
                        assertTrue(result.next())
                        assertFalse(result.getBoolean("email_verified"))
                        assertFalse(result.wasNull())
                        assertFalse(result.next())
                    }
                    statement.execute("""INSERT INTO $schema.users (id,nickname,role,is_guest)
                        VALUES ('${UUID.randomUUID()}','Guest','USER',true)""")
                }
            }
            assertEquals(0, migrate(schema).migrationsExecuted)
        }
    }

    @Test
    fun `case collisions stop migration without merging accounts`() {
        legacySchema(true) { schema ->
            assertThrows(FlywayException::class.java) { migrate(schema) }
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT count(*) FROM $schema.users").use { result ->
                        assertTrue(result.next())
                        assertEquals(3, result.getInt(1))
                    }
                }
            }
        }
    }
}
