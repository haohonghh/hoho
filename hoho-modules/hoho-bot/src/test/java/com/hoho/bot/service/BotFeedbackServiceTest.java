package com.hoho.bot.service;

import java.util.ArrayList;
import java.util.List;

import com.hoho.bot.domain.BotFeedback;
import com.hoho.bot.domain.BotMessage;
import com.hoho.bot.model.request.BotFeedbackAcceptRequest;
import com.hoho.bot.mapper.BotFeedbackMapper;
import com.hoho.bot.mapper.BotMessageMapper;
import com.hoho.bot.model.request.BotFeedbackRequest;
import com.hoho.bot.model.request.BotFeedbackStatusRequest;
import com.hoho.common.core.context.SecurityContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BotFeedbackServiceTest
{
    @AfterEach
    void 清理用户上下文()
    {
        SecurityContextHolder.remove();
    }

    @Test
    void 提交纠错反馈时记录反馈人与目标助手消息()
    {
        SecurityContextHolder.setUserId("1001");
        SecurityContextHolder.setUserName("zhangsan");
        StubMessageMapper messageMapper = new StubMessageMapper();
        messageMapper.message = assistantMessage(10L, "session-001", 1001L);
        StubFeedbackMapper feedbackMapper = new StubFeedbackMapper();
        BotFeedbackService service = new BotFeedbackService(feedbackMapper, messageMapper, new BotUserContext());

        BotFeedbackRequest request = new BotFeedbackRequest();
        request.setConversationId("session-001");
        request.setMessageId(10L);
        request.setFeedbackType("correction");
        request.setReason("回答不够准确");
        request.setSuggestion("应该先检查 DNS 配置");
        service.submit(request);

        assertEquals("session-001", feedbackMapper.inserted.getConversationId());
        assertEquals(10L, feedbackMapper.inserted.getMessageId());
        assertEquals(1001L, feedbackMapper.inserted.getFeedbackUserId());
        assertEquals("zhangsan", feedbackMapper.inserted.getFeedbackUserName());
        assertEquals("correction", feedbackMapper.inserted.getFeedbackType());
        assertEquals("pending", feedbackMapper.inserted.getStatus());
    }

    @Test
    void Studio无登录态提交反馈时使用固定调试用户()
    {
        StubMessageMapper messageMapper = new StubMessageMapper();
        messageMapper.message = assistantMessage(10L, "session-001", 0L);
        StubFeedbackMapper feedbackMapper = new StubFeedbackMapper();
        BotFeedbackService service = new BotFeedbackService(feedbackMapper, messageMapper, new BotUserContext());

        BotFeedbackRequest request = new BotFeedbackRequest();
        request.setConversationId("session-001");
        request.setMessageId(10L);
        request.setFeedbackType("like");
        service.submit(request);

        assertEquals(0L, feedbackMapper.inserted.getFeedbackUserId());
        assertEquals("studio", feedbackMapper.inserted.getFeedbackUserName());
    }

    @Test
    void 不能对用户消息提交反馈()
    {
        StubMessageMapper messageMapper = new StubMessageMapper();
        messageMapper.message = assistantMessage(10L, "session-001", 0L);
        messageMapper.message.setRole("user");
        BotFeedbackService service = new BotFeedbackService(new StubFeedbackMapper(), messageMapper, new BotUserContext());

        BotFeedbackRequest request = new BotFeedbackRequest();
        request.setConversationId("session-001");
        request.setMessageId(10L);
        request.setFeedbackType("like");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.submit(request));
        assertEquals("只能对助手回答提交反馈", exception.getMessage());
    }

    @Test
    void 纠错反馈必须填写建议答案()
    {
        StubMessageMapper messageMapper = new StubMessageMapper();
        messageMapper.message = assistantMessage(10L, "session-001", 0L);
        BotFeedbackService service = new BotFeedbackService(new StubFeedbackMapper(), messageMapper, new BotUserContext());

        BotFeedbackRequest request = new BotFeedbackRequest();
        request.setConversationId("session-001");
        request.setMessageId(10L);
        request.setFeedbackType("correction");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.submit(request));
        assertEquals("纠错反馈的建议答案不能为空", exception.getMessage());
    }

    @Test
    void 可以查询当前用户反馈并更新状态()
    {
        StubMessageMapper messageMapper = new StubMessageMapper();
        StubFeedbackMapper feedbackMapper = new StubFeedbackMapper();
        BotFeedbackService service = new BotFeedbackService(feedbackMapper, messageMapper, new BotUserContext());

        assertEquals(feedbackMapper.feedbacks, service.list("pending"));

        BotFeedbackStatusRequest request = new BotFeedbackStatusRequest();
        request.setStatus("resolved");
        service.updateStatus(5L, request);

        assertEquals(0L, feedbackMapper.lastListUserId);
        assertEquals("pending", feedbackMapper.lastListStatus);
        assertEquals(5L, feedbackMapper.updated.getId());
        assertEquals(0L, feedbackMapper.updated.getFeedbackUserId());
        assertEquals("resolved", feedbackMapper.updated.getStatus());
    }

    @Test
    void 可以采纳纠错反馈并回灌知识库()
    {
        StubFeedbackMapper feedbackMapper = new StubFeedbackMapper();
        BotFeedback feedback = correctionFeedback();
        feedbackMapper.feedback = feedback;
        StubMessageMapper messageMapper = new StubMessageMapper();
        messageMapper.message = assistantMessage(10L, "session-001", 0L);
        messageMapper.previousUserMessage = userMessage(9L, "session-001", 0L, "电脑无法联网怎么办？");
        StubKbClient kbClient = new StubKbClient();
        BotFeedbackService service = new BotFeedbackService(feedbackMapper, messageMapper, new BotUserContext(),
                kbClient);

        BotFeedbackAcceptRequest request = new BotFeedbackAcceptRequest();
        request.setCategoryId(2L);
        Long qaId = service.acceptCorrection(5L, request);

        assertEquals(88L, qaId);
        assertEquals(2L, kbClient.lastCategoryId);
        assertEquals("电脑无法联网怎么办？", kbClient.lastQuestion);
        assertEquals("应该先检查 DNS 配置", kbClient.lastAnswer);
        assertEquals(5L, feedbackMapper.updated.getId());
        assertEquals(88L, feedbackMapper.updated.getAcceptedQaId());
        assertEquals("resolved", feedbackMapper.updated.getStatus());
    }

    @Test
    void 采纳纠错反馈时可以覆盖知识库问题()
    {
        StubFeedbackMapper feedbackMapper = new StubFeedbackMapper();
        feedbackMapper.feedback = correctionFeedback();
        StubMessageMapper messageMapper = new StubMessageMapper();
        messageMapper.message = assistantMessage(10L, "session-001", 0L);
        messageMapper.previousUserMessage = userMessage(9L, "session-001", 0L, "电脑无法联网怎么办？");
        StubKbClient kbClient = new StubKbClient();
        BotFeedbackService service = new BotFeedbackService(feedbackMapper, messageMapper, new BotUserContext(),
                kbClient);

        BotFeedbackAcceptRequest request = new BotFeedbackAcceptRequest();
        request.setCategoryId(2L);
        request.setQuestion("电脑突然断网怎么处理？");
        service.acceptCorrection(5L, request);

        assertEquals("电脑突然断网怎么处理？", kbClient.lastQuestion);
    }

    @Test
    void 只能采纳纠错类型反馈()
    {
        StubFeedbackMapper feedbackMapper = new StubFeedbackMapper();
        BotFeedback feedback = correctionFeedback();
        feedback.setFeedbackType("like");
        feedbackMapper.feedback = feedback;
        BotFeedbackService service = new BotFeedbackService(feedbackMapper, new StubMessageMapper(),
                new BotUserContext(), new StubKbClient());

        BotFeedbackAcceptRequest request = new BotFeedbackAcceptRequest();
        request.setCategoryId(2L);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.acceptCorrection(5L, request));
        assertEquals("只能采纳纠错反馈", exception.getMessage());
    }

    private BotMessage assistantMessage(Long id, String conversationId, Long userId)
    {
        BotMessage message = new BotMessage();
        message.setId(id);
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole("assistant");
        message.setContent("请先检查网线。");
        return message;
    }

    private BotMessage userMessage(Long id, String conversationId, Long userId, String content)
    {
        BotMessage message = new BotMessage();
        message.setId(id);
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setRole("user");
        message.setContent(content);
        return message;
    }

    private BotFeedback correctionFeedback()
    {
        BotFeedback feedback = new BotFeedback();
        feedback.setId(5L);
        feedback.setConversationId("session-001");
        feedback.setMessageId(10L);
        feedback.setFeedbackUserId(0L);
        feedback.setFeedbackUserName("studio");
        feedback.setFeedbackType("correction");
        feedback.setSuggestion("应该先检查 DNS 配置");
        feedback.setStatus("pending");
        return feedback;
    }

    private static class StubFeedbackMapper implements BotFeedbackMapper
    {
        private BotFeedback inserted;

        private BotFeedback updated;

        private BotFeedback feedback;

        private Long lastListUserId;

        private String lastListStatus;

        private final List<BotFeedback> feedbacks = List.of(new BotFeedback());

        @Override
        public int insertFeedback(BotFeedback feedback)
        {
            inserted = feedback;
            return 1;
        }

        @Override
        public List<BotFeedback> selectFeedbackList(Long feedbackUserId, String status)
        {
            lastListUserId = feedbackUserId;
            lastListStatus = status;
            return feedbacks;
        }

        @Override
        public BotFeedback selectFeedbackByIdAndUserId(Long id, Long feedbackUserId)
        {
            return feedback;
        }

        @Override
        public int updateFeedbackStatus(BotFeedback feedback)
        {
            updated = feedback;
            return 1;
        }
    }

    private static class StubMessageMapper implements BotMessageMapper
    {
        private BotMessage message;

        private BotMessage previousUserMessage;

        @Override
        public int insertMessage(BotMessage message)
        {
            return 1;
        }

        @Override
        public BotMessage selectMessageById(Long id)
        {
            return message;
        }

        @Override
        public BotMessage selectPreviousUserMessage(String conversationId, Long userId, Long beforeMessageId)
        {
            return previousUserMessage;
        }

        @Override
        public List<BotMessage> selectMessageList(String conversationId, Long userId)
        {
            return new ArrayList<>();
        }
    }

    private static class StubKbClient extends KbClient
    {
        private Long lastCategoryId;

        private String lastQuestion;

        private String lastAnswer;

        StubKbClient()
        {
            super(null);
        }

        @Override
        public Long createAndPublishQa(Long categoryId, String question, String answer, String similarQuestions)
        {
            lastCategoryId = categoryId;
            lastQuestion = question;
            lastAnswer = answer;
            return 88L;
        }
    }
}
