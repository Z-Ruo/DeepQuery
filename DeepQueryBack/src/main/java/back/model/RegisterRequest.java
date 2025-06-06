package back.model;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String phone;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
