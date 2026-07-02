package com.hoho.kb.mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KbPersistenceStyleTest
{
    @Test
    void 知识库运行时代码不再使用JdbcTemplate() throws IOException
    {
        Path sourceRoot = Path.of("src/main/java/com/hoho/kb");

        try (Stream<Path> files = Files.walk(sourceRoot))
        {
            List<Path> javaFiles = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles)
            {
                String content = Files.readString(javaFile, StandardCharsets.UTF_8);
                assertFalse(content.contains("JdbcTemplate"), javaFile + " 不应再使用 JdbcTemplate");
            }
        }
    }

    @Test
    void 知识库Mapper保留PostgreSQL和pgvector语法() throws IOException
    {
        String embeddingMapper = new ClassPathResource("mapper/kb/EmbeddingMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);
        String keywordMapper = new ClassPathResource("mapper/kb/KeywordMapper.xml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(embeddingMapper.contains("::vector"));
        assertTrue(embeddingMapper.contains("&lt;=&gt;"));
        assertTrue(keywordMapper.contains("ILIKE"));
    }
}
