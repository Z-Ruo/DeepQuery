<template>
  <div id="app-container">
    <NavBar />
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue';
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

const handleAuthError = () => {
  console.log('Authentication error detected, redirecting to login.');
  alert('您的会话已过期或无效，请重新登录。');
  router.push('/login').catch(err => {
    console.warn('Redirect to login failed from App.vue:', err);
    if (window.location.pathname !== '/login') {
      window.location.pathname = '/login';
    }
  });
};

onMounted(() => {
  window.addEventListener('auth-error', handleAuthError);
});

onUnmounted(() => {
  window.removeEventListener('auth-error', handleAuthError);
});

</script>

<style>
/* Styles for App.vue, can remain scoped or be global depending on needs */
#app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex-grow: 1;
  padding-top: 60px; /* Adjust if NavBar height changes */
  /* Add other styling for the main content area as needed */
}
</style>
