package com.evomind.api;

import com.evomind.api.store.InMemoryStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InMemoryStoreTest {

    @Test
    void shouldAddAndDeleteSource() {
        InMemoryStore store = new InMemoryStore();
        var created = store.addSource("u1", "知乎", "测试博主", "https://example.cn/u1");
        Assertions.assertEquals(1, store.getSources("u1").size());
        Assertions.assertTrue(store.removeSource("u1", created.id()));
        Assertions.assertEquals(0, store.getSources("u1").size());
    }

    @Test
    void shouldCompleteOnboarding() {
        InMemoryStore store = new InMemoryStore();
        Assertions.assertFalse(store.isOnboardingDone("u2"));
        store.completeOnboarding("u2");
        Assertions.assertTrue(store.isOnboardingDone("u2"));
    }
}
