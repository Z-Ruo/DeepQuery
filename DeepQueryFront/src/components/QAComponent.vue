<template>
  <div class="qa-component">
    <h2>问答组件</h2>
    <textarea v-model="question" placeholder="请输入您的问题"></textarea>
    <button @click="submitQuestion">提交</button>
    <div v-if="answer">
      <h3>回答：</h3>
      <p>{{ answer }}</p>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      question: '',
      answer: ''
    };
  },
  methods: {
    submitQuestion() {
      // 模拟回答逻辑
      this.answer = `您提出的问题是：${this.question}`;
    }
  }
};
</script>

<style scoped>
.qa-component {
  border: 1px solid #ccc;
  padding: 20px;
  border-radius: 5px;
  max-width: 600px;
  margin: 20px auto;
}

textarea {
  width: 100%;
  height: 100px;
  margin-bottom: 10px;
}

button {
  padding: 10px 20px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

button:hover {
  background-color: #0056b3;
}
</style>
