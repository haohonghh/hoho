package com.hoho.kb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 知识库配置
 *
 * @author hoho
 */
@Component
@ConfigurationProperties(prefix = "hoho.kb")
public class KbProperties
{
    private final Embedding embedding = new Embedding();

    private final Search search = new Search();

    public Embedding getEmbedding()
    {
        return embedding;
    }

    public Search getSearch()
    {
        return search;
    }

    public static class Embedding
    {
        private String model = "bge-m3";

        private int dimension = 1024;

        public String getModel()
        {
            return model;
        }

        public void setModel(String model)
        {
            this.model = model;
        }

        public int getDimension()
        {
            return dimension;
        }

        public void setDimension(int dimension)
        {
            this.dimension = dimension;
        }
    }

    public static class Search
    {
        private int defaultTopK = 5;

        public int getDefaultTopK()
        {
            return defaultTopK;
        }

        public void setDefaultTopK(int defaultTopK)
        {
            this.defaultTopK = defaultTopK;
        }
    }
}
