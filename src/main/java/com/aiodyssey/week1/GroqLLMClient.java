package com.aiodyssey.week1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import org.json.JSONObject;

public class GroqLLMClient {

    private static final double INPUT_COST_PER_1K = 0.0;
    private static final double OUTPUT_COST_PER_1K = 0.0;

    private static HttpResponse<String> callWithRetry(HttpClient client, HttpRequest request, int maxRetries) throws Exception {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 429 || response.statusCode() >= 500) {
                    throw new RuntimeException("API error: " + response.statusCode());
                }
                return response;
            } catch (Exception e) {
                attempt++;
                if (attempt == maxRetries) throw e;
                long waitTime = (long) Math.pow(2, attempt) * 1000;
                System.out.printf("Attempt %d failed. Retrying in %d seconds...%n", attempt, waitTime / 1000);
                Thread.sleep(waitTime);
            }
        }
        throw new RuntimeException("All retries exhausted");
    }

    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.load(GroqLLMClient.class.getResourceAsStream("/application.properties"));
        String apiKey = properties.getProperty("GROQ_API_KEY");

        String body = """
            {
                "messages": [
                    {
                        "role": "user",
                        "content": "Explain RAG in one paragraph."
                    }
                ],
                "model": "llama-3.3-70b-versatile",
                "temperature": 1,
                "max_tokens": 1024,
                "top_p": 1,
                "stream": false
            }
        """;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = callWithRetry(client, request, 3);
        JSONObject json = new JSONObject(response.body());

        String answer = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        JSONObject usage = json.getJSONObject("usage");
        int inputTokens = usage.getInt("prompt_tokens");
        int outputTokens = usage.getInt("completion_tokens");
        int totalTokens = usage.getInt("total_tokens");

        double cost = (inputTokens / 1000.0 * INPUT_COST_PER_1K)
                + (outputTokens / 1000.0 * OUTPUT_COST_PER_1K);

        System.out.println("=== RESPONSE ===");
        System.out.println(answer);
        System.out.println("\n=== TOKEN USAGE ===");
        System.out.printf("Input tokens  : %d%n", inputTokens);
        System.out.printf("Output tokens : %d%n", outputTokens);
        System.out.printf("Total tokens  : %d%n", totalTokens);
        System.out.printf("Cost          : $%.6f%n", cost);
    }
}