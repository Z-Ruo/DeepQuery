package back.service;

import java.util.List;

import back.model.SessionRequest;
import back.model.SessionResponse;

public interface SessionService {
    SessionResponse startSession(SessionRequest request);
    List<SessionResponse> listSessions(Integer userId);
    SessionResponse previousSession(Integer userId);
}
