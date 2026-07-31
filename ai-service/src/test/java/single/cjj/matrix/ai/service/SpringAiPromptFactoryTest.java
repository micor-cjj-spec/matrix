package single.cjj.matrix.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import single.cjj.matrix.ai.api.ModelContracts;
import single.cjj.matrix.ai.config.MatrixAiProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SpringAiPromptFactoryTest {

    @Test
    void shouldAssembleSystemHistoryKnowledgeAndCurrentQuestion() {
        MatrixAiProperties properties = new MatrixAiProperties();
        properties.setInternalToken("test-token");
        properties.setSystemPrompt("system-rule");
        SpringAiPromptFactory factory = new SpringAiPromptFactory(properties);

        Prompt prompt = factory.create(new ModelContracts.ChatRequest(
                "current-question",
                List.of(
                        new ModelContracts.Message("user", "history-user"),
                        new ModelContracts.Message("assistant", "history-assistant")
                ),
                List.of("knowledge-one", "knowledge-two")
        ));

        assertEquals(5, prompt.getInstructions().size());
        assertInstanceOf(SystemMessage.class, prompt.getInstructions().get(0));
        assertInstanceOf(UserMessage.class, prompt.getInstructions().get(1));
        assertInstanceOf(AssistantMessage.class, prompt.getInstructions().get(2));
        assertInstanceOf(SystemMessage.class, prompt.getInstructions().get(3));
        assertInstanceOf(UserMessage.class, prompt.getInstructions().get(4));
        assertEquals("current-question", prompt.getInstructions().get(4).getText());
    }
}
