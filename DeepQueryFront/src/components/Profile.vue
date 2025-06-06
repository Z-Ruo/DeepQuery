<template>
  <div class="profile">
    <h2 class="page-title">个人中心</h2>
    <el-card class="profile-card">
      <el-form @submit.prevent="updatePassword" class="profile-form">
        <el-form-item label="电话">
          <el-input
            v-model="phone"
            placeholder="请输入您的电话"
            clearable
          />
        </el-form-item>
        <el-form-item label="原密码">
          <el-input
            v-model="oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="confirmNewPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" class="submit-button">
            修改密码
          </el-button>
        </el-form-item>
        <el-alert
          v-if="message"
          :title="message"
          :type="message.includes('失败') ? 'error' : 'success'"
          show-icon
          class="message-alert"
        />
      </el-form>
      
      <div class="logout-section">
        <el-button type="danger" @click="handleLogout" class="logout-button">
          退出登录
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { updatePassword, logout } from '../api/auth';
import { ElMessage } from 'element-plus';

export default {
  data() {
    return {
      phone: '',
      oldPassword: '',
      newPassword: '',
      confirmNewPassword: '',
      message: '',
    };
  },
  methods: {
    async updatePassword() {
      this.message = ''; // 清除之前的消息

      // 表单验证
      if (!this.phone || !this.oldPassword || !this.newPassword) {
        this.message = '所有字段都是必填的';
        return;
      }

      if (this.newPassword !== this.confirmNewPassword) {
        this.message = '新密码和确认新密码不匹配';
        return;
      }

      if (this.newPassword.length < 6) {
        this.message = '新密码长度不能小于6位';
        return;
      }

      try {
        const response = await updatePassword(
          this.phone,
          this.oldPassword,
          this.newPassword
        );
        this.message = '密码修改成功';
        
        // 清空表单
        this.oldPassword = '';
        this.newPassword = '';
        this.confirmNewPassword = '';
        
      } catch (error) {
        this.message = error.message || '修改密码失败，请重试';
      }
    },
    handleLogout() {
      logout();
      this.$router.push('/');
    },
  },
};
</script>

<style scoped>
.profile {
  padding: 24px;
  max-width: 600px;
  margin: 0 auto;
}

.page-title {
  color: #2c3e50;
  text-align: center;
  margin-bottom: 24px;
}

.profile-card {
  padding: 24px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.profile-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.profile-form {
  max-width: 400px;
  margin: 0 auto;
}

.submit-button {
  width: 100%;
  margin-top: 16px;
}

.message-alert {
  margin-top: 16px;
}

.logout-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #ebeef5;
  text-align: center;
}

.logout-button {
  width: 100%;
  max-width: 400px;
}

.logout-button:hover {
  opacity: 0.9;
}
</style> 