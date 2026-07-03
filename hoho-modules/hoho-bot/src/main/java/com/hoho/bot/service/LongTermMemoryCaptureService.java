package com.hoho.bot.service;

import com.hoho.bot.model.response.BotChatResponse;
import com.hoho.common.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 长期记忆自动抽取服务
 *
 * @author hoho
 */
@Service
public class LongTermMemoryCaptureService
{
    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryCaptureService.class);

    private final BotUserContext botUserContext;

    private final AiProxyClient aiProxyClient;

    public LongTermMemoryCaptureService(BotUserContext botUserContext, AiProxyClient aiProxyClient)
    {
        this.botUserContext = botUserContext;
        this.aiProxyClient = aiProxyClient;
    }

    public void captureIfNecessary(String conversationId, String userMessage, BotChatResponse response)
    {
        if (StringUtils.isBlank(userMessage) || response == null)
        {
            return;
        }

        MemoryFact memoryFact = extractMemoryFact(userMessage);
        if (memoryFact == null)
        {
            return;
        }

        BotUserContext.CurrentUser currentUser = botUserContext.currentUser();
        if (currentUser == null || currentUser.getUserId() == null)
        {
            return;
        }

        try
        {
            aiProxyClient.upsertLongTermMemory(currentUser.getUserId(), conversationId,
                    memoryFact.getMemoryType(), memoryFact.getMemoryKey(), memoryFact.getMemoryValue());
            log.info("自动写入长期记忆完成 conversationId={}, userId={}, memoryType={}, memoryKey={}",
                    conversationId, currentUser.getUserId(), memoryFact.getMemoryType(), memoryFact.getMemoryKey());
        }
        catch (Exception e)
        {
            log.warn("自动写入长期记忆失败，但不影响当前回答 conversationId={}, reason={}", conversationId, e.getMessage());
        }
    }

    private MemoryFact extractMemoryFact(String userMessage)
    {
        String normalized = userMessage.trim();

        MemoryFact preferenceStyle = extractReplyStylePreference(normalized);
        if (preferenceStyle != null)
        {
            return preferenceStyle;
        }

        MemoryFact preferenceLanguage = extractReplyLanguagePreference(normalized);
        if (preferenceLanguage != null)
        {
            return preferenceLanguage;
        }

        MemoryFact profileRole = extractProfileRole(normalized);
        if (profileRole != null)
        {
            return profileRole;
        }

        MemoryFact environmentOs = extractEnvironmentOs(normalized);
        if (environmentOs != null)
        {
            return environmentOs;
        }

        return extractEnvironmentNetwork(normalized);
    }

    private MemoryFact extractReplyLanguagePreference(String normalized)
    {
        boolean mentionsReply = normalized.contains("回答") || normalized.contains("回复") || normalized.contains("以后")
                || normalized.contains("之后");
        if (mentionsReply && normalized.contains("中文"))
        {
            return new MemoryFact("preference", "reply_language", "中文");
        }
        return null;
    }

    private MemoryFact extractReplyStylePreference(String normalized)
    {
        boolean mentionsReply = normalized.contains("回答") || normalized.contains("回复") || normalized.contains("以后")
                || normalized.contains("之后");
        if (!mentionsReply)
        {
            return null;
        }
        if (normalized.contains("分步骤") || normalized.contains("一步一步") || normalized.contains("按步骤"))
        {
            return new MemoryFact("preference", "reply_style", "分步骤");
        }
        if (normalized.contains("详细"))
        {
            return new MemoryFact("preference", "reply_style", "详细");
        }
        if (normalized.contains("简洁") || normalized.contains("简短") || normalized.contains("精简"))
        {
            return new MemoryFact("preference", "reply_style", normalized);
        }
        return null;
    }

    private MemoryFact extractProfileRole(String normalized)
    {
        if (!normalized.startsWith("我是"))
        {
            return null;
        }
        if (normalized.contains("运维"))
        {
            return new MemoryFact("profile", "role", "运维");
        }
        if (normalized.contains("开发"))
        {
            return new MemoryFact("profile", "role", "开发");
        }
        if (normalized.contains("测试"))
        {
            return new MemoryFact("profile", "role", "测试");
        }
        if (normalized.contains("产品"))
        {
            return new MemoryFact("profile", "role", "产品");
        }
        if (normalized.contains("新员工"))
        {
            return new MemoryFact("profile", "role", "新员工");
        }
        return null;
    }

    private MemoryFact extractEnvironmentOs(String normalized)
    {
        if (!normalized.contains("我们") && !normalized.contains("公司") && !normalized.contains("电脑"))
        {
            return null;
        }
        if (normalized.contains("Windows"))
        {
            return new MemoryFact("environment", "os", "Windows");
        }
        if (normalized.contains("macOS") || normalized.contains("Mac"))
        {
            return new MemoryFact("environment", "os", "macOS");
        }
        if (normalized.contains("Linux"))
        {
            return new MemoryFact("environment", "os", "Linux");
        }
        return null;
    }

    private MemoryFact extractEnvironmentNetwork(String normalized)
    {
        if ((normalized.contains("公司") || normalized.contains("网络")) && normalized.contains("代理"))
        {
            return new MemoryFact("environment", "network", "代理网络");
        }
        return null;
    }

    private static class MemoryFact
    {
        private final String memoryType;

        private final String memoryKey;

        private final String memoryValue;

        private MemoryFact(String memoryType, String memoryKey, String memoryValue)
        {
            this.memoryType = memoryType;
            this.memoryKey = memoryKey;
            this.memoryValue = memoryValue;
        }

        public String getMemoryType()
        {
            return memoryType;
        }

        public String getMemoryKey()
        {
            return memoryKey;
        }

        public String getMemoryValue()
        {
            return memoryValue;
        }
    }
}
