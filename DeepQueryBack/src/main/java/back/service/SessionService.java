package back.service;

import java.util.List;

import back.model.Message;
import back.model.SessionRequest;
import back.model.SessionResponse;

public interface SessionService {
    SessionResponse startSession(SessionRequest request);
    List<SessionResponse> listSessions(Integer userId);
    SessionResponse previousSession(Integer userId);
    List<Message> getSessionMessages(Integer sessionId);
    SessionResponse updateSession(Integer sessionId, SessionRequest request);
    boolean deleteSession(Integer sessionId);
    SessionResponse createNewSession(Integer userId);
    boolean saveSessionMessage(Integer sessionId, Message message);
}
