package akelza.etctranslate.mod;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class EtCEnglishToChineseAndViceVersa implements ModInitializer {

	public static final String MOD_ID = "etc-english-to-chinese-and-vice-versa";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// main initialization, the listeners are in client file
		LOGGER.info("EtC Mod main initialization complete!");
	}

	// Универсальный метод перевода с двумя параметрами
	public static String translateWithOllama(String textToTranslate, String targetLanguage) {
		try {
			// special characaters for json
			String safeText = textToTranslate.replace("\\", "\\\\")
					.replace("\"", "\\\"")
					.replace("\n", " ");

			// prompt
			String systemInstruction = "You are a professional translator for Minecraft chat. "
					+ "Translate the user message strictly into " + targetLanguage + ". "
					+ "Do not leave any raw English words in the output unless it is a proper noun or player name (like Dream). "
					+ "Correct typos in slang (e.g. 'leep' -> 'like') and translate naturally into " + targetLanguage + ". "
					+ "Output ONLY the translation, nothing else.";

			// adding options, like temp (its for creativity)
			String jsonInputStrings = "{"
					+ "\"model\": \"qwen2.5:3b\","
					+ "\"messages\": ["
					+ "  {\"role\": \"system\", \"content\": \"" + systemInstruction + "\"},"
					+ "  {\"role\": \"user\", \"content\": \"" + safeText + "\"}"
					+ "],"
					+ "\"options\": {"
					+ "  \"temperature\": 0.1,"
					+ "  \"top_p\": 0.2"
					+ "},"
					+ "\"stream\": false"
					+ "}";

			URL url = new URL("http://localhost:11434/api/chat");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json; utf-8");
			conn.setDoOutput(true);

			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = jsonInputStrings.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			StringBuilder response = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
				String responseLine;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
			}

			String json = response.toString();
			int contentIndex = json.indexOf("\"content\":");
			if (contentIndex != -1) {
				int startIndex = json.indexOf("\"", contentIndex + 10) + 1;
				int endIndex = json.indexOf("\"", startIndex);
				if (startIndex > 0 && endIndex > startIndex) {
					String result = json.substring(startIndex, endIndex)
							.replace("\\n", "\n")
							.replace("\\\"", "\"")
							.replace("\\\\", "\\")
							.trim();

					return result.isEmpty() ? textToTranslate : result;
				}
			}
			return textToTranslate;

		} catch (Exception e) {
			e.printStackTrace();
			return textToTranslate;
		}
	}
}