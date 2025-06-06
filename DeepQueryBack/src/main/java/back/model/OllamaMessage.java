package back.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OllamaMessage {
    // 角色
    private String role;
    // 信息内容
    private String content;
}
