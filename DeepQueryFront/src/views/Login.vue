<template>
  <div class="box">
    <StarBackground />
    <div class="login-box">
      <div class="title">
        <img src="../assets/logo.png">
        <h2>徽算</h2>
      </div>      <form @submit.prevent="login">
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
        <div class="links">
          <router-link to="/register">立即注册</router-link>
        </div>
        <button type="submit">登录</button>
      </form>
    </div>
  </div>
</template>

<script>
import { login } from "../api/auth";
import { ElMessage } from "element-plus";
import StarBackground from '../components/StarBackground.vue'

export default {
  components: {
    StarBackground
  },  data() {
    return {
      username: "",
      password: "",
    };
  },
  methods: {
    async login() {
      if (!this.username || !this.password) {
        ElMessage.error("用户名和密码不能为空");
        return;
      }

      if (this.password.length < 6) {
        ElMessage.error("密码长度必须大于等于6位");
        return;
      }

      try {
        const response = await login(this.username, this.password);
        if (response.status === 200) {
          ElMessage.success(response.message || "登录成功");
          this.$router.push("/home");
        } else {
          ElMessage.error(response.message || "登录失败");
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || "登录失败，请检查您的用户名和密码");
      }
    }
  },
  created() {
    if (localStorage.getItem("token")) {
      this.$router.push("/home");
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
  background: #b6e1ff;
  box-sizing: border-box;
  box-shadow: 4px 7px 19px rgba(0, 0, 0, 0.4);
  border-radius: 20px;
  opacity: 0.7;
}
.title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.title img {
  width: 60px;  /* 根据实际图片比例调整 */
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
  color: #235074 ;
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
  background: #03a9f4;
  color: #fff;
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 16px;
  transition: 0.3s;
}

button:hover {
  background: #0388c4;
}
</style>