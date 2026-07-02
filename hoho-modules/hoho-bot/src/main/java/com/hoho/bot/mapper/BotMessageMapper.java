package com.hoho.bot.mapper;

import java.util.List;

import com.hoho.bot.domain.BotMessage;

/**
 * 机器人消息数据层
 *
 * @author hoho
 */
public interface BotMessageMapper
{
    int insertMessage(BotMessage message);

    List<BotMessage> selectMessageList(String conversationId);
}
