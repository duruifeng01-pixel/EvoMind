package com.evomind.api.integration;

import com.evomind.api.model.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AiSdkClient {
    public List<CardItem> buildCards(String userId) {
        return List.of(
                new CardItem(UUID.randomUUID().toString(), "科技博主A", "知乎", "从信息焦虑到行动闭环", "核心：输入筛选+日清行动。", true),
                new CardItem(UUID.randomUUID().toString(), "产品观察B", "公众号", "AI时代的学习节奏", "亮点：把输出作为唯一学习指标。", true)
        );
    }

    public MindmapResponse buildMindmap(String cardId) {
        return new MindmapResponse(cardId, "核心论点", List.of(
                new MindmapResponse.Node("n1", "输入源质量优先", "L1", false),
                new MindmapResponse.Node("n2", "行动反馈驱动迭代", "L1", true)
        ), "AI生成，仅供参考");
    }

    public DailyQuestionResponse dailyQuestion() {
        return new DailyQuestionResponse(UUID.randomUUID().toString(), "你今天愿意放弃哪一件低价值任务，换来30分钟深度学习？", "AI生成，仅供参考");
    }
}
