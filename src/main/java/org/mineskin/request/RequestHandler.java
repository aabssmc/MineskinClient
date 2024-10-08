package org.mineskin.request;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.mineskin.MineSkinClientImpl;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.ResponseConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RequestHandler {

    private final Gson gson;
    private final HttpClient httpClient;
    protected final String userAgent;
    protected final String apiKey;

    public RequestHandler(
            String userAgent, String apiKey,
            int timeout,
            Gson gson) {
        this.userAgent = userAgent;
        this.apiKey = apiKey;
        this.gson = gson;
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(timeout));
        if (userAgent != null) {
            clientBuilder.followRedirects(HttpClient.Redirect.NORMAL);
        }
        this.httpClient = clientBuilder.build();
    }

    public String getApiKey() {
        return apiKey;
    }

    private <T, R extends MineSkinResponse<T>> R wrapResponse(HttpResponse<String> response, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        String rawBody = response.body();
        try {
            JsonObject jsonBody = gson.fromJson(rawBody, JsonObject.class);
            R wrapped = constructor.construct(
                    response.statusCode(),
                    lowercaseHeaders(response.headers().map()),
                    jsonBody,
                    gson, clazz
            );
            if (!wrapped.isSuccess()) {
                throw new MineSkinRequestException(wrapped.getError().orElse("Request Failed"), wrapped);
            }
            return wrapped;
        } catch (JsonParseException e) {
            MineSkinClientImpl.LOGGER.log(Level.WARNING, "Failed to parse response body: " + rawBody, e);
            throw new MineskinException("Failed to parse response", e);
        }
    }

    private Map<String, String> lowercaseHeaders(Map<String, java.util.List<String>> headers) {
        return headers.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> String.join(", ", entry.getValue())
                ));
    }

    public <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent", this.userAgent);
        HttpRequest request;
        if (apiKey != null) {
            request = requestBuilder
                    .header("Authorization", "Bearer "+apiKey)
                    .header("Accept", "application/json").build();
        } else {
            request = requestBuilder.build();
        }
        HttpResponse<String> response = null;
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return wrapResponse(response, clazz, constructor);
    }

    public <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(BodyPublishers.ofString(gson.toJson(data)))
                .header("Content-Type", "application/json")
                .header("User-Agent", this.userAgent);
        HttpRequest request;
        if (apiKey != null) {
            request = requestBuilder
                    .header("Authorization", "Bearer "+apiKey)
                    .header("Accept", "application/json").build();
        } else {
            request = requestBuilder.build();
        }

        HttpResponse<String> response = null;
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return wrapResponse(response, clazz, constructor);
    }

    public <T, R extends MineSkinResponse<T>> R postFormDataFile(String url, String key, String filename, InputStream in, Map<String, String> data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {

        String boundary = "mineskin-" + System.currentTimeMillis();
        byte[] fileContent = in.readAllBytes();
        String bodyBuilder = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n" +
                "Content-Type: application/octet-stream\r\n\r\n";
        byte[] bodyStart = bodyBuilder.getBytes();
        byte[] boundaryEnd = ("\r\n--" + boundary + "--\r\n").getBytes();
        byte[] bodyString = new byte[bodyStart.length + fileContent.length + boundaryEnd.length];
        System.arraycopy(bodyStart, 0, bodyString, 0, bodyStart.length);
        System.arraycopy(fileContent, 0, bodyString, bodyStart.length, fileContent.length);
        System.arraycopy(boundaryEnd, 0, bodyString, bodyStart.length + fileContent.length, boundaryEnd.length);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyString))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("User-Agent", this.userAgent);
        HttpRequest request;
        if (apiKey != null) {
            request = requestBuilder
                    .header("Authorization", "Bearer "+apiKey)
                    .header("Accept", "application/json").build();
        } else {
            request = requestBuilder.build();
        }

        HttpResponse<String> response = null;
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return wrapResponse(response, clazz, constructor);
    }

}
