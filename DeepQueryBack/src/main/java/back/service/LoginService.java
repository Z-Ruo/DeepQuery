package back.service;

public interface LoginService {
    boolean login(String username, String password, String ip);
    boolean registerUser(String username, String password, String phone);
}


