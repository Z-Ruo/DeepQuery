package back.service.impl;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import back.entity.UserInfo;
import back.entity.UserLoginInfo;
import back.repository.UserLoginInfoRepository;
import back.repository.UserRepository;
import back.service.LoginService;
import jakarta.transaction.Transactional;

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
    public Integer login(String username, String password, String ip) {
        UserInfo user = userRepository.findByUserName(username);
        if (user != null && passwordEncoder.matches(password, user.getPasswordHash())) {
            UserLoginInfo loginInfo = new UserLoginInfo();
            loginInfo.setUser(user);
            loginInfo.setLastLoginTime(Instant.now());
            loginInfo.setLoginIp(ip); // 设置登录 IP
            userLoginInfoRepository.save(loginInfo);
            return user.getId();
        }
        return null;
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
