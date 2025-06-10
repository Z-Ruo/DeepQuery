package back.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import back.entity.DialogueSessionInfo;
import back.entity.HistoryRecordInfo;
import back.entity.UserInfo;
import back.model.Message;
import back.model.SessionRequest;
import back.model.SessionResponse;
import back.repository.DialogueSessionsRepository;
import back.repository.HistoryRecordsRepository;
import back.repository.UserRepository;
import back.service.SessionService;

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
        
        // 生成会话标题
        String title = generateSessionTitle(user, Instant.now());
        session.setTitle(title);
        
        session = dialogueSessionsRepository.save(session);
        SessionResponse response = new SessionResponse();
        response.setSessionId(session.getId());
        response.setTitle(session.getTitle());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }

    @Override
    public List<SessionResponse> listSessions(Integer userId) {
        List<DialogueSessionInfo> sessions = dialogueSessionsRepository.findAll().stream()
                .filter(s -> s.getUser() != null && s.getUser().getId().equals(userId))
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt())) // 按更新时间倒序排列
                .collect(Collectors.toList());
        return sessions.stream().map(s -> {
            SessionResponse resp = new SessionResponse();
            resp.setSessionId(s.getId());
            resp.setTitle(s.getTitle());
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
    @Override
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
        resp.setTitle(prev.getTitle());
        resp.setCreatedAt(prev.getCreatedAt());
        resp.setUpdatedAt(prev.getUpdatedAt());
        return resp;
    }

    @Override
    public SessionResponse updateSession(Integer sessionId, SessionRequest request) {
        DialogueSessionInfo session = dialogueSessionsRepository.findById(sessionId).orElse(null);
        if (session == null) return null;
        
        session.setUpdatedAt(Instant.now());
        session = dialogueSessionsRepository.save(session);
        
        SessionResponse response = new SessionResponse();
        response.setSessionId(session.getId());
        response.setTitle(session.getTitle());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }

    @Override
    public boolean deleteSession(Integer sessionId) {
        try {
            // 首先删除相关的历史记录
            List<HistoryRecordInfo> records = historyRecordsRepository.findRecentBySessionId(sessionId, PageRequest.of(0, Integer.MAX_VALUE));
            historyRecordsRepository.deleteAll(records);
            
            // 然后删除会话
            dialogueSessionsRepository.deleteById(sessionId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public SessionResponse createNewSession(Integer userId) {
        SessionRequest request = new SessionRequest();
        request.setUserId(userId);
        return startSession(request);
    }

    @Override
    public boolean saveSessionMessage(Integer sessionId, Message message) {
        try {
            DialogueSessionInfo session = dialogueSessionsRepository.findById(sessionId).orElse(null);
            if (session == null) return false;
            
            HistoryRecordInfo record = new HistoryRecordInfo();
            record.setSession(session);
            record.setRole(message.getRole());
            record.setContent(message.getContent());
            record.setTimestamp(message.getTimestamp() != null ? message.getTimestamp() : Instant.now());
            
            historyRecordsRepository.save(record);
            
            // 更新会话的最后更新时间
            session.setUpdatedAt(Instant.now());
            dialogueSessionsRepository.save(session);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // 生成会话标题的辅助方法
    private String generateSessionTitle(UserInfo user, Instant createdAt) {
        if (user == null) {
            return "新对话 - " + java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(java.time.ZoneId.systemDefault()).format(createdAt);
        }
        
        // 获取该用户今天创建的会话数量
        Instant startOfDay = createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant();
        Instant endOfDay = startOfDay.plus(java.time.Duration.ofDays(1));
        
        long todaySessionCount = dialogueSessionsRepository.findAll().stream()
                .filter(s -> s.getUser() != null && s.getUser().getId().equals(user.getId()))
                .filter(s -> s.getCreatedAt().isAfter(startOfDay) && s.getCreatedAt().isBefore(endOfDay))
                .count();
                
        return String.format("%s的对话 %d - %s", 
            user.getUserName(), 
            todaySessionCount + 1,
            java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(java.time.ZoneId.systemDefault()).format(createdAt));
    }
}
