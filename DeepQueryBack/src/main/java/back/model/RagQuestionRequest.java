package back.model;

import lombok.Data;

@Data
public class RagQuestionRequest {
    // 用户问题
    private String query;
    
    // 知识库（集合）名称
    private String collectionName;
    
    // 最大检索结果数
    private int maxResults = 3;
    
    // 会话ID（可选，用于保存到历史记录）
    private Integer sessionId;
    
}
