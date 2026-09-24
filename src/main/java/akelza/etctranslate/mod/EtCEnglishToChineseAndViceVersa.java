package akelza.etctranslate.mod;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class EtCEnglishToChineseAndViceVersa implements ModInitializer {
	public static final String MOD_ID = "etc-english-to-chinese-and-vice-versa";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	public static String translateWithOllama(String textToTranslate){
		//secure if Ollama fails
		try{
			String targetLanguage = "Chinese";
			//JSON letter for the AI
			String jsonInputStrings = "{"
					+ "\"model\": \"qwen2.5:3b\","
					+ "\"prompt\": \"Translate this Minecraft chat text to " + targetLanguage + ". Output ONLY the translation, nothing else, preserve player names and gaming slang: " + textToTranslate + "\","
					+ "\"stream\": false"
					+ "}";
			// HTTP request
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create("http://localhost:11434/api/generate"))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonInputStrings))
					.build();

			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

			//making JSON an object
			com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
			return jsonObject.get("response").getAsString();
		} catch (Exception e) {
			LOGGER.error("An error occured while trying to connect to Ollama: " + e.getMessage());
			return "Translation error";
		}
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
		ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, instant) -> {
			if (signedMessage != null) {
				String originalText = signedMessage.signedContent();

				String translatedText = translateWithOllama(originalText);
				LOGGER.info("Original: [" + originalText + "] ----> Translation: [" + translatedText + "]");


				//System.out.println("Mod caught a chat message: " + originalText);
			}
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
