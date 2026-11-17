package com.project.urlshortener.service;

public interface ClickCountBufferService {
    void increment(String shortLink);

    void flushToDatabase();

    void shutdown();

    long getBufferedCount(String shortLink);
}
