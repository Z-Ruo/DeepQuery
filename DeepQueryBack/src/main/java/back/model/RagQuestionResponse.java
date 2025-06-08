package back.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagQuestionResponse {
    // 用户查询
    private String query;
    
    // 回答内容
    private String answer;
    
    // 使用的模型
    private String model;
    
    // 知识库名称
    private String collectionName;
    
    // 参考的文档信息
    private List<RagSourceInfo> sources;
    
    // 创建时间
    private Instant timestamp;
    
    // 会话ID（如果是会话中的一部分）
    private Integer sessionId;
}
