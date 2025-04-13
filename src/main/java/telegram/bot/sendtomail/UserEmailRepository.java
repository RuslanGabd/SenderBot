package telegram.bot.sendtomail;

import java.sql.*;

public class UserEmailRepository {
    private final Connection conn;

    public UserEmailRepository(String dbPath) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        createTableIfNotExists();
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS user_emails (
                    user_id INTEGER PRIMARY KEY,
                    email TEXT NOT NULL
                );
                """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void saveEmail(Long userId, String email) throws SQLException {
        String sql = "REPLACE INTO user_emails (user_id, email) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }

    public String getEmail(Long userId) throws SQLException {
        String sql = "SELECT email FROM user_emails WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("email") : null;
        }
    }
}
