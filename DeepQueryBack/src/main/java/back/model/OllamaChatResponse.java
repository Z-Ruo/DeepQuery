package back.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OllamaChatResponse {
    private String model;
    private String created_at;
    private Message message;
    // 当前分片消息
    private boolean done;
    // 是否结束
    private String done_reason;

}
