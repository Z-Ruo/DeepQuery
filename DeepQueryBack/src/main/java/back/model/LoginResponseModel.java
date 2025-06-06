package back.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class LoginResponseModel {
    private String message;
    private int status;
    private String token;
}
