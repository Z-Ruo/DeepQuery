package back.service.impl;

import back.entity.DialogueSessionInfo;
import back.entity.UserInfo;
import back.entity.HistoryRecordInfo;
import back.model.SessionRequest;
import back.model.SessionResponse;
import back.model.Message;
import back.repository.DialogueSessionsRepository;
import back.repository.UserRepository;
import back.repository.HistoryRecordsRepository;
import back.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SessionServiceImpl implements SessionService {
    @Autowired
    private DialogueSessionsRepository dialogueSessionsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HistoryRecordsRepository historyRecordsRepository;

    @Override
    public SessionResponse startSession(SessionRequest request) {
        UserInfo user = userRepository.findById(request.getUserId()).orElse(null);
        DialogueSessionInfo session = new DialogueSessionInfo();
        session.setUser(user);
        session.setCreatedAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        session = dialogueSessionsRepository.save(session);
        SessionResponse response = new SessionResponse();
        response.setSessionId(session.getId());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }

    @Override
    public List<SessionResponse> listSessions(Integer userId) {
        List<DialogueSessionInfo> sessions = dialogueSessionsRepository.findAll().stream()
                .filter(s -> s.getUser() != null && s.getUser().getId().equals(userId))
                .collect(Collectors.toList());
        return sessions.stream().map(s -> {
            SessionResponse resp = new SessionResponse();
            resp.setSessionId(s.getId());
            resp.setCreatedAt(s.getCreatedAt());
            resp.setUpdatedAt(s.getUpdatedAt());
            // 查询最近20条记录
            List<HistoryRecordInfo> records = historyRecordsRepository.findRecentBySessionId(s.getId(), PageRequest.of(0, 20));
            List<Message> messages = records.stream().map(r -> new Message(r.getRole(), r.getContent(), r.getTimestamp())).collect(Collectors.toList());
            resp.setMessages(messages);
            return resp;
        }).collect(Collectors.toList());
    }

    // 新增：通过会话id获取前50条记录
    public List<Message> getSessionMessages(Integer sessionId) {
        List<HistoryRecordInfo> records = historyRecordsRepository.findRecentBySessionId(sessionId, PageRequest.of(0, 50));
        return records.stream().map(r -> new Message(r.getRole(), r.getContent(), r.getTimestamp())).collect(Collectors.toList());
    }

    @Override
    public SessionResponse previousSession(Integer userId) {
        List<DialogueSessionInfo> sessions = dialogueSessionsRepository.findAll().stream()
                .filter(s -> s.getUser() != null && s.getUser().getId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
        if (sessions.size() < 2) return null;
        DialogueSessionInfo prev = sessions.get(1);
        SessionResponse resp = new SessionResponse();
        resp.setSessionId(prev.getId());
        resp.setCreatedAt(prev.getCreatedAt());
        resp.setUpdatedAt(prev.getUpdatedAt());
        return resp;
    }
}
