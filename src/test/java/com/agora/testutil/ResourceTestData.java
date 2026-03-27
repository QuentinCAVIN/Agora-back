package com.agora.testutil;

import java.util.List;

public final class ResourceTestData {

    private ResourceTestData() {}

    public static final List<String> IMAGE_URLS = List.of(
            "https://picsum.photos/800/600",
            "https://images.unsplash.com/photo-1506744038136-46273834b3fb",
            "https://images.unsplash.com/photo-1492724441997-5dc865305da7"
    );

    public static String randomImage() {
        return IMAGE_URLS.get((int) (Math.random() * IMAGE_URLS.size()));
    }
    public static String meetingRoomImage() {
        return "https://images.unsplash.com/photo-1497366216548-37526070297c";
    }

    public static String equipmentImage() {
        return "https://images.unsplash.com/photo-1581091870622-6c0f1c1b5b2d";
    }
}
