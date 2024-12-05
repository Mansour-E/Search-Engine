package Servlet;

import java.util.HashMap;
import java.util.Map;

public class RateLimiter {
    private static final int MAX_GLOBAL_REQUESTS_PER_SECOND = 10; // Max 10 Anfragen pro Sekunde insgesamt
    private static final int MAX_REQUESTS_PER_SECOND_PER_IP = 1;  // Max 1 Anfrage pro Sekunde pro IP

    private Map<String, Long> lastRequestTime = new HashMap<>();
    private Map<String, Integer> requestCountPerIp = new HashMap<>();
    private long globalLastRequestTime = 0;
    private int globalRequestCount = 0;

    public synchronized boolean isAllowed(String clientIp) {
        long currentTime = System.currentTimeMillis();

        // Global Rate-Limit überprüfen
        if (currentTime - globalLastRequestTime > 1000) {
            globalRequestCount = 0; // Reset global request count, falls 1 Sekunde vorbei ist
            globalLastRequestTime = currentTime;
        }
        if (globalRequestCount >= MAX_GLOBAL_REQUESTS_PER_SECOND) {
            return false; // Globales Limit überschritten
        }

        // IP-spezifisches Rate-Limit überprüfen
        long lastRequest = lastRequestTime.getOrDefault(clientIp, currentTime);
        if (currentTime - lastRequest > 1000) {
            requestCountPerIp.put(clientIp, 1); // Reset IP-spezifische Anfrageanzahl
            lastRequestTime.put(clientIp, currentTime);
        } else {
            int currentCount = requestCountPerIp.getOrDefault(clientIp, 0);
            if (currentCount >= MAX_REQUESTS_PER_SECOND_PER_IP) {
                return false; // IP-spezifisches Limit überschritten
            }
            requestCountPerIp.put(clientIp, currentCount + 1);
        }

        // Anfragen zählen und erlauben
        globalRequestCount++;
        return true;
    }
}
