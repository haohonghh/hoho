package com.hoho.bot.studio;

import java.util.List;

import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.hoho.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Studio 机器人加载器
 *
 * @author hoho
 */
@Component
public class BotStudioAgentLoader implements AgentLoader
{
    private final BotStudioAgent botStudioAgent;

    public BotStudioAgentLoader(BotStudioAgent botStudioAgent)
    {
        this.botStudioAgent = botStudioAgent;
    }

    @Override
    public List<String> listAgents()
    {
        return List.of(BotStudioAgent.APP_NAME);
    }

    @Override
    public Agent loadAgent(String appName)
    {
        if (BotStudioAgent.APP_NAME.equals(StringUtils.trim(appName)))
        {
            return botStudioAgent;
        }
        throw new IllegalArgumentException("未知Studio应用:" + appName);
    }
}
