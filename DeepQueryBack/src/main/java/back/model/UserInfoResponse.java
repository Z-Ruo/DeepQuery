package back.model;

import lombok.Data;
import java.time.Instant;

@Data
public class UserInfoResponse {
    private Integer id;
    private String username;
    private String phone;
    private String role;
    private Instant createdAt;
}
