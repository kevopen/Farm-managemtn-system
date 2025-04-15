package com.farmingmgt.system.Views;


import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MarketplaceService {

    public static CompletableFuture<List<Marketplace>> fetchMarketplaceItems() {
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection("marketplace")
                .whereEqualTo("status", "Active")
                .get();

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Marketplace> items = new ArrayList<>();
                List<QueryDocumentSnapshot> docs = future.get().getDocuments();
                for (QueryDocumentSnapshot doc : docs) {
                    Marketplace item = doc.toObject(Marketplace.class);
                    item.setId(doc.getId());
                    items.add(item);
                }
                return items;
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }
}
