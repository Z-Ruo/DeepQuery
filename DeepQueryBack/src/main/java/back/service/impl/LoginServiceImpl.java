package back.service.impl;

import back.entity.UserLoginInfo;
import back.entity.UserInfo;
import back.repository.UserRepository;
import back.repository.UserLoginInfoRepository;
import back.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.Instant;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserLoginInfoRepository userLoginInfoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public boolean login(String username, String password, String ip) {
        UserInfo user = userRepository.findByUserName(username);
        if (user != null && passwordEncoder.matches(password, user.getPasswordHash())) {
            UserLoginInfo loginInfo = new UserLoginInfo();
            loginInfo.setUser(user);
            loginInfo.setLastLoginTime(Instant.now());
            loginInfo.setLoginIp(ip); // 设置登录 IP
            userLoginInfoRepository.save(loginInfo);
            return true;
        }
        return false;
    }

    @Override
    public boolean registerUser(String username, String password, String phone) {
        if (userRepository.existsByUserName(username)) {
            return false;
        }
        UserInfo newUser = new UserInfo();
        newUser.setUserName(username);
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setCreatedAt(Instant.now());
        newUser.setPhone(phone);
        newUser.setRole("普通用户");
        userRepository.save(newUser);
        return true;
    }
}
