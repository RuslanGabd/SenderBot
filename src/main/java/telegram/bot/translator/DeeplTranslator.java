package telegram.bot.translator;

import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DeeplTranslator {

    private final String apiKey;

    public DeeplTranslator() {
        // Read from Railway environment variables
        this.apiKey = System.getenv("DEEPL_API_KEY");
    }

    public String translate(String text, String sourceLang, String targetLang) {
        try {
            URL url = new URL("https://api-free.deepl.com/v2/translate");

            String urlParams = String.format("auth_key=%s&text=%s&source_lang=%s&target_lang=%s",
                    URLEncoder.encode(apiKey, StandardCharsets.UTF_8),
                    URLEncoder.encode(text, StandardCharsets.UTF_8),
                    sourceLang.toUpperCase(),
                    targetLang.toUpperCase()
            );

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(urlParams.getBytes(StandardCharsets.UTF_8));
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = reader.lines().collect(Collectors.joining());
                JSONObject json = new JSONObject(response);
                return json.getJSONArray("translations")
                        .getJSONObject(0)
                        .getString("text");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Failed to translate.";
        }
    }
}
