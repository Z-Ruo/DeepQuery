package back.service;

public interface LoginService {
    Integer login(String username, String password, String ip);
    boolean registerUser(String username, String password, String phone);
}


