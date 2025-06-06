package back.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    // 使用模型名称
    private String model;
    // 会话id
    private Integer sessionId;
    // 历史记录
    private History  history;
    // 答案信息
    private Message answer;
    //创建时间
    private Instant createdAt;
}


