package back.model;

import lombok.Data;
import java.time.LocalDate;


/**
 * @author 对话接口请求DTO
 */
@Data
public class ChatRequest {
    //使用的类型选择，有ollama和zhipu
    private String status;
    // 智谱的API密钥
    private String zhipu_apiKey;
    //Ollama地址和端口
    private String ollama_url="http://localhost:11434";
    // 智谱API模型
    private String zhipu_model="glm-4-flash";
    // 使用Ollama模型名称
    private String ollama_model="qwen2.5:0.5b";
    // 会话id
    private Integer sessionId;
    // 历史记录
    private History history;
    // 问题信息
    private Message question;
    // 用户指定日期（可选）
    private LocalDate date;

}
