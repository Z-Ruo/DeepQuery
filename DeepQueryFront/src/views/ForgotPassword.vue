<template>
  <div class="box">
    <div class="forgot-password-container">
      <h2 class="forgot-password-title">找回密码</h2>
      <el-form @submit.prevent="recoverPassword" class="forgot-password-form">
        <el-form-item label="电话" class="form-item">
          <el-input v-model="phone" placeholder="请输入电话" />
          <div v-if="phoneError" class="error">{{ phoneError }}</div>
        </el-form-item>
        <el-form-item label="验证码" class="form-item">
          <div style="display: flex; align-items: center">
            <el-input
              v-model="verificationCode"
              placeholder="请输入验证码"
              style="flex: 1"
            />
            <el-button
              type="button"
              @click="getVerificationCode"
              style="margin-left: 10px"
              >获取验证码</el-button
            >
          </div>
          <div v-if="verificationCodeError" class="error">
            {{ verificationCodeError }}
          </div>
        </el-form-item>
        <el-form-item class="form-item">
          <el-button
            type="primary"
            native-type="submit"
            class="forgot-password-button"
            >找回密码</el-button
          >
        </el-form-item>
      </el-form>
      <div v-if="errorMessage" class="error">{{ errorMessage }}</div>
      <div class="links">
        <router-link to="/">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { getVerificationCode, recoverPassword } from "../api/auth"; // 导入 API

export default {
  data() {
    return {
      phone: "",
      verificationCode: "",
      errorMessage: "",
      phoneError: "",
      verificationCodeError: "",
    };
  },
  methods: {
    async getVerificationCode() {
      this.errorMessage = "";
      this.phoneError = "";

      const phoneRegex = /^1(3\d|4[5-9]|5[0-35-9]|6[5-6]|7[0-8]|8\d|9\d)\d{8}$/;
      if (!phoneRegex.test(this.phone)) {
        this.phoneError = "电话号码格式不正确";
        return;
      }

      try {
        const response = await getVerificationCode(this.phone);
        console.log("获取验证码成功", response.data);
      } catch (error) {
        this.errorMessage = "获取验证码失败，请重试";
      }
    },
    async recoverPassword() {
      this.errorMessage = "";
      this.phoneError = "";
      this.verificationCodeError = "";

      const phoneRegex = /^1(3\d|4[5-9]|5[0-35-9]|6[5-6]|7[0-8]|8\d|9\d)\d{8}$/;
      if (!phoneRegex.test(this.phone)) {
        this.phoneError = "电话号码格式不正确";
        return;
      }

      if (this.verificationCode.length === 0) {
        this.verificationCodeError = "验证码不能为空";
        return;
      }

      try {
        const response = await recoverPassword(
          this.phone,
          this.verificationCode
        );
        console.log("找回密码成功", response.data);
      } catch (error) {
        this.errorMessage = "找回密码失败，请重试";
      }
    },
  },
};
</script>

<style scoped>
.box {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 97.7vh; /* 使容器占满视口高度 */
  position: relative;
  overflow: hidden;
}

.box::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(160deg, #f0f7ff 0%, #e1eeff 100%);
  z-index: 0;
}

.box::after {
  content: '';
  position: absolute;
  bottom: -10%;
  right: -20%;
  width: 140%;
  height: 40%;
  background: #fff;
  border-radius: 50% 50% 0 0;
  transform: rotate(5deg);
  box-shadow: 0 -10px 30px rgba(64,158,255,0.1);
  z-index: 1;
}

.forgot-password-container {
  width: 320px;
  padding: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 2;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(10px);
}

.forgot-password-title {
  text-align: center;
  margin-top: 10px;
}

.forgot-password-form {
  margin: 0 20px;
}

.form-item {
  margin-bottom: 22px;
  position: relative; /* 使子元素绝对定位相对于此元素 */
  text-align: right; /* 文字右对齐 */
}

.error {
  color: #f56c6c;
  position: absolute;
  left: 0;
  bottom: -25px; /* 增加与输入框的距离 */
  font-size: 12px;
}

.links {
  margin-top: 10px; /* 添加顶部间距 */
  text-align: right; /* 右对齐 */
  width: 100%; /* 使链接占满整个宽度 */
}

.links a {
  color: #409eff;
  text-decoration: none;
}

.links a:hover {
  text-decoration: underline;
}

.forgot-password-button {
  width: 100%; /* 找回密码按钮宽度占满 */
}
</style> 