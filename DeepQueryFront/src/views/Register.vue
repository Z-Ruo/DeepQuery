<template>
  <div class="box">
    <StarBackground />
    <div class="login-box">
      <div class="title">
        <h2>深度检索</h2>
      </div>      
      <form @submit.prevent="register">
        <div class="login-field">
          <input
              type="text"
              v-model="username"
              required
              autocomplete="off"
          />
          <label>用户名</label>
        </div>
        <div class="login-field">
          <input
              type="password"
              v-model="password"
              required
              autocomplete="off"
          />
          <label>密码</label>
        </div>
        <div class="login-field">
          <input
              type="password"
              v-model="confirmPassword"
              required
              autocomplete="off"
          />
          <label>确认密码</label>
        </div>
        <div class="login-field">
          <input
              type="text"
              v-model="phone"
              required
              autocomplete="off"
          />
          <label>手机号码</label>
        </div>
        <div class="links">
          <router-link to="/">立即登录</router-link>
        </div>
        <button type="submit">注册</button>
      </form>
    </div>
  </div>
</template>

<script>
import { register } from "../api/auth";
import { ElMessage } from "element-plus";
import StarBackground from '../components/StarBackground.vue'

export default {
  components: {
    StarBackground
  },  data() {
    return {
      username: "",
      password: "",
      confirmPassword: "",
      phone: "",
    };
  },
  methods: {
    async register() {
      if (!this.username || !this.password || !this.phone) {
        ElMessage.error("所有字段都必须填写");
        return;
      }

      const phoneRegex = /^1(3\d|4[5-9]|5[0-35-9]|6[5-6]|7[0-8]|8\d|9\d)\d{8}$/;
      if (!phoneRegex.test(this.phone)) {
        ElMessage.error("手机号码格式不正确");
        return;
      }

      if (this.password.length < 6) {
        ElMessage.error("密码长度必须大于等于6位");
        return;
      }

      if (this.password !== this.confirmPassword) {
        ElMessage.error("两次输入的密码不一致");
        return;
      }

      try {
        const response = await register(this.username, this.password, this.phone);
        if (response.status === 200) {
          ElMessage.success(response.message || "注册成功");
          this.$router.push('/login');
        } else {
          ElMessage.error(response.message || "注册失败");
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || "注册失败，请稍后重试");
      }
    }
  }
};
</script>

<style scoped>
.box {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  position: relative;
  overflow: hidden;
}

.login-box {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 40px;
  background: #b6e1ff; /* 统一背景 */
  box-sizing: border-box;
  box-shadow: 4px 7px 19px rgba(0, 0, 0, 0.4); /* 统一阴影 */
  border-radius: 20px; /* 统一圆角 */
  opacity: 0.7; /* 统一透明度 */
}

.title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.title img {
  width: 60px;
  height: 60px;
  vertical-align: middle;
  margin-top: -12px;
  margin-left: -12px;
}

.login-box h2 {
  margin-left: 12px;
  letter-spacing: 4px;
  padding: 0;
  text-align: center;
  color: #235074;
}

.login-field {
  position: relative;
  margin-bottom: 30px;
}

.login-field input {
  width: 100%;
  padding: 10px 0;
  font-size: 16px;
  color: #235074;
  border: none;
  border-bottom: 1px solid #fff;
  outline: none;
  background: transparent;
}

.login-field label {
  position: absolute;
  top: 0;
  left: 0;
  padding: 10px 0;
  font-size: 16px;
  color: #235074;
  pointer-events: none;
  transition: 0.5s;
}

.login-field input:focus ~ label,
.login-field input:valid ~ label {
  top: -20px;
  left: 0;
  color: #03a9f4;
  font-size: 12px;
}

.links {
  text-align: right;
  margin: 20px 0;
}

.links a {
  color: #235074;
  text-decoration: none;
  font-size: 14px;
}

.links a:hover {
  text-decoration: underline;
}

button {
  width: 100%;
  background: #03a9f4; /* 统一按钮颜色 */
  color: #fff;
  padding: 10px 20px;
  border: none;
  border-radius: 10px; /* 统一按钮圆角 */
  cursor: pointer;
  font-size: 16px;
  transition: 0.3s;
}

button:hover {
  background: #0388c4; /* 统一按钮悬停颜色 */
}
</style>