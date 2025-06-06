package back.controller;

import back.model.LoginRequest;
import back.model.RegisterRequest;
import back.model.LoginResponseModel;
import back.service.LoginService;
import back.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/v1/auth")
public class LoginController {

    @Autowired
    private LoginService loginService;

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @PostMapping("/login")
    public ResponseEntity<LoginResponseModel> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for username: {} from IP: {}", loginRequest.getUsername(), loginRequest.getIp());
        boolean success = loginService.login(loginRequest.getUsername(), loginRequest.getPassword(), loginRequest.getIp());
        if (success) {
            String token = JwtUtil.generateToken(loginRequest.getUsername());
            logger.info("Login successful for username: {}", loginRequest.getUsername());
            return ResponseEntity.ok(new LoginResponseModel("登录成功" , 200, token));
        } else {
            logger.warn("Login failed for username: {}", loginRequest.getUsername());
            return ResponseEntity.status(401).body(new LoginResponseModel("用户名或密码错误", 401, null));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponseModel> register(@RequestBody RegisterRequest registerRequest) {
        boolean success = loginService.registerUser(registerRequest.getUsername(), registerRequest.getPassword(), registerRequest.getPhone());
        if (success) {
            return ResponseEntity.ok(new LoginResponseModel("注册成功", 200, null));
        } else {
            return ResponseEntity.status(400).body(new LoginResponseModel("用户名已存在", 400,null));
        }
    }
}
