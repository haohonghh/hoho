package com.hoho.bot.mapper;

import java.util.List;

import com.hoho.bot.domain.BotFeedback;
import org.apache.ibatis.annotations.Param;

/**
 * 机器人回答反馈数据层
 *
 * @author hoho
 */
public interface BotFeedbackMapper
{
    int insertFeedback(BotFeedback feedback);

    List<BotFeedback> selectFeedbackList(@Param("feedbackUserId") Long feedbackUserId, @Param("status") String status);

    BotFeedback selectFeedbackByIdAndUserId(@Param("id") Long id, @Param("feedbackUserId") Long feedbackUserId);

    int updateFeedbackStatus(BotFeedback feedback);
}
