package folk.sisby.switchy;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebhookIntegration {
	private final String url;

	public WebhookIntegration(String url) {
		this.url = url;
	}

	public void sendMessage(String content) {
		try {
			URL webhookUrl = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) webhookUrl.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");

			String jsonPayload = "{\"content\":\"" + content.replace("\"", "\\\"") + "\"}";

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			int responseCode = connection.getResponseCode();
			if (responseCode != 204) {
				System.out.println("Discord webhook failed: " + responseCode);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
