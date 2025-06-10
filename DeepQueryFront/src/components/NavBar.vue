<template>
  <div class="navbar">
    <div class="logo">
      <img src="@/assets/logo.png" alt="DeepQuery" class="logo-image" />
      <span class="logo-text">DeepQuery</span>
    </div>
    
    <div class="nav-links">
      <router-link to="/" class="nav-link" active-class="active-nav-link">
        <i class="nav-icon chat-icon"></i>
        聊天
      </router-link>
      <router-link to="/sessions" class="nav-link" active-class="active-nav-link">
        <i class="nav-icon sessions-icon"></i>
        会话记录
      </router-link>
      <router-link to="/admin/knowledge-base" class="nav-link" active-class="active-nav-link">
        <i class="nav-icon rag-icon"></i>
        知识库管理
      </router-link>
    </div>
    
    <div class="user-menu">
      <div v-if="isLoggedIn" class="user-profile" @click="toggleUserDropdown" ref="userMenuRef">
        <div class="avatar">{{ userInitials }}</div>
        <span class="username">{{ username }}</span>
        <i class="dropdown-icon"></i>
        
        <div v-if="showUserDropdown" class="dropdown-menu">
          <div class="dropdown-item" @click="goToProfile">
            <i class="profile-icon"></i> 个人资料
          </div>
          <div class="dropdown-item" @click="logout">
            <i class="logout-icon"></i> 退出登录
          </div>
        </div>
      </div>
      <router-link v-else to="/login" class="login-button-navbar">
        登录
      </router-link>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { auth } from '@/api';

export default {
  name: 'NavBar',
  
  setup() {
    const router = useRouter();
    const showUserDropdown = ref(false);
    const username = ref('');
    const isLoggedIn = ref(false);
    const userMenuRef = ref(null); // 用于点击外部关闭dropdown
    
    const userInitials = computed(() => {
      if (!username.value) return '';
      return username.value.charAt(0).toUpperCase();
    });
    
    const checkAuthStatus = () => {
      isLoggedIn.value = auth.isLoggedIn();
      if (isLoggedIn.value) {
        username.value = auth.getCurrentUsername() || '用户';
      }
    };
    
    const toggleUserDropdown = () => {
      showUserDropdown.value = !showUserDropdown.value;
    };
    
    const handleClickOutside = (event) => {
      // 使用 userMenuRef.value 替代 document.querySelector('.user-menu')
      if (userMenuRef.value && !userMenuRef.value.contains(event.target)) {
        showUserDropdown.value = false;
      }
    };
    
    const goToProfile = () => {
      if (router.currentRoute.value.path !== '/profile') {
        router.push('/profile');
      }
      showUserDropdown.value = false;
    };
    
    const logout = async () => {
      try {
        await auth.logout(); // 假设 auth.logout 是一个异步操作
        isLoggedIn.value = false;
        username.value = '';
        showUserDropdown.value = false;
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login');
        }
      } catch (error) {
        console.error("Logout failed:", error);
        // 可以添加一些用户提示，例如使用一个 notification 服务
      }
    };
    
    onMounted(() => {
      checkAuthStatus();
      document.addEventListener('click', handleClickOutside);
    });
    
    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside);
    });
    
    return {
      username,
      isLoggedIn,
      userInitials,
      showUserDropdown,
      toggleUserDropdown,
      goToProfile,
      logout,
      userMenuRef // 导出 ref
    };
  }
}
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 20px;
  background-color: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.logo {
  display: flex;
  align-items: center;
}

.logo-image {
  height: 30px;
  margin-right: 10px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.nav-links {
  display: flex;
  gap: 20px;
}

.nav-link {
  display: flex;
  align-items: center;
  color: #555;
  text-decoration: none;
  font-size: 15px;
  padding: 8px 10px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.nav-link:hover {
  background-color: #f5f5f5;
}

.nav-link.active-nav-link {
  color: #3b82f6;
  font-weight: 600; /* 加粗字体 */
  background-color: #eef2ff; /* 添加背景色 */
}

.nav-icon {
  margin-right: 6px;
  width: 18px;
  height: 18px;
  background-position: center;
  background-repeat: no-repeat;
  background-size: contain;
}

.user-menu {
  position: relative;
}

.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  transition: background-color 0.2s;
}

.user-profile:hover {
  background-color: #f5f5f5;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #3b82f6;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-right: 8px;
}

.username {
  font-weight: 500;
  margin-right: 5px;
}

.dropdown-menu {
  position: absolute;
  top: 45px;
  right: 0;
  width: 180px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  z-index: 10;
}

.dropdown-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background-color: #f5f5f5;
}

.login-button-navbar {
  padding: 8px 15px;
  background-color: #3b82f6;
  color: white;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
  transition: background-color 0.2s, transform 0.1s;
}

.login-button-navbar:hover {
  background-color: #2563eb;
  transform: translateY(-1px);
}

.login-button-navbar:active {
  transform: translateY(0);
}

/* 图标样式 可以用实际图标替换 */
.chat-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'%3E%3C/path%3E%3C/svg%3E");
}

.rag-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z'%3E%3C/path%3E%3Cpath d='M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z'%3E%3C/path%3E%3C/svg%3E");
}

.sessions-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='3' y='4' width='18' height='18' rx='2' ry='2'%3E%3C/rect%3E%3Cline x1='16' y1='2' x2='16' y2='6'%3E%3C/line%3E%3Cline x1='8' y1='2' x2='8' y2='6'%3E%3C/line%3E%3Cline x1='3' y1='10' x2='21' y2='10'%3E%3C/line%3E%3C/svg%3E");
}

.profile-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2'%3E%3C/path%3E%3Ccircle cx='12' cy='7' r='4'%3E%3C/circle%3E%3C/svg%3E");
}

.logout-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4'%3E%3C/path%3E%3Cpolyline points='16 17 21 12 16 7'%3E%3C/polyline%3E%3Cline x1='21' y1='12' x2='9' y2='12'%3E%3C/line%3E%3C/svg%3E");
}

.dropdown-icon {
  width: 12px;
  height: 12px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-position: center;
  background-repeat: no-repeat;
  background-size: contain;
}
</style>
