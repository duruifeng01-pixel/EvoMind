package com.evomind.api.store;

import com.evomind.api.model.ChallengeTask;
import com.evomind.api.model.SourceItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryStore {
    private final Map<String, Boolean> onboardingDone = new ConcurrentHashMap<>();
    private final Map<String, List<SourceItem>> userSources = new ConcurrentHashMap<>();
    private final Map<String, ChallengeTask> userTask = new ConcurrentHashMap<>();

    public boolean isOnboardingDone(String userId) {
        return onboardingDone.getOrDefault(userId, false);
    }

    public void completeOnboarding(String userId) {
        onboardingDone.put(userId, true);
    }

    public List<SourceItem> getSources(String userId) {
        return userSources.computeIfAbsent(userId, k -> new ArrayList<>());
    }

    public SourceItem addSource(String userId, String platform, String nick, String link) {
        SourceItem item = new SourceItem(UUID.randomUUID().toString(), platform, nick, link, false, "默认", LocalDateTime.now().toString());
        getSources(userId).add(item);
        return item;
    }

    public boolean removeSource(String userId, String id) {
        return getSources(userId).removeIf(s -> s.id().equals(id));
    }

    public ChallengeTask getOrInitTask(String userId) {
        return userTask.computeIfAbsent(userId, uid -> new ChallengeTask(
                UUID.randomUUID().toString(),
                "入门",
                "使用AI整理本周工作复盘",
                "待开始",
                "10分钟内产出一页总结并保存到语料库",
                LocalDateTime.now().plusDays(1).toString()
        ));
    }

    public ChallengeTask updateTaskStatus(String userId, String status) {
        ChallengeTask old = getOrInitTask(userId);
        ChallengeTask n = new ChallengeTask(old.id(), old.stage(), old.title(), status, old.description(), old.deadline());
        userTask.put(userId, n);
        return n;
    }
}
