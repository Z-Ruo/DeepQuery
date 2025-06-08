package back.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RagSourceInfo {
    // 源文档标题或文件名
    private String title;
    
    // 源文档片段内容
    private String segment;
    
    // 相关度/置信度分数
    private double score;
}
