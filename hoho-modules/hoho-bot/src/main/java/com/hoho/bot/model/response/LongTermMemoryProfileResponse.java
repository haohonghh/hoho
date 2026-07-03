package com.hoho.bot.model.response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 长期记忆画像响应
 *
 * @author hoho
 */
public class LongTermMemoryProfileResponse
{
    private Long userId;

    private Map<String, String> preferences = new LinkedHashMap<>();

    private Map<String, String> profile = new LinkedHashMap<>();

    private Map<String, String> environment = new LinkedHashMap<>();

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Map<String, String> getPreferences()
    {
        return preferences;
    }

    public void setPreferences(Map<String, String> preferences)
    {
        this.preferences = preferences;
    }

    public Map<String, String> getProfile()
    {
        return profile;
    }

    public void setProfile(Map<String, String> profile)
    {
        this.profile = profile;
    }

    public Map<String, String> getEnvironment()
    {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment)
    {
        this.environment = environment;
    }
}
