package com.hoho.bot.service;

import com.hoho.common.core.utils.StringUtils;
import com.hoho.common.security.utils.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * 机器人当前用户上下文
 *
 * @author hoho
 */
@Component
public class BotUserContext
{
    private static final Long STUDIO_USER_ID = 0L;

    private static final String STUDIO_USER_NAME = "studio";

    /**
     * 获取当前调用用户；无登录态时使用 Studio 调试用户。
     *
     * @return 当前用户信息
     */
    public CurrentUser currentUser()
    {
        Long userId = SecurityUtils.getUserId();
        String userName = SecurityUtils.getUsername();
        if (userId == null)
        {
            return new CurrentUser(STUDIO_USER_ID, STUDIO_USER_NAME);
        }
        if (StringUtils.isBlank(userName))
        {
            userName = STUDIO_USER_NAME;
        }
        return new CurrentUser(userId, userName);
    }

    /**
     * 当前用户快照
     */
    public static class CurrentUser
    {
        private final Long userId;

        private final String userName;

        public CurrentUser(Long userId, String userName)
        {
            this.userId = userId;
            this.userName = userName;
        }

        public Long getUserId()
        {
            return userId;
        }

        public String getUserName()
        {
            return userName;
        }
    }
}
