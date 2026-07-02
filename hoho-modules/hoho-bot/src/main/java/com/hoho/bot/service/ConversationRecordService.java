package com.hoho.bot.service;

import java.util.List;

import com.hoho.bot.domain.BotConversation;
import com.hoho.bot.domain.BotMessage;
import com.hoho.bot.mapper.BotConversationMapper;
import com.hoho.bot.mapper.BotMessageMapper;
import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话记录服务
 *
 * @author hoho
 */
@Service
public class ConversationRecordService
{
    private final BotConversationMapper conversationMapper;

    private final BotMessageMapper messageMapper;

    public ConversationRecordService(BotConversationMapper conversationMapper, BotMessageMapper messageMapper)
    {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 记录一条用户消息，并保证会话存在。
     *
     * <p>该方法在一个数据库事务内依次执行三步：
     * <ol>
     *   <li>{@link #ensureConversation}：若该 conversationId 尚不存在，
     *       则以当前消息截取前 30 个字符作为 title 新建会话；</li>
     *   <li>{@link #insertMessage}：写入一条 role=user 的消息记录；</li>
     *   <li>{@link #updateSummary}：更新会话的 last_message 摘要字段。</li>
     * </ol>
     *
     * @param conversationId 会话编号（由 BotService 解析/生成，保证非空）
     * @param content        用户消息正文
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordUserMessage(String conversationId, String content)
    {
        ensureConversation(conversationId, content);
        insertMessage(conversationId, "user", content, null, null);
        updateSummary(conversationId, content);
    }

    /**
     * 记录一条助手回复消息，并保证会话存在。
     *
     * <p>与 {@link #recordUserMessage} 类似，额外写入答案的来源（kb / ai_with_kb / ai）
     * 以及知识库匹配的相似度分数，便于后续审计与分析。
     *
     * @param conversationId 会话编号
     * @param response       BotService 构建好的响应对象；为 null 时方法直接返回，不做任何持久化
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordAssistantMessage(String conversationId, BotChatResponse response)
    {
        if (response == null)
        {
            return;
        }
        ensureConversation(conversationId, response.getAnswer());
        insertMessage(conversationId, "assistant", response.getAnswer(), response.getSource(), response.getScore());
        updateSummary(conversationId, response.getAnswer());
    }

    /**
     * 查询全部会话列表。
     *
     * <p>按 mapper 默认排序（通常即按创建时间倒序或最近活跃顺序）返回，
     * 用于前端会话历史列表展示。
     *
     * @return BotConversation 列表；无记录时返回空集合
     */
    public List<BotConversation> listConversations()
    {
        return conversationMapper.selectConversationList();
    }

    /**
     * 按会话编号查询该会话的全部消息明细。
     *
     * @param conversationId 会话编号，不能为空/空白
     * @return 按时间正序的消息列表
     * @throws IllegalArgumentException 当 conversationId 为空或空白时
     */
    public List<BotMessage> listMessages(String conversationId)
    {
        if (StringUtils.isBlank(conversationId))
        {
            throw new IllegalArgumentException("会话编号不能为空");
        }
        return messageMapper.selectMessageList(conversationId);
    }

    /**
     * 确保指定 conversationId 对应的会话在数据库中存在，不存在则新建。
     *
     * <p>采用"先查询再插入"的方式做幂等处理；标题直接从传入的 content
     * 截取前 30 个字符，{@code lastMessage} 初始为空字符串，后续由
     * {@link #updateSummary} 逐渐更新。
     *
     * <p>注意：该方法依赖于调用方已开启事务（如 @Transactional），
     * 以保证"查询 + 插入"两步在同一事务内完成，避免并发下重复创建会话。
     *
     * @param conversationId 会话编号
     * @param title          用于生成会话标题的内容（如首条消息）
     */
    private void ensureConversation(String conversationId, String title)
    {
        BotConversation existing = conversationMapper.selectByConversationId(conversationId);
        if (existing != null)
        {
            return;
        }
        BotConversation conversation = new BotConversation();
        conversation.setConversationId(conversationId);
        conversation.setTitle(buildTitle(title));
        conversation.setLastMessage("");
        conversationMapper.insertConversation(conversation);
    }

    /**
     * 向 bot_message 表插入一条消息记录。
     *
     * @param conversationId 所属会话编号
     * @param role           消息角色：{@code "user"} 或 {@code "assistant"}
     * @param content        消息正文
     * @param source         答案来源：{@code kb / ai_with_kb / ai}
     *                       （用户消息时为 null）
     * @param score          知识库相似度分数（用户消息及纯 AI 兜底时为 null）
     */
    private void insertMessage(String conversationId, String role, String content, String source, Double score)
    {
        BotMessage message = new BotMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setSource(source);
        message.setScore(score);
        messageMapper.insertMessage(message);
    }

    /**
     * 更新会话的最近一条消息摘要。
     *
     * <p>每次写入用户/助手消息后调用，用于前端会话列表中展示最近活跃内容。
     *
     * @param conversationId 会话编号
     * @param lastMessage    最近一条消息的正文
     */
    private void updateSummary(String conversationId, String lastMessage)
    {
        BotConversation conversation = new BotConversation();
        conversation.setConversationId(conversationId);
        conversation.setLastMessage(lastMessage);
        conversationMapper.updateConversationSummary(conversation);
    }

    /**
     * 根据消息内容生成会话标题：截取前 30 个字符。
     *
     * <p>若内容为空白，则返回默认标题"新会话"。
     *
     * @param content 原始内容（通常为首条用户消息）
     * @return 长度不超过 30 的标题字符串
     */
    private String buildTitle(String content)
    {
        if (StringUtils.isBlank(content))
        {
            return "新会话";
        }
        String title = content.trim();
        return title.length() > 30 ? title.substring(0, 30) : title;
    }
}
