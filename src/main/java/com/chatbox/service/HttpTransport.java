package com.chatbox.service;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Functional interface abstracting HTTP request execution.
 * Allows deterministic unit testing without live network connections or real API keys.
 */
@FunctionalInterface
public interface HttpTransport {
    HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException;
}
