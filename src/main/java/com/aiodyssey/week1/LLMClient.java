package com.aiodyssey.week1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import org.json.JSONObject;

public class LLMClient {

    // Gemini 2.5 Flash pricing per 1000 tokens
    private static final double INPUT_COST_PER_1K = 0.00015;
    private static final double OUTPUT_COST_PER_1K = 0.00060;

    private static HttpResponse<String> callWithRetry(HttpClient client, HttpRequest request, int maxRetries) throws Exception {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                // If rate limited, treat as failure
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
        properties.load(LLMClient.class.getResourceAsStream("/application.properties"));
        String apiKey = properties.getProperty("GEMINI_API_KEY");

        String body = """
            {
                "contents": [
                    {
                        "parts": [
                            {"text": "Explain RAG in one paragraph."}
                        ]
                    }
                ]
            }
        """;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response = callWithRetry(client, request, 3);
        JSONObject json = new JSONObject(response.body());

        // Extract answer
        String answer = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        // Extract token usage
        JSONObject usage = json.getJSONObject("usageMetadata");
        int inputTokens = usage.getInt("promptTokenCount");
        int outputTokens = usage.getInt("candidatesTokenCount");
        int totalTokens = usage.getInt("totalTokenCount");

        // Calculate cost
        double cost = (inputTokens / 1000.0 * INPUT_COST_PER_1K)
                + (outputTokens / 1000.0 * OUTPUT_COST_PER_1K);

        // Log everything
        System.out.println("=== RESPONSE ===");
        System.out.println(answer);
        System.out.println("\n=== TOKEN USAGE ===");
        System.out.printf("Input tokens  : %d%n", inputTokens);
        System.out.printf("Output tokens : %d%n", outputTokens);
        System.out.printf("Total tokens  : %d%n", totalTokens);
        System.out.printf("Cost          : $%.6f%n", cost);
    }
}