package com.hoho.bot.mapper;

import java.util.List;

import com.hoho.bot.domain.BotMessage;
import org.apache.ibatis.annotations.Param;

/**
 * 机器人消息数据层
 *
 * @author hoho
 */
public interface BotMessageMapper
{
    int insertMessage(BotMessage message);

    BotMessage selectMessageById(@Param("id") Long id);

    BotMessage selectPreviousUserMessage(@Param("conversationId") String conversationId, @Param("userId") Long userId,
            @Param("beforeMessageId") Long beforeMessageId);

    List<BotMessage> selectMessageList(@Param("conversationId") String conversationId, @Param("userId") Long userId);
}
