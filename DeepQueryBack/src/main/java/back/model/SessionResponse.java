package back.model;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class SessionResponse {
    private Integer sessionId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Message> messages;
}
