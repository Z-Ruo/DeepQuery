package back.model;

import java.util.List;

import lombok.Data;

@Data
public class SystemResopnse {
    // 请求的状态码
    private String status;
    // 请求的消息内容
    private String message;
    // 知识库列表
    private List<String> knowledgeList;
    
}