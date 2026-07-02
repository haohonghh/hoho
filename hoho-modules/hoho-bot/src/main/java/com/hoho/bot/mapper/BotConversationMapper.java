package com.hoho.bot.mapper;

import java.util.List;

import com.hoho.bot.domain.BotConversation;
import org.apache.ibatis.annotations.Param;

/**
 * 机器人会话数据层
 *
 * @author hoho
 */
public interface BotConversationMapper
{
    BotConversation selectByConversationIdAndUserId(@Param("conversationId") String conversationId,
            @Param("userId") Long userId);

    List<BotConversation> selectConversationList(@Param("userId") Long userId);

    int insertConversation(BotConversation conversation);

    int updateConversationSummary(BotConversation conversation);
}
