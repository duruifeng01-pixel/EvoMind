package com.evomind.api;

import com.evomind.api.model.OrderCreateRequest;
import com.evomind.api.model.SourceImportRequest;
import com.evomind.api.store.InMemoryStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class FlowStoreTest {

    @Test
    void shouldImportSourcesAndCreateOrder() {
        InMemoryStore store = new InMemoryStore();
        var imported = store.importSources(new SourceImportRequest(
                "u100",
                "知乎",
                List.of(
                        new SourceImportRequest.Item("博主1", "https://a.cn"),
                        new SourceImportRequest.Item("博主2", "https://b.cn")
                )
        ));
        Assertions.assertEquals(2, imported.size());

        var order = store.createOrder(new OrderCreateRequest("u100", "BASIC", "WECHAT", 12));
        Assertions.assertEquals("u100", order.userId());
        Assertions.assertEquals(1, store.orders("u100").size());
    }
}
