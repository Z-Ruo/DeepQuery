<template>
  <div class="login-container">
    <div class="login-form">
      <h2 class="login-title">{{ isRegister ? '注册账号' : '用户登录' }}</h2>
      
      <div class="form-group">
        <label for="username">用户名</label>
        <input 
          id="username" 
          v-model="formData.username" 
          type="text" 
          placeholder="请输入用户名"
          class="form-input"
        />
      </div>
      
      <div class="form-group">
        <label for="password">密码</label>
        <input 
          id="password" 
          v-model="formData.password" 
          type="password" 
          placeholder="请输入密码"
          class="form-input"
        />
      </div>
      
      <div v-if="isRegister" class="form-group">
        <label for="phone">手机号</label>
        <input 
          id="phone" 
          v-model="formData.phone" 
          type="text" 
          placeholder="请输入手机号"
          class="form-input"
        />
      </div>
      
      <div class="error-message" v-if="errorMsg">{{ errorMsg }}</div>
      
      <button 
        @click="handleSubmit" 
        class="login-button"
        :disabled="loading"
      >
        {{ loading ? '处理中...' : (isRegister ? '注册' : '登录') }}
      </button>
      
      <div class="form-actions">
        <a href="#" @click.prevent="toggleForm">
          {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
        </a>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue';
import { auth } from '../api';
import { useRouter } from 'vue-router';

export default {
  name: 'LoginForm',
  setup() {
    const router = useRouter();
    const isRegister = ref(false);
    const loading = ref(false);
    const errorMsg = ref('');
    
    const formData = reactive({
      username: '',
      password: '',
      phone: ''
    });
    
    const toggleForm = () => {
      isRegister.value = !isRegister.value;
      errorMsg.value = '';
      // 重置表单数据
      formData.username = '';
      formData.password = '';
      formData.phone = '';
    };
    
    const validatePhoneNumber = (phone) => {
      // 简单的11位数字校验
      const phoneRegex = /^1[3-9]\d{9}$/;
      return phoneRegex.test(phone);
    };

    const handleSubmit = async () => {
      errorMsg.value = ''; // 重置错误信息

      if (!formData.username) {
        errorMsg.value = '用户名不能为空';
        return;
      }
      
      if (!formData.password) {
        errorMsg.value = '密码不能为空';
        return;
      }

      if (formData.password.length < 6) {
        errorMsg.value = '密码长度不能少于6位';
        return;
      }
      
      if (isRegister.value) {
        if (!formData.phone) {
          errorMsg.value = '手机号不能为空';
          return;
        }
        if (!validatePhoneNumber(formData.phone)) {
          errorMsg.value = '请输入有效的11位手机号';
          return;
        }
      }
      
      try {
        loading.value = true;
        errorMsg.value = '';
        
        if (isRegister.value) {
          const result = await auth.register(formData.username, formData.password, formData.phone);
          if (result && result.code === 200) {
            // 注册成功，切换到登录
            isRegister.value = false;
            errorMsg.value = '注册成功，请登录';
            formData.password = ''; // 清空密码以便用户重新输入登录
            // formData.username 保持不变，方便用户直接登录
          } else {
            errorMsg.value = (result && result.message) || '注册失败，请稍后再试';
          }
        } else { // Login logic
          const result = await auth.login(formData.username, formData.password);
          if (result && result.status === 200) {
            try {
              await router.push('/chat');
           } catch (navError) {
              console.error("Navigation to /chat failed:", navError); // For developer debugging
              errorMsg.value = '登录成功，但页面跳转失败。'; 
            }
          } else {
            errorMsg.value = (result && result.message) || '用户名或密码错误';
          }
        }
      } catch (error) {
        console.error("Login/Register error:", error); // For developer debugging
        errorMsg.value = error.message || (isRegister.value ? '注册接口失败' : '登录接口失败');
      } finally {
        loading.value = false;
      }
    };
    
    return {
      isRegister,
      formData,
      loading,
      errorMsg,
      toggleForm,
      handleSubmit
    };
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f7f9fc;
}

.login-form {
  width: 400px;
  padding: 30px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  margin-bottom: 24px;
  color: #333;
  font-weight: 600;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #555;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 15px;
  transition: border-color 0.3s;
}

.form-input:focus {
  border-color: #3b82f6;
  outline: none;
}

.login-button {
  width: 100%;
  padding: 12px;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

.login-button:hover {
  background-color: #2563eb;
}

.login-button:disabled {
  background-color: #93c5fd;
  cursor: not-allowed;
}

.error-message {
  color: #dc2626;
  margin-bottom: 16px;
  font-size: 14px;
}

.form-actions {
  margin-top: 16px;
  text-align: center;
}

.form-actions a {
  color: #3b82f6;
  text-decoration: none;
  font-size: 14px;
}

.form-actions a:hover {
  text-decoration: underline;
}
</style>
