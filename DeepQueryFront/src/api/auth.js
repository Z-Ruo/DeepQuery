import axios from 'axios';

// 修改 IP 地址为 111.229.211.148
const ip = "111.229.211.148";

// 登录 API
export const login = (username, password) => {
  return axios.post('/auth/login', { username, password, ip }).then(response => {
    if (response.data.code === 200 && response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('username', username);
    }
    return response.data;
  });
};

// 注册 API
export const register = (username, password, phone) => {
  return axios.post('/auth/register', { username, password, phone }, {
    headers: {
      'Content-Type': 'application/json'
    }
  }).then(response => response.data);
};


export async function getChatCompletions(model, messages,kbs,uploadedImagesBase64) {
  const url = axios.defaults.baseURL + '/chat/stream';
  return await fetch(url, {
    method: 'POST',
    body: JSON.stringify({
      model: model,
      messages: messages,
     temperature: 1.0,
    kbs: kbs,
      sessionId:localStorage.getItem('sessionId'),
      images: uploadedImagesBase64,
    }),
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${localStorage.getItem('token')}`
    }
  });
}

//获取相关问题的 API
// export const getRelatedQuestions = (question) => {
//   return axios.post('/related_questions', {
//     question
//   });
// };

// 添加获取相关文献的 API
export const getReferenceFiles = (questionId) => {
  return axios.get(`/reference_files?question_id=${questionId}`, {
    headers: {
      Authorization: `Bearer YOUR_API_KEY`,
      'Content-Type': 'application/json'
    }
  });
};
// 上传图片 API
export const uploadImage = (image) => {
  const formData = new FormData();
  formData.append('image', image);
  return axios.post('/image', formData);
};

// 获取历史记录 API
// export const getHistoryRecords = async () => {
//   try {
//     const response = await axios.post('/history', {
//       userId: localStorage.getItem('userId')
//     }, {
//       headers: {
//         'Content-Type': 'application/json',
//         'Authorization': `Bearer ${localStorage.getItem('token')}`
//       }
//     });
//     return response;
//   } catch (error) {
//     throw error;
//   }
// };


// 修改密码 API
export const updatePassword = async (phone, oldPassword, newPassword) => {
  // 参数验证
  if (!phone || !oldPassword || !newPassword) {
    throw new Error('所有字段都是必填的');
  }

  // 手机号格式验证
  const phoneRegex = /^1[3-9]\d{9}$/;
  if (!phoneRegex.test(phone)) {
    throw new Error('手机号格式不正确');
  }

  // 密码长度验证
  if (newPassword.length < 6) {
    throw new Error('新密码长度不能小于6位');
  }

  try {
    return await axios.post('/update-password', {
      phone,
      oldPassword,
      newPassword
    }, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
  } catch (error) {
    if (error.response) {
      // 服务器返回的错误
      switch (error.response.status) {
        case 400:
          throw new Error('请求参数错误');
        case 401:
          throw new Error('原密码错误');
        case 403:
          throw new Error('没有权限，请重新登录');
        case 500:
          throw new Error('服务器内部错误');
        default:
          throw new Error('修改密码失败');
      }
    }
    throw error;
  }
};


// 解析 SSE 数据流
export const parseSSE = () => {
  let buffer = '';
  let currentEvent = {
    type: 'message',
    data: ''
  };

  return new TransformStream({
    transform(chunk, controller) {
      buffer += chunk;

      while (true) {
        // 处理可能存在的 \r\n 换行符
        const eventEndIndex = buffer.indexOf('\n\n');
        if (eventEndIndex === -1) break;

        const eventData = buffer.substring(0, eventEndIndex);
        buffer = buffer.substring(eventEndIndex + 2);

        // 重置事件对象
        currentEvent = { type: 'message', data: '', id: '', retry: '' };

        eventData.split(/\n|\r\n/).forEach(line => {
          const colonIndex = line.indexOf(':');
          if (colonIndex <= 0) return;

          const field = line.substring(0, colonIndex).trim();
          const value = line.substring(colonIndex + 1).trim();

          switch (field) {
            case 'event':
              currentEvent.type = value;
              break;
            case 'data':
              // 确保只解析JSON部分
              if (value.startsWith('data:')) {
                currentEvent.data += value.substring(5) + '\n';
              } else {
                currentEvent.data += value + '\n';
              }
              break;
            case 'id':
              currentEvent.id = value;
              break;
            case 'retry':
              currentEvent.retry = parseInt(value, 10);
              break;
          }
        });

        // 处理数据内容
        try {
          if (currentEvent.data.startsWith('[DONE]')) {
            controller.enqueue({ type: 'complete' });
          } else if (currentEvent.data) {
            const cleanedData = currentEvent.data.trimEnd();
            // 确保解析的是有效的JSON
            const jsonData = JSON.parse(cleanedData);
            controller.enqueue({
              type: currentEvent.type,
              data: jsonData
            });
          }
        } catch (error) {
          console.error('SSE 解析错误:', error);
          controller.enqueue({
            type: 'error',
            error: `解析错误: ${error.message}`
          });
        }
      }
    }
  });
};


export const logout = () => {
  // 清除所有存储的认证信息
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
};
