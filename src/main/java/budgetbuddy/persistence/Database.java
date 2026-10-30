package budgetbuddy.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns a JDBC connection to a SQLite database and creates the schema on open.
 * Tests should use {@link #inMemory()} so they never touch the file database.
 */
public final class Database implements AutoCloseable {
    private final Connection connection;

    private Database(String url) {
        try {
            connection = DriverManager.getConnection(url);
            connection.setAutoCommit(true);
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON");
            }
            createSchema();
        } catch (SQLException e) {
            throw new PersistenceException("Could not open SQLite database: " + url, e);
        }
    }

    public static Database inMemory() {
        return new Database("jdbc:sqlite::memory:");
    }

    public static Database file(Path path) {
        return new Database("jdbc:sqlite:" + path.toAbsolutePath());
    }

    public Connection connection() {
        return connection;
    }

    private void createSchema() {
        String sql = readSchema();
        try (Statement statement = connection.createStatement()) {
            for (String part : sql.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    statement.executeUpdate(trimmed);
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not create schema", e);
        }
    }

    private static String readSchema() {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new PersistenceException("schema.sql is missing from the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenceException("Could not read schema.sql", e);
        }
    }

    @Override
    public void close() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not close database", e);
        }
    }
}
