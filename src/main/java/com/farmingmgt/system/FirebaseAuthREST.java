package com.farmingmgt.system;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FirebaseAuthREST {

    private static final String FIREBASE_API_KEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"; // Replace with your Firebase Web API Key

    public static boolean verifyUser(String email, String password) {
        try {
            URL url = new URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FIREBASE_API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = String.format("{\"email\":\"%s\",\"password\":\"%s\",\"returnSecureToken\":true}", email, password);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                return true; // Login successful
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false; // Invalid login
    }
}
