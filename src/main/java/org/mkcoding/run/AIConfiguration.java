package org.mkcoding.run;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfiguration {

    @Value("${openai.model.name:GPT_4_O_MINI}")
    private String openAiModelName;

    @Value("${openai.temperature:0.7}")
    private Double temperature;

    @Value("${openai.max.tokens:1500}")
    private Integer maxTokens;

    @Value("${openai.api.key:}")
    private String apiKeyFromProperties;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new BgeSmallEnV15QuantizedEmbeddingModel();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // Try to get the API key from the environment variable first
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = apiKeyFromProperties;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key must be set either as an environment variable 'OPENAI_API_KEY' or in 'application.properties' as 'openai.api.key'");
        }

        OpenAiChatModelName modelName = OpenAiChatModelName.valueOf(openAiModelName);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
}
