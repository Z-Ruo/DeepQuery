package back.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
public class History {
    // 信息列表
    private List<Message> messages = new ArrayList<>();
    // 会话ID
    private Integer sessionId;
}
