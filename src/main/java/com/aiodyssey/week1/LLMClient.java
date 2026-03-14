package com.aiodyssey.week1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

public class LLMClient {
    public static void main(String[] args) throws Exception{
        //Load API key
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

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }
}
