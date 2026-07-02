package com.hoho.bot.mapper;

import java.util.List;

import com.hoho.bot.domain.BotConversation;

/**
 * 机器人会话数据层
 *
 * @author hoho
 */
public interface BotConversationMapper
{
    BotConversation selectByConversationId(String conversationId);

    List<BotConversation> selectConversationList();

    int insertConversation(BotConversation conversation);

    int updateConversationSummary(BotConversation conversation);
}
