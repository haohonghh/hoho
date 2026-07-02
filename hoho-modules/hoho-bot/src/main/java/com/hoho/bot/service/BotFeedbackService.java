package com.hoho.bot.service;

import java.util.List;
import java.util.Set;

import com.hoho.bot.domain.BotFeedback;
import com.hoho.bot.domain.BotMessage;
import com.hoho.bot.mapper.BotFeedbackMapper;
import com.hoho.bot.mapper.BotMessageMapper;
import com.hoho.bot.model.request.BotFeedbackAcceptRequest;
import com.hoho.bot.model.request.BotFeedbackRequest;
import com.hoho.bot.model.request.BotFeedbackStatusRequest;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 机器人回答反馈服务
 *
 * @author hoho
 */
@Service
public class BotFeedbackService
{
    private static final String ROLE_ASSISTANT = "assistant";

    private static final String TYPE_CORRECTION = "correction";

    private static final String STATUS_PENDING = "pending";

    private static final String STATUS_RESOLVED = "resolved";

    private static final Set<String> FEEDBACK_TYPES = Set.of("like", "dislike", TYPE_CORRECTION);

    private static final Set<String> FEEDBACK_STATUS = Set.of(STATUS_PENDING, "reviewed", "resolved", "ignored");

    private final BotFeedbackMapper feedbackMapper;

    private final BotMessageMapper messageMapper;

    private final BotUserContext botUserContext;

    private final KbClient kbClient;

    public BotFeedbackService(BotFeedbackMapper feedbackMapper, BotMessageMapper messageMapper,
            BotUserContext botUserContext)
    {
        this(feedbackMapper, messageMapper, botUserContext, null);
    }

    @Autowired
    public BotFeedbackService(BotFeedbackMapper feedbackMapper, BotMessageMapper messageMapper,
            BotUserContext botUserContext, KbClient kbClient)
    {
        this.feedbackMapper = feedbackMapper;
        this.messageMapper = messageMapper;
        this.botUserContext = botUserContext;
        this.kbClient = kbClient;
    }

    /**
     * 提交当前用户对助手回答的反馈。
     *
     * @param request 反馈请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void submit(BotFeedbackRequest request)
    {
        validateSubmitRequest(request);
        BotUserContext.CurrentUser currentUser = botUserContext.currentUser();
        BotMessage message = messageMapper.selectMessageById(request.getMessageId());
        validateTargetMessage(request, message, currentUser.getUserId());

        BotFeedback feedback = new BotFeedback();
        feedback.setConversationId(request.getConversationId());
        feedback.setMessageId(request.getMessageId());
        feedback.setFeedbackUserId(currentUser.getUserId());
        feedback.setFeedbackUserName(currentUser.getUserName());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setReason(request.getReason());
        feedback.setSuggestion(request.getSuggestion());
        feedback.setStatus(STATUS_PENDING);
        feedbackMapper.insertFeedback(feedback);
    }

    /**
     * 查询当前用户提交的反馈。
     *
     * @param status 反馈状态，可为空
     * @return 反馈列表
     */
    public List<BotFeedback> list(String status)
    {
        if (StringUtils.isNotBlank(status) && !FEEDBACK_STATUS.contains(status))
        {
            throw new IllegalArgumentException("反馈状态不支持");
        }
        return feedbackMapper.selectFeedbackList(botUserContext.currentUser().getUserId(), status);
    }

    /**
     * 更新反馈处理状态。
     *
     * @param id      反馈编号
     * @param request 状态请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, BotFeedbackStatusRequest request)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("反馈编号不能为空");
        }
        if (request == null || StringUtils.isBlank(request.getStatus()))
        {
            throw new IllegalArgumentException("反馈状态不能为空");
        }
        if (!FEEDBACK_STATUS.contains(request.getStatus()))
        {
            throw new IllegalArgumentException("反馈状态不支持");
        }
        BotFeedback feedback = new BotFeedback();
        feedback.setId(id);
        feedback.setFeedbackUserId(botUserContext.currentUser().getUserId());
        feedback.setStatus(request.getStatus());
        feedbackMapper.updateFeedbackStatus(feedback);
    }

    /**
     * 采纳纠错反馈并回灌到知识库。
     *
     * @param id      反馈编号
     * @param request 采纳请求
     * @return 新建问答知识编号
     */
    @Transactional(rollbackFor = Exception.class)
    public Long acceptCorrection(Long id, BotFeedbackAcceptRequest request)
    {
        validateAcceptRequest(id, request);
        BotUserContext.CurrentUser currentUser = botUserContext.currentUser();
        BotFeedback feedback = feedbackMapper.selectFeedbackByIdAndUserId(id, currentUser.getUserId());
        if (feedback == null)
        {
            throw new IllegalArgumentException("反馈记录不存在");
        }
        if (!TYPE_CORRECTION.equals(feedback.getFeedbackType()))
        {
            throw new IllegalArgumentException("只能采纳纠错反馈");
        }
        if (StringUtils.isBlank(feedback.getSuggestion()))
        {
            throw new IllegalArgumentException("纠错反馈的建议答案不能为空");
        }

        BotMessage assistantMessage = messageMapper.selectMessageById(feedback.getMessageId());
        validateTargetMessage(feedback, assistantMessage, currentUser.getUserId());
        BotMessage userMessage = messageMapper.selectPreviousUserMessage(feedback.getConversationId(),
                currentUser.getUserId(), feedback.getMessageId());
        if (userMessage == null && StringUtils.isBlank(request.getQuestion()))
        {
            throw new IllegalArgumentException("原始用户问题不存在");
        }

        String question = StringUtils.isNotBlank(request.getQuestion()) ? request.getQuestion() : userMessage.getContent();
        Long qaId = kbClient.createAndPublishQa(request.getCategoryId(), question, feedback.getSuggestion(), null);

        BotFeedback update = new BotFeedback();
        update.setId(id);
        update.setFeedbackUserId(currentUser.getUserId());
        update.setAcceptedQaId(qaId);
        update.setStatus(STATUS_RESOLVED);
        feedbackMapper.updateFeedbackStatus(update);
        return qaId;
    }

    private void validateSubmitRequest(BotFeedbackRequest request)
    {
        if (request == null)
        {
            throw new IllegalArgumentException("反馈请求不能为空");
        }
        if (StringUtils.isBlank(request.getConversationId()))
        {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        if (request.getMessageId() == null)
        {
            throw new IllegalArgumentException("消息编号不能为空");
        }
        if (StringUtils.isBlank(request.getFeedbackType()) || !FEEDBACK_TYPES.contains(request.getFeedbackType()))
        {
            throw new IllegalArgumentException("反馈类型不支持");
        }
        if (TYPE_CORRECTION.equals(request.getFeedbackType()) && StringUtils.isBlank(request.getSuggestion()))
        {
            throw new IllegalArgumentException("纠错反馈的建议答案不能为空");
        }
    }

    private void validateTargetMessage(BotFeedbackRequest request, BotMessage message, Long userId)
    {
        if (message == null || !request.getConversationId().equals(message.getConversationId())
                || !userId.equals(message.getUserId()))
        {
            throw new IllegalArgumentException("反馈目标消息不存在");
        }
        if (!ROLE_ASSISTANT.equals(message.getRole()))
        {
            throw new IllegalArgumentException("只能对助手回答提交反馈");
        }
    }

    private void validateTargetMessage(BotFeedback feedback, BotMessage message, Long userId)
    {
        if (message == null || !feedback.getConversationId().equals(message.getConversationId())
                || !userId.equals(message.getUserId()))
        {
            throw new IllegalArgumentException("反馈目标消息不存在");
        }
        if (!ROLE_ASSISTANT.equals(message.getRole()))
        {
            throw new IllegalArgumentException("只能对助手回答提交反馈");
        }
    }

    private void validateAcceptRequest(Long id, BotFeedbackAcceptRequest request)
    {
        if (id == null)
        {
            throw new IllegalArgumentException("反馈编号不能为空");
        }
        if (request == null)
        {
            throw new IllegalArgumentException("采纳请求不能为空");
        }
        if (request.getCategoryId() == null)
        {
            throw new IllegalArgumentException("分类ID不能为空");
        }
        if (kbClient == null)
        {
            throw new IllegalStateException("知识库客户端未配置");
        }
    }
}
