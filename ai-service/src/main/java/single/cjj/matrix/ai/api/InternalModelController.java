package single.cjj.matrix.ai.api;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import single.cjj.matrix.ai.security.InternalTokenGuard;
import single.cjj.matrix.ai.service.AiTaskRouter;
import single.cjj.matrix.ai.service.SpringAiEmbeddingGateway;
import single.cjj.matrix.ai.service.SpringAiModelGateway;

@RestController
@RequestMapping("/internal/model")
public class InternalModelController {

    private final SpringAiModelGateway modelGateway;
    private final SpringAiEmbeddingGateway embeddingGateway;
    private final InternalTokenGuard tokenGuard;

    public InternalModelController(
            SpringAiModelGateway modelGateway,
            SpringAiEmbeddingGateway embeddingGateway,
            InternalTokenGuard tokenGuard
    ) {
        this.modelGateway = modelGateway;
        this.embeddingGateway = embeddingGateway;
        this.tokenGuard = tokenGuard;
    }

    @GetMapping("/status")
    public ModelContracts.StatusResponse status(
            @RequestHeader(value = InternalTokenGuard.HEADER_NAME, required = false) String internalToken
    ) {
        tokenGuard.verify(internalToken);
        return modelGateway.status();
    }

    @PostMapping("/chat")
    public ModelContracts.ChatResponse chat(
            @RequestHeader(value = InternalTokenGuard.HEADER_NAME, required = false) String internalToken,
            @Valid @RequestBody ModelContracts.ChatRequest request
    ) {
        tokenGuard.verify(internalToken);
        return modelGateway.chat(request);
    }

    @PostMapping("/embeddings")
    public ModelContracts.EmbeddingResponse embeddings(
            @RequestHeader(value = InternalTokenGuard.HEADER_NAME, required = false) String internalToken,
            @Valid @RequestBody ModelContracts.EmbeddingRequest request
    ) {
        tokenGuard.verify(internalToken);
        return embeddingGateway.embed(request);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<ModelContracts.StreamEvent>> stream(
            @RequestHeader(value = InternalTokenGuard.HEADER_NAME, required = false) String internalToken,
            @Valid @RequestBody ModelContracts.ChatRequest request
    ) {
        tokenGuard.verify(internalToken);

        String traceId = modelGateway.newTraceId();
        StringBuilder answer = new StringBuilder();
        AiTaskRouter.ModelRoute route = modelGateway.route(request);

        Flux<ServerSentEvent<ModelContracts.StreamEvent>> start = Flux.just(
                event("start", ModelContracts.StreamEvent.start())
        );
        Flux<ServerSentEvent<ModelContracts.StreamEvent>> deltas = modelGateway.stream(request, route, traceId)
                .doOnNext(answer::append)
                .map(delta -> event("delta", ModelContracts.StreamEvent.delta(delta)));
        Flux<ServerSentEvent<ModelContracts.StreamEvent>> done = Flux.defer(() -> Flux.just(
                event("done", ModelContracts.StreamEvent.done(new ModelContracts.ChatResponse(
                        answer.toString(),
                        route.model(),
                        "spring-ai",
                        traceId,
                        0,
                        0,
                        0,
                        0.0
                )))
        ));

        return Flux.concat(start, deltas, done)
                .onErrorResume(error -> Flux.just(
                        event("error", ModelContracts.StreamEvent.error(safeMessage(error)))
                ));
    }

    private ServerSentEvent<ModelContracts.StreamEvent> event(
            String name,
            ModelContracts.StreamEvent data
    ) {
        return ServerSentEvent.<ModelContracts.StreamEvent>builder(data)
                .event(name)
                .build();
    }

    private String safeMessage(Throwable error) {
        String message = error == null ? null : error.getMessage();
        if (message == null || message.isBlank()) {
            return "Spring AI 模型调用失败";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
