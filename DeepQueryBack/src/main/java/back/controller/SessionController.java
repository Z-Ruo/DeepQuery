package back.controller;

import back.model.SessionRequest;
import back.model.SessionResponse;
import back.service.SessionService;
import back.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/session")
public class SessionController {
    @Autowired
    private SessionService sessionService;
@PostMapping("/start")
    public SessionResponse startSession(@RequestBody SessionRequest request) {
        return sessionService.startSession(request);
    }

    @GetMapping("/list/{userId}")
    public List<SessionResponse> listSessions(@PathVariable Integer userId) {
        return sessionService.listSessions(userId);
    }

    @GetMapping("/previous/{userId}")
    public SessionResponse previousSession(@PathVariable Integer userId) {
        return sessionService.previousSession(userId);
    }

    // 新增：通过会话id获取前50条消息
    @GetMapping("/messages/{sessionId}")
    public List<Message> getSessionMessages(@PathVariable Integer sessionId) {
        return ((back.service.impl.SessionServiceImpl)sessionService).getSessionMessages(sessionId);
    }
}
