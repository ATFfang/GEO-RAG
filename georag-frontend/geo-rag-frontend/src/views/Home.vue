<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { userApi } from '../api/user'; // 引入你的 API

const router = useRouter();

// --- 类型定义 ---
interface ChatMessage {
  id: number;
  role: 'user' | 'ai';
  content: string;
  type?: 'text' | 'image' | 'file'; // 消息类型
  fileName?: string; // 文件名（如果是文件类型）
  timestamp: number;
}

interface ChatSession {
  id: string;
  title: string;
  messages: ChatMessage[];
  updatedAt: number;
}

// --- 状态管理 ---
const showSidebar = ref(true);
const isTyping = ref(false);
const inputMessage = ref('');
const showTools = ref(false); // 工具栏展开状态
const fileInput = ref<HTMLInputElement | null>(null);

// 用户信息
const userInfo = ref({ nickname: 'Traveler', avatar: '' });

// 会话数据
const chatHistory = ref<ChatSession[]>([]);
const currentChatId = ref<string>('');
const currentMessages = computed(() => {
  const session = chatHistory.value.find(s => s.id === currentChatId.value);
  return session ? session.messages : [];
});

// DOM 引用
const chatContainer = ref<HTMLElement | null>(null);
const textareaRef = ref<HTMLTextAreaElement | null>(null);
let typingInterval: any = null;

// --- 初始化与 API ---

onMounted(async () => {
  // 1. 获取用户信息
  try {
    const res = await userApi.getProfile();
    if (res.code === 200 && res.data) {
      userInfo.value = {
        nickname: res.data.nickname || res.data.username,
        avatar: res.data.avatar || ''
      };
    }
  } catch (e) {
    console.error('获取用户信息失败', e);
  }

  // 2. 加载本地存储的聊天记录
  const storedHistory = localStorage.getItem('geo_chat_history');
  if (storedHistory) {
    chatHistory.value = JSON.parse(storedHistory);
  }

  // 3. 初始化会话
  if (chatHistory.value.length === 0) {
    createNewChat();
  } else {
    // 默认选中最新的一个
    currentChatId.value = chatHistory.value[0]!.id;
    scrollToBottom();
  }
});

// 监听聊天记录变化，自动保存到本地
watch(chatHistory, (newVal) => {
  localStorage.setItem('geo_chat_history', JSON.stringify(newVal));
}, { deep: true });

// --- 会话逻辑 ---

// 生成唯一ID
const generateId = () => Math.random().toString(36).substring(2, 9);

// 创建新会话
const createNewChat = () => {
  const newId = generateId();
  const newSession: ChatSession = {
    id: newId,
    title: 'New Chat',
    messages: [],
    updatedAt: Date.now()
  };
  chatHistory.value.unshift(newSession); // 加到最前
  currentChatId.value = newId;
  inputMessage.value = '';
  showSidebar.value = true; // 移动端自动打开侧边栏体验可能不好，可视情况调整
  if (textareaRef.value) textareaRef.value.focus();
};

// 切换会话
const switchChat = (id: string) => {
  currentChatId.value = id;
  scrollToBottom();
  if (window.innerWidth < 1024) showSidebar.value = false; // 移动端切换后自动收起
};

// 删除会话
const deleteChat = (e: Event, id: string) => {
  e.stopPropagation(); // 阻止冒泡
  const index = chatHistory.value.findIndex(s => s.id === id);
  if (index !== -1) {
    chatHistory.value.splice(index, 1);
    // 如果删除了当前会话
    if (currentChatId.value === id) {
      if (chatHistory.value.length > 0) {
        currentChatId.value = chatHistory.value[0]!.id;
      } else {
        createNewChat();
      }
    }
  }
};

// 更新会话标题（取第一条消息的前20个字）
const updateSessionTitle = (sessionId: string, firstMessage: string) => {
  const session = chatHistory.value.find(s => s.id === sessionId);
  if (session && session.title === 'New Chat') {
    session.title = firstMessage.length > 20 ? firstMessage.substring(0, 20) + '...' : firstMessage;
  }
};

// --- 发送逻辑 ---

const scrollToBottom = async (smooth = true) => {
  await nextTick();
  if (chatContainer.value) {
    chatContainer.value.scrollTo({
      top: chatContainer.value.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto'
    });
  }
};

const adjustTextareaHeight = () => {
  const el = textareaRef.value;
  if (el) {
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 128)}px`;
  }
};

const handleSend = async () => {
  const text = inputMessage.value.trim();
  if (!text || isTyping.value) return;

  const sessionId = currentChatId.value;
  const currentSession = chatHistory.value.find(s => s.id === sessionId);
  if (!currentSession) return;

  // 1. 用户消息
  const userMsg: ChatMessage = {
    id: Date.now(),
    role: 'user',
    content: text,
    type: 'text',
    timestamp: Date.now()
  };
  
  currentSession.messages.push(userMsg);
  currentSession.updatedAt = Date.now();
  
  // 更新标题
  if (currentSession.messages.length === 1) {
    updateSessionTitle(sessionId, text);
  }
  
  // 排序会话（把最新的顶上去）
  const sessionIndex = chatHistory.value.findIndex(s => s.id === sessionId);
  if (sessionIndex > 0) {
    const [s] = chatHistory.value.splice(sessionIndex, 1) || [];
    if (s) chatHistory.value.unshift(s);
  }

  inputMessage.value = '';
  if (textareaRef.value) textareaRef.value.style.height = 'auto';
  await scrollToBottom();

  // 2. AI 响应
  isTyping.value = true;
  
  setTimeout(() => {
    const aiMsg: ChatMessage = {
      id: Date.now() + 1,
      role: 'ai',
      content: '',
      type: 'text',
      timestamp: Date.now()
    };
    currentSession.messages.push(aiMsg);

    const replyText = "I've received your data. Analyzing spatial coordinates...\n\nThis is a demo response. In a real environment, I would connect to the backend API to process your request.";
    let i = 0;
    
    typingInterval = setInterval(() => {
      aiMsg.content += replyText.charAt(i);
      i++;
      scrollToBottom(false);
      if (i >= replyText.length) {
        clearInterval(typingInterval);
        isTyping.value = false;
      }
    }, 20);
  }, 600);
};

// --- 工具栏与上传 ---

const triggerFileUpload = (type: 'image' | 'file') => {
  if (fileInput.value) {
    fileInput.value.accept = type === 'image' ? 'image/*' : '*/*';
    fileInput.value.click();
  }
  showTools.value = false;
};

const handleFileChange = (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;

  const sessionId = currentChatId.value;
  const currentSession = chatHistory.value.find(s => s.id === sessionId);
  if (!currentSession) return;

  // 模拟上传消息
  const msg: ChatMessage = {
    id: Date.now(),
    role: 'user',
    content: `Uploaded: ${file.name}`,
    type: file.type.startsWith('image/') ? 'image' : 'file',
    fileName: file.name,
    timestamp: Date.now()
  };
  
  // 如果是图片，这里简单用 createObjectURL 预览
  if (msg.type === 'image') {
    msg.content = URL.createObjectURL(file);
  }

  currentSession.messages.push(msg);
  scrollToBottom();
  
  // 触发一个简单的 AI 回复
  setTimeout(() => {
    const aiMsg: ChatMessage = {
      id: Date.now() + 1,
      role: 'ai',
      content: `I successfully received the ${msg.type}. Processing data...`,
      type: 'text',
      timestamp: Date.now()
    };
    currentSession.messages.push(aiMsg);
    scrollToBottom();
  }, 1000);
};

const formatTime = (ts: number) => {
  return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};
</script>

<template>
  <div class="flex h-screen w-full bg-[#121212] text-white overflow-hidden font-sans selection:bg-blue-500/30">
    
    <aside :class="['flex-shrink-0 bg-[#1a1a1a] border-r border-white/5 transition-all duration-300 flex flex-col', showSidebar ? 'w-72' : 'w-0 overflow-hidden']">
      <div class="p-4 space-y-4">
        <div class="flex items-center justify-between px-2">
           <span class="text-lg font-bold font-mono tracking-wider text-gray-200">GEO-RAG</span>
           <button @click="showSidebar = false" class="lg:hidden text-gray-400">✕</button>
        </div>
        
        <button @click="createNewChat" 
          class="w-full flex items-center gap-3 px-4 py-3 bg-white/5 hover:bg-white/10 border border-white/5 rounded-xl transition-all text-sm font-medium text-gray-200 group">
          <span class="w-5 h-5 flex items-center justify-center bg-white/10 rounded-full group-hover:bg-white/20">+</span>
          New Chat
        </button>
      </div>

      <div class="flex-1 overflow-y-auto px-3 custom-scrollbar">
        <div v-if="chatHistory.length > 0" class="text-xs font-semibold text-gray-500 px-3 mb-2 mt-2">Recent</div>
        <div class="space-y-1">
          <div v-for="chat in chatHistory" :key="chat.id"
            @click="switchChat(chat.id)"
            class="group relative flex items-center gap-3 px-3 py-3 rounded-lg text-sm transition-colors cursor-pointer"
            :class="chat.id === currentChatId ? 'bg-[#2a2a2a] text-white' : 'text-gray-400 hover:bg-[#222] hover:text-gray-200'"
          >
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4 shrink-0 opacity-70">
              <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 0 1 .865-.501 48.172 48.172 0 0 0 3.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0 0 12 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018Z" />
            </svg>
            
            <span class="truncate flex-1">{{ chat.title }}</span>

            <button @click="(e) => deleteChat(e, chat.id)" 
              class="absolute right-2 p-1 rounded-md text-gray-500 hover:text-red-400 hover:bg-white/10 opacity-0 group-hover:opacity-100 transition-opacity"
              :class="{ 'opacity-100': chat.id === currentChatId }"
            >
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-4 h-4">
                <path stroke-linecap="round" stroke-linejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div class="p-4 border-t border-white/5">
        <div @click="router.push('/profile')" class="flex items-center gap-3 p-2 rounded-xl hover:bg-[#2a2a2a] cursor-pointer transition-colors group">
          <img :src="userInfo.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'" class="w-9 h-9 rounded-full bg-gray-700 object-cover" />
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-white truncate">{{ userInfo.nickname }}</div>
            <div class="text-xs text-gray-500 group-hover:text-blue-400 transition-colors">View Profile</div>
          </div>
        </div>
      </div>
    </aside>

    <main class="flex-1 flex flex-col relative min-w-0 bg-[#121212]">
      
      <header class="h-16 flex items-center justify-between px-6 border-b border-white/5 bg-[#121212]/80 backdrop-blur-md sticky top-0 z-20">
        <div class="flex items-center gap-4">
          <button @click="showSidebar = !showSidebar" class="p-2 -ml-2 rounded-lg text-gray-400 hover:text-white hover:bg-white/5 transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
            </svg>
          </button>
          <div class="flex items-center gap-2">
            <span class="text-sm font-semibold text-gray-200">Geo-Spatial Model</span>
            <span class="px-1.5 py-0.5 rounded text-[10px] bg-blue-500/20 text-blue-400 border border-blue-500/20">Beta</span>
          </div>
        </div>
      </header>

      <div ref="chatContainer" class="flex-1 overflow-y-auto px-4 py-6 custom-scrollbar">
        <div class="max-w-3xl mx-auto w-full space-y-6">
          
          <div v-if="currentMessages.length === 0" class="flex flex-col items-center justify-center min-h-[50vh] text-center space-y-6 opacity-0 animate-fade-in" style="animation-fill-mode: forwards;">
            <div class="w-16 h-16 rounded-2xl bg-gradient-to-br from-blue-500/20 to-purple-500/20 flex items-center justify-center mb-4">
              <span class="text-3xl">🌍</span>
            </div>
            <h2 class="text-2xl font-bold text-white">Hi, {{ userInfo.nickname }}</h2>
            <p class="text-gray-500 max-w-md">I'm ready to assist with your geographic data analysis. Upload a file or start a conversation.</p>
          </div>

          <div v-for="msg in currentMessages" :key="msg.id" class="flex gap-4" :class="msg.role === 'user' ? 'flex-row-reverse' : ''">
            
            <div class="flex-shrink-0 mt-1">
              <img v-if="msg.role === 'user'" :src="userInfo.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'" class="w-8 h-8 rounded-full bg-gray-800 object-cover" />
              <div v-else class="w-8 h-8 rounded-full bg-[#2a2a2a] border border-white/10 flex items-center justify-center">
                <span class="text-xs">🤖</span>
              </div>
            </div>

            <div class="flex flex-col max-w-[85%] md:max-w-[75%]" :class="msg.role === 'user' ? 'items-end' : 'items-start'">
              
              <div v-if="msg.type === 'text'" 
                class="px-5 py-3 rounded-2xl text-sm leading-relaxed shadow-sm whitespace-pre-wrap break-words"
                :class="msg.role === 'user' ? 'bg-[#2563eb] text-white rounded-tr-sm' : 'bg-[#2a2a2a] text-gray-200 border border-white/5 rounded-tl-sm'">
                {{ msg.content }}
                <span v-if="msg.role === 'ai' && isTyping && msg.id === currentMessages[currentMessages.length - 1]?.id" class="inline-block w-1.5 h-4 ml-1 align-middle bg-blue-400 animate-pulse"></span>
              </div>

              <div v-else-if="msg.type === 'image'" class="rounded-xl overflow-hidden border border-white/10 max-w-sm">
                <img :src="msg.content" class="w-full h-auto" />
              </div>

              <div v-else-if="msg.type === 'file'" class="flex items-center gap-3 p-3 rounded-xl bg-[#2a2a2a] border border-white/10 max-w-xs">
                <div class="w-10 h-10 rounded-lg bg-white/5 flex items-center justify-center text-xl">📄</div>
                <div class="overflow-hidden">
                  <div class="text-sm font-medium text-white truncate">{{ msg.fileName }}</div>
                  <div class="text-xs text-gray-500">File uploaded</div>
                </div>
              </div>

              <span class="text-[10px] text-gray-600 mt-1.5 px-1">{{ formatTime(msg.timestamp) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="p-6 bg-[#121212]">
        <div class="max-w-3xl mx-auto relative">
          
          <div class="relative flex items-end gap-2 bg-[#1a1a1a] rounded-xl p-2 border border-white/10 focus-within:border-white/20 focus-within:bg-[#1f1f1f] transition-all duration-300">
            
            <div class="relative flex-shrink-0">
              <button @click="showTools = !showTools" class="p-2.5 text-gray-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5 transition-transform duration-300" :class="showTools ? 'rotate-45' : ''">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
              </button>

              <transition name="scale">
                <div v-if="showTools" class="absolute bottom-full left-0 mb-3 w-40 p-1.5 bg-[#252525] border border-white/10 rounded-xl shadow-xl flex flex-col gap-1 z-30 origin-bottom-left">
                  <button @click="triggerFileUpload('image')" class="flex items-center gap-3 px-3 py-2 text-xs font-medium text-gray-300 hover:bg-white/5 hover:text-white rounded-lg transition-colors text-left w-full">
                    <span class="text-purple-400 text-sm">🖼️</span> Upload Image
                  </button>
                  <button @click="triggerFileUpload('file')" class="flex items-center gap-3 px-3 py-2 text-xs font-medium text-gray-300 hover:bg-white/5 hover:text-white rounded-lg transition-colors text-left w-full">
                    <span class="text-blue-400 text-sm">📄</span> Upload File
                  </button>
                </div>
              </transition>
              
              <input type="file" ref="fileInput" class="hidden" @change="handleFileChange" />
            </div>

            <textarea 
              ref="textareaRef"
              v-model="inputMessage"
              @input="adjustTextareaHeight"
              @keydown.enter.prevent="handleSend"
              placeholder="Ask anything about geo-spatial..."
              rows="1"
              class="w-full bg-transparent text-white placeholder-gray-500 text-sm px-2 py-3 focus:outline-none resize-none custom-scrollbar"
              style="min-height: 44px; max-height: 150px;"
            ></textarea>

            <button 
              @click="handleSend"
              :disabled="!inputMessage.trim() || isTyping"
              class="p-2.5 rounded-lg transition-all duration-300 flex-shrink-0 mb-0.5"
              :class="inputMessage.trim() && !isTyping ? 'bg-white text-black hover:bg-gray-200' : 'bg-white/5 text-gray-600 cursor-not-allowed'"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-4 h-4">
                <path d="M3.478 2.404a.75.75 0 0 0-.926.941l2.432 7.905H13.5a.75.75 0 0 1 0 1.5H4.984l-2.432 7.905a.75.75 0 0 0 .926.94 60.519 60.519 0 0 0 18.445-8.986.75.75 0 0 0 0-1.218A60.517 60.517 0 0 0 3.478 2.404Z" />
              </svg>
            </button>
          </div>
          
          <div class="text-center mt-3">
            <p class="text-[10px] text-gray-600">
              Geo-RAG can make mistakes. Verify important info.
            </p>
          </div>
        </div>
      </div>

      <div v-if="showTools" @click="showTools = false" class="fixed inset-0 z-20 bg-transparent cursor-default"></div>

    </main>
  </div>
</template>

<style scoped>
/* 自定义滚动条 */
.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 20px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

/* 简单的淡入 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.animate-fade-in {
  animation: fadeIn 0.6s ease-out;
}

/* 菜单弹出动画 */
.scale-enter-active,
.scale-leave-active {
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}
.scale-enter-from,
.scale-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(5px);
}
</style>