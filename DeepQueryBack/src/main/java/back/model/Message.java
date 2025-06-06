package back.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
public @Data class Message {
    // 角色
    private String role;
    // 信息内容
    private String content;
    // 信息时间
    private Instant timestamp;
}
