package dao;

import business.service.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Gestionnaire d'accès aux données SQLite via JDBC.
 */
public class SQLiteDAO {

    private static final Logger log = LoggerFactory.getLogger(SQLiteDAO.class);
    private static final String DB_URL = "jdbc:sqlite:tasks.db";

    public SQLiteDAO() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS task (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                done INTEGER NOT NULL DEFAULT 0
            );
        """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            log.info("Base de données SQLite initialisée par SQLiteDAO.");
        } catch (SQLException e) {
            log.error("Erreur à l'initialisation de la table", e);
            throw new RuntimeException(e);
        }
    }

    public Task save(Task task) {
        if (task.id() != null && findById(task.id()).isPresent()) {
            String sql = "UPDATE task SET title = ?, description = ?, done = ? WHERE id = ?;";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, task.title());
                ps.setString(2, task.description());
                ps.setInt(3, task.done() ? 1 : 0);
                ps.setInt(4, task.id());
                ps.executeUpdate();
                return task;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            String sql = "INSERT INTO task (title, description, done) VALUES (?, ?, ?);";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, task.title());
                ps.setString(2, task.description());
                ps.setInt(3, task.done() ? 1 : 0);
                ps.executeUpdate();

                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return new Task(generatedKeys.getInt(1), task.title(), task.description(), task.done());
                    }
                }
                return task;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Optional<Task> findById(int id) {
        String sql = "SELECT id, title, description, done FROM task WHERE id = ?;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Task(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getInt("done") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public Collection<Task> findAll(boolean todoOnly) {
        Collection<Task> tasks = new ArrayList<>();
        String sql = todoOnly ?
                "SELECT id, title, description, done FROM task WHERE done = 0;" :
                "SELECT id, title, description, done FROM task;";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getInt("done") == 1
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tasks;
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM task WHERE id = ?;";
        // CORRECTION ICI : Remplacement de URL par DB_URL
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearAll() {
        String sql = "DELETE FROM task;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM task;";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}