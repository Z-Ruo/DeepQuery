<template>
  <div class="profile-popup" v-if="visible">
    <div class="popup-overlay" @click="close"></div>
    <div class="popup-content">
      <div class="popup-header">
        <img src="../assets/gerenzhongxin.svg" alt="个人中心图标" class="header-icon" />
        <h3>个人中心</h3>
        <button class="close-button" @click="close">×</button>
      </div>
      <form @submit.prevent="updatePassword" class="profile-form">
        <div class="profile-field">
          <input
              type="text"
              v-model="phone"
              required
              autocomplete="off"
          />
          <label>电话</label>
        </div>
        <div class="profile-field">
          <input
              type="password"
              v-model="oldPassword"
              required
              autocomplete="off"
          />
          <label>原密码</label>
        </div>
        <div class="profile-field">
          <input
              type="password"
              v-model="newPassword"
              required
              autocomplete="off"
          />
          <label>新密码</label>
        </div>
        <div class="button-row">
          <button type="submit" class="update-button">修改密码</button>
          <button type="button" class="logout-button" @click="handleLogout">退出登录</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import { logout } from '../api/auth';

export default {
  name: 'ProfilePopup',
  props: {
    visible: Boolean
  },
  data() {
    return {
      phone: '',
      oldPassword: '',
      newPassword: '',
      message: ''
    };
  },
  methods: {
    close() {
      this.$emit('update:visible', false);
    },
    async updatePassword() {
      this.message = '';
      if (!this.phone || !this.oldPassword || !this.newPassword) {
        this.message = '所有字段都是必填的';
        return;
      }
      if (this.newPassword.length < 6) {
        this.message = '新密码长度不能小于6位';
        return;
      }
      try {
        await updatePassword(this.phone, this.oldPassword, this.newPassword);
        this.message = '密码修改成功';
        this.oldPassword = '';
        this.newPassword = '';
      } catch (error) {
        this.message = error.message || '修改密码失败，请重试';
      }
    },
    handleLogout() {
      logout();
      this.$router.push('/');
    }
  }
};
</script>

<style scoped>
.profile-popup {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  background: rgba(31, 31, 31, 0.34);
}

.popup-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(86, 86, 86, 0.5);
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.popup-content {
  position: relative;
  width: 400px;
  padding: 40px;
  background: linear-gradient(to bottom right, #c5e0f3, #a0c4e0, #b2c9ef);
  border-radius: 20px;
  opacity: 1;
}

.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.popup-header h3 {
  margin: 0;
  font-size: 18px;
  color: #235074;
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  width: 24px;
  height: 24px;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #909399;
  transition: all 0.3s ease;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  background-color: #f5f7fa;
  color: #409EFF;
}

.profile-form {
  max-width: 400px;
  margin: 0 auto;
  padding: 20px 0;
}

.profile-field {
  position: relative;
  margin-bottom: 30px;
}

.profile-field input {
  width: 100%;
  padding: 10px 0;
  font-size: 16px;
  color: #235074;
  border: none;
  border-bottom: 1px solid #fff;
  outline: none;
  background: transparent;
}

.profile-field label {
  position: absolute;
  top: 0;
  left: 0;
  padding: 10px 0;
  font-size: 16px;
  color: #235074;
  pointer-events: none;
  transition: 0.5s;
}

.profile-field input:focus ~ label,
.profile-field input:valid ~ label {
  top: -20px;
  left: 0;
  color: #03a9f4;
  font-size: 12px;
}

.button-row {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

button {
  width: 48%;
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

.logout-button {
  background: #f56c6c;
}

.logout-button:hover {
  background: #d9534f;
}
</style>
