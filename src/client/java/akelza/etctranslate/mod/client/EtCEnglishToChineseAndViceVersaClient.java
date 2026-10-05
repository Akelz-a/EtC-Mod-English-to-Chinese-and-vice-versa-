package akelza.etctranslate.mod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import akelza.etctranslate.mod.EtCEnglishToChineseAndViceVersa;
import java.util.concurrent.CompletableFuture;

public class EtCEnglishToChineseAndViceVersaClient implements ClientModInitializer {

	private static boolean isSendingTranslation = false;
	private static boolean isInternalSend = false;

	@Override
	public void onInitializeClient() {

		// incoming messages (eg chinese -> english)
		ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, instant) -> {
			if (signedMessage != null) {
				Minecraft client = Minecraft.getInstance();

				// if the message was sent by ourselves, then dont need to translate it again
				if (client.player != null && sender != null && sender.id().equals(client.player.getGameProfile().id())) {
					return true; // showing the message as it is
				}

				// translation of others messages
				String originalText = signedMessage.signedContent();
				// a simple if/else statement. If we have a sender, use their name. Otherwise, use "Sender"
				String senderName = (sender != null) ? sender.name() : "Sender";
				String translatedText = EtCEnglishToChineseAndViceVersa.translateWithOllama(originalText, "English");

				if (client.player != null) {
					client.player.sendSystemMessage(
							Component.literal("§f<" + senderName + "> §a" + translatedText)
					);
				}
				return false;
			}
			return true;
		});

		// Our messages being translated. (language -> chinese)
		ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
			if (isSendingTranslation) {
				return true;
			}

			// before sending the message, we translate the message
			String translatedMessage = EtCEnglishToChineseAndViceVersa.translateWithOllama(message, "Chinese");

			Minecraft client = Minecraft.getInstance();
			if (client.player != null && client.player.connection != null) {
				isSendingTranslation = true;
				client.player.connection.sendChat(translatedMessage);
				isSendingTranslation = false;
			}
			return false; // do NOT send the original message
		});
		// for /msg
		ClientSendMessageEvents.ALLOW_COMMAND.register((command) -> {
			if (isInternalSend) {
				return true; // dont send our message
			}

			String[] parts = command.split(" ", 3);
			if (parts.length < 3) return true;

			String cmdName = parts[0].toLowerCase();

			if (cmdName.equals("tell") || cmdName.equals("msg") || cmdName.equals("w") || cmdName.equals("whisper")) {
				String targetPlayer = parts[1];
				String originalText = parts[2];

				CompletableFuture.runAsync(() -> {
					String translatedText = EtCEnglishToChineseAndViceVersa.translateWithOllama(originalText, "Chinese");

					var client = net.minecraft.client.Minecraft.getInstance();
					client.execute(() -> {
						if (client.player != null && client.player.connection != null) {
							isInternalSend = true;
							try {
								// to send commands in  MojMap, i use connection.sendCommand(...)
								// no slash added
								client.player.connection.sendCommand(cmdName + " " + targetPlayer + " " + translatedText);
							} finally {
								isInternalSend = false;
							}
						}
					});
				});

				return false; // cancel the original command
			}
			return true;
		});
	}
}