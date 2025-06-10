package back.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import back.model.Message;
import back.model.SessionRequest;
import back.model.SessionResponse;
import back.service.SessionService;

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
        return sessionService.getSessionMessages(sessionId);
    }

    // 新增：更新会话信息
    @PutMapping("/update/{sessionId}")
    public ResponseEntity<SessionResponse> updateSession(@PathVariable Integer sessionId, @RequestBody SessionRequest request) {
        SessionResponse response = sessionService.updateSession(sessionId, request);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    // 新增：删除会话
    @DeleteMapping("/delete/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable Integer sessionId) {
        boolean success = sessionService.deleteSession(sessionId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "会话删除成功" : "会话删除失败");
        return ResponseEntity.ok(response);
    }

    // 新增：创建新会话（用于前端的/new端点）
    @PostMapping("/new")
    public ResponseEntity<SessionResponse> createNewSession(@RequestBody Map<String, Object> request) {
        Integer userId = (Integer) request.get("userId");
        if (userId == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "userId is required");
            return ResponseEntity.badRequest().build();
        }
        
        SessionResponse response = sessionService.createNewSession(userId);
        return ResponseEntity.ok(response);
    }

    // 新增：保存会话消息
    @PostMapping("/{sessionId}/message")
    public ResponseEntity<Map<String, Object>> saveSessionMessage(@PathVariable Integer sessionId, @RequestBody Message message) {
        boolean success = sessionService.saveSessionMessage(sessionId, message);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "消息保存成功" : "消息保存失败");
        return ResponseEntity.ok(response);
    }
}
