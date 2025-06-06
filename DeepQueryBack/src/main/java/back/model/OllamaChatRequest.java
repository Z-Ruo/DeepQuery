package back.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public @Data class OllamaChatRequest {
    private String model;
    private List<OllamaMessage> messages;
    private boolean stream;
}
