package back.model;

import lombok.Data;

@Data
public class SearchRequest {
    // 搜索查询
    private String query;
    // 最大结果数
    private int maxResults = 5;
}
