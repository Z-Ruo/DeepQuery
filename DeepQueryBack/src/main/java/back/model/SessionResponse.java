package back.model;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class SessionResponse {
    private Integer sessionId;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Message> messages;
}
