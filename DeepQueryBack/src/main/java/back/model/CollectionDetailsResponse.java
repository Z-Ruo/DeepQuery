package back.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class CollectionDetailsResponse {
    // 请求的状态码
    private String status;
    // 请求的消息内容
    private String message;
    // 集合中的实体列表
    private List<Map<String, Object>> entities;
}
