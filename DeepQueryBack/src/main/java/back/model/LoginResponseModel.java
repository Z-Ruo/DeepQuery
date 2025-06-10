package back.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LoginResponseModel {
    private String message;
    private int status;
    private String token;
    private Integer userId;
    
    // 为了兼容之前的构造函数，保留无userId的构造函数
    public LoginResponseModel(String message, int status, String token) {
        this.message = message;
        this.status = status;
        this.token = token;
        this.userId = null;
    }
}
