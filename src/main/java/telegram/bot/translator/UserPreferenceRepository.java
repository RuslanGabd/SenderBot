package telegram.bot.translator;
import java.sql.*;

public class UserPreferenceRepository {

    private final Connection conn;

    public UserPreferenceRepository(String dbPath) throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        createTableIfNotExists();
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS user_preferences (
                user_id INTEGER PRIMARY KEY,
                source_lang TEXT,
                target_lang TEXT
            );
        """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void savePreferences(Long userId, String sourceLang, String targetLang) throws SQLException {
        String sql = "REPLACE INTO user_preferences (user_id, source_lang, target_lang) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, sourceLang);
            stmt.setString(3, targetLang);
            stmt.executeUpdate();
        }
    }

    public String getSourceLang(Long userId) throws SQLException {
        return getValue(userId, "source_lang");
    }

    public String getTargetLang(Long userId) throws SQLException {
        return getValue(userId, "target_lang");
    }

    private String getValue(Long userId, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM user_preferences WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString(1) : null;
        }
    }
}
