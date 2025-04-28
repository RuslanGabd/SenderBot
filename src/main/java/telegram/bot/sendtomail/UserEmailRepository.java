package telegram.bot.sendtomail;





import telegram.bot.utils.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserEmailRepository {

    public UserEmailRepository() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.createStatement().executeUpdate("""
                CREATE TABLE IF NOT EXISTS user_emails (
                    id SERIAL PRIMARY KEY,
                    telegram_user_id BIGINT UNIQUE NOT NULL,
                    email TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT NOW(),
                    updated_at TIMESTAMP DEFAULT NOW()
                )
            """);
            // ❌ No need to create trigger in Java anymore!
        }
    }

    public void saveEmail(Long userId, String email) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO user_emails (telegram_user_id, email)
                VALUES (?, ?)
                ON CONFLICT (telegram_user_id)
                DO UPDATE SET email = EXCLUDED.email
            """);
            stmt.setLong(1, userId);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
    }

    public String getEmail(Long userId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("""
                SELECT email FROM user_emails
                WHERE telegram_user_id = ?
            """);
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("email");
            }
            return null;
        }
    }
}
