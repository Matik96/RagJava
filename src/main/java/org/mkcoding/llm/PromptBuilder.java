package org.mkcoding.llm;

import dev.langchain4j.rag.content.Content;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String buildPrompt(List<Content> segments, String question) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an assistant that answers questions based on the provided document.\n\n");
        prompt.append("Context:\n");
        for (Content segment : segments) {
            prompt.append(segment.textSegment().text()).append("\n");
        }
        prompt.append("\nQuestion:\n").append(question).append("\n\nAnswer:");
        return prompt.toString();
    }
}
