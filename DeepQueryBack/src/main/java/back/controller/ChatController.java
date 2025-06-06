package back.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import back.model.ChatRequest;
import back.model.ChatResponse;
import back.service.ChatService;

/**
 * @author 对话接口
 */
@RestController
@RequestMapping("/v1/chat")
public class ChatController {


    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    //直接输出
    @PostMapping(value = "/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse completions(@RequestBody ChatRequest chatRequest) throws IOException {
            return chatService.completions(chatRequest);
    }
    //流式输出
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        return chatService.streamChat(request);
    }

}
