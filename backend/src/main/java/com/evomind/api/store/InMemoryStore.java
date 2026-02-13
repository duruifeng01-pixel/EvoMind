package com.evomind.api.store;

import com.evomind.api.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryStore {
    private final Map<String, Boolean> onboardingDone = new ConcurrentHashMap<>();
    private final Map<String, List<SourceItem>> userSources = new ConcurrentHashMap<>();
    private final Map<String, ChallengeTask> userTask = new ConcurrentHashMap<>();
    private final Map<String, List<OrderItem>> userOrders = new ConcurrentHashMap<>();

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

    public List<SourceItem> importSources(SourceImportRequest req) {
        List<SourceItem> added = new ArrayList<>();
        for (SourceImportRequest.Item it : req.items()) {
            added.add(addSource(req.userId(), req.platform(), it.nickname(), it.homepage()));
        }
        return added;
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

    public List<CardItem> feed(String userId) {
        return List.of(
                new CardItem(UUID.randomUUID().toString(), "科技博主A", "知乎", "AI工作流的三层架构", "核心观点：先固化输入质量，再做自动化。", true),
                new CardItem(UUID.randomUUID().toString(), "产品观察B", "公众号", "避免信息焦虑的实践框架", "亮点：48小时半衰期策略降低信息堆积。", true)
        );
    }

    public MindmapResponse mindmap(String cardId) {
        return new MindmapResponse(
                cardId,
                "核心论点",
                List.of(
                        new MindmapResponse.Node("n1", "输入源质量决定输出上限", "L1", false),
                        new MindmapResponse.Node("n2", "建立反馈闭环缩短成长周期", "L1", true)
                ),
                "AI生成，仅供参考"
        );
    }

    public DrilldownResponse drilldown(String cardId, String nodeId) {
        return new DrilldownResponse(cardId, nodeId,
                "这是与节点关联的原文段落（临时抓取示例，不落地存储）。",
                "AI生成，仅供参考");
    }

    public DailyQuestionResponse dailyQuestion() {
        return new DailyQuestionResponse(UUID.randomUUID().toString(), "如果今天只能改一个习惯，哪个最能提升你的执行力？", "AI生成，仅供参考");
    }

    public DiscussionReplyResponse followUp(String discussionId, String answer) {
        return new DiscussionReplyResponse(discussionId, "你提到了" + answer + "，请给出一个明天就能执行的具体动作。", "AI生成，仅供参考");
    }

    public DiscussionFinalizeResponse finalizeDiscussion(String discussionId) {
        return new DiscussionFinalizeResponse(discussionId, "你已形成可执行策略：每天固定30分钟复盘+输出。", "如何持续8周不间断？", "AI生成，仅供参考");
    }

    public OrderItem createOrder(OrderCreateRequest req) {
        OrderItem item = new OrderItem(
                "OD" + System.currentTimeMillis(),
                req.userId(),
                req.planCode(),
                req.channel(),
                req.amount(),
                "PAID",
                LocalDateTime.now().toString());
        userOrders.computeIfAbsent(req.userId(), k -> new ArrayList<>()).add(item);
        return item;
    }

    public List<OrderItem> orders(String userId) {
        return userOrders.getOrDefault(userId, List.of());
    }
}
