package com.farmingmgt.system;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
@Theme(value = "Farming Mgt")
@Push(PushMode.AUTOMATIC) // Add the @Push annotation here
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public Application() { // Constructor to initialize Firebase
        initializeFirebase();
    }

    private void initializeFirebase() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount = getClass().getResourceAsStream("/serviceAccountKey.json");
                if (serviceAccount == null) {
                    throw new IllegalStateException("Service account key file not found in resources.");
                }

                FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).setDatabaseUrl("https://dema-solutions-default-rtdb.firebaseio.com").setStorageBucket("dema-solutions.appspot.com").build();

                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase has been initialized successfully.");

                // Test Firestore connection
                Firestore firestore = FirestoreClient.getFirestore();
                System.out.println("✅ Firestore connection established: " + (firestore != null));

                // Test Storage connection
                com.google.cloud.storage.Storage storage = com.google.cloud.storage.StorageOptions.newBuilder().setCredentials(GoogleCredentials.fromStream(getClass().getResourceAsStream("/serviceAccountKey.json"))).build().getService();
                System.out.println("✅ Storage connection established: " + (storage != null));
            } catch (IOException e) {
                System.err.println("❌ Error initializing Firebase: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}