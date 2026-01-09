<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

console.log('Home component loaded');
// --- 状态定义 ---
// 定义接口
interface ChatMessage {
  id: number;
  role: 'user' | 'ai';
  content: string;
  timestamp: number;
}

const userInfo = ref({ nickname: 'Traveler', avatar: '' });
const inputMessage = ref('');
const isTyping = ref(false);
const showSidebar = ref(true);
const chatContainer = ref<HTMLElement | null>(null);
const messages = ref<ChatMessage[]>([]);
const textareaRef = ref<HTMLTextAreaElement | null>(null); // 新增 textarea 引用
let typingInterval: ReturnType<typeof setInterval> | null = null; // 用于清除定时器

// --- 生命周期 & 初始化 ---

onMounted(() => {
  // 修复 1: 在客户端挂载后再读取 localStorage，避免 SSR 水合不匹配
  try {
    userInfo.value = {
      nickname: localStorage.getItem('nickname') || 'Traveler',
      avatar: localStorage.getItem('avatar') || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'
    };
  } catch (e) {
    console.warn('LocalStorage access failed', e);
  }
});

onUnmounted(() => {
  // 修复 2: 组件销毁时清除定时器，防止内存泄漏和报错
  if (typingInterval) clearInterval(typingInterval);
});

// 预设的建议问题
const suggestions = [
  { icon: '🗺️', text: 'Plan a trip to Kyoto', sub: 'Include hiking trails' },
  { icon: '💻', text: 'Debug my Vue code', sub: 'Fix reactivity issues' },
  { icon: '🎨', text: 'Generate an image', sub: 'Cyberpunk street style' },
  { icon: '📝', text: 'Write a poem', sub: 'About spatial computing' },
];

// --- 交互逻辑 ---

// 滚动到底部
const scrollToBottom = async (smooth = true) => {
  await nextTick();
  if (chatContainer.value) {
    chatContainer.value.scrollTo({
      top: chatContainer.value.scrollHeight,
      behavior: smooth ? 'smooth' : 'auto' // 打字时不使用 smooth，防止抖动
    });
  }
};

// 修复 3: 输入框高度自适应
const adjustTextareaHeight = () => {
  const el = textareaRef.value;
  if (el) {
    el.style.height = 'auto'; // 重置高度以计算 scrollHeight
    el.style.height = `${Math.min(el.scrollHeight, 128)}px`; // 限制最大高度
  }
};

// 发送消息
const handleSend = async (text: string = inputMessage.value) => {
  const contentToSend = text.trim();
  if (!contentToSend || isTyping.value) return;

  // 1. 添加用户消息
  const userMsg: ChatMessage = {
    id: Date.now(),
    role: 'user',
    content: contentToSend,
    timestamp: Date.now()
  };
  messages.value.push(userMsg);
  
  // 重置输入框
  inputMessage.value = '';
  if (textareaRef.value) textareaRef.value.style.height = 'auto'; // 重置高度
  
  await scrollToBottom();

  // 2. 模拟 AI 思考和打字
  isTyping.value = true;
  
  // 模拟网络延迟
  setTimeout(() => {
    // 再次检查是否组件已卸载
    if (!chatContainer.value) return;

    const aiMsgId = Date.now() + 1;
    const fullReply = "As a Geo-Spatial Intelligence Agent, I can help you analyze terrain data, track satellite movements, or visualize complex coordinates. \n\nCurrently, I am running in **Mock Mode**, but my interface is ready to connect to your powerful Python backend.";
    
    const aiMsg: ChatMessage = {
      id: aiMsgId,
      role: 'ai',
      content: '', 
      timestamp: Date.now()
    };
    messages.value.push(aiMsg);

    // 打字机效果
    let i = 0;
    typingInterval = setInterval(() => {
      aiMsg.content += fullReply.charAt(i);
      i++;
      // 打字过程中使用 instant 滚动，体验更流畅
      scrollToBottom(false); 
      
      if (i >= fullReply.length) {
        if (typingInterval) clearInterval(typingInterval);
        isTyping.value = false;
        typingInterval = null;
      }
    }, 30); 
  }, 800);
};

// 格式化时间
const formatTime = (ts: number) => {
  return new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

const goToProfile = () => {
  router.push('/profile');
};
</script>

<template>
  <div class="flex h-screen w-full bg-[#0f172a] text-white overflow-hidden font-sans selection:bg-blue-500/30">
    
    <aside :class="['flex-shrink-0 bg-black/20 backdrop-blur-xl border-r border-white/5 transition-all duration-300 flex flex-col', showSidebar ? 'w-64' : 'w-0 overflow-hidden']">
      <div class="h-16 flex items-center px-6 border-b border-white/5">
        <span class="text-xl font-bold tracking-widest font-mono bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-purple-400 whitespace-nowrap">
          GEO-GPT
        </span>
      </div>

      <div class="flex-1 overflow-y-auto p-4 space-y-2 custom-scrollbar">
        <div class="text-xs text-gray-500 font-medium px-2 mb-2 uppercase tracking-wider">Recent</div>
        <button v-for="i in 5" :key="i" class="w-full text-left px-3 py-2.5 rounded-lg text-sm text-gray-400 hover:bg-white/5 hover:text-white transition-colors truncate group flex items-center gap-2">
          <span class="opacity-50 group-hover:opacity-100">💬</span>
          Spatial Analysis #{{ i }}
        </button>
      </div>

      <div class="p-4 border-t border-white/5">
        <button class="flex items-center gap-3 w-full px-3 py-2 rounded-lg hover:bg-white/5 text-sm text-gray-400 transition-colors">
          <span class="w-5 h-5 flex items-center justify-center border border-gray-600 rounded-full text-[10px]">?</span>
          Help & Support
        </button>
      </div>
    </aside>

    <main class="flex-1 flex flex-col relative min-w-0">
      
      <header class="absolute top-0 left-0 right-0 z-20 h-16 flex items-center justify-between px-6 bg-gradient-to-b from-[#0f172a] to-transparent pointer-events-none">
        <div class="pointer-events-auto flex items-center gap-4">
          <button @click="showSidebar = !showSidebar" class="p-2 rounded-full hover:bg-white/10 text-gray-400 transition-colors lg:hidden">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
            </svg>
          </button>
          
          <div @click="goToProfile" class="group relative flex items-center gap-3 pl-1 pr-3 py-1 rounded-full hover:bg-white/5 transition-all cursor-pointer">
            <div class="relative shrink-0">
              <img :src="userInfo.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'" alt="User" class="w-9 h-9 rounded-full border border-white/10 group-hover:border-blue-400 transition-colors object-cover bg-gray-800" />
              <div class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 border-2 border-[#0f172a] rounded-full"></div>
            </div>
            <span class="text-sm font-medium text-gray-300 group-hover:text-white transition-colors truncate max-w-[120px]">{{ userInfo.nickname }}</span>
          </div>
        </div>

        <div class="pointer-events-auto">
          <button class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-xs text-gray-300 transition-colors backdrop-blur-md">
            <span class="w-2 h-2 rounded-full bg-blue-500 shadow-[0_0_10px_rgba(59,130,246,0.5)] animate-pulse"></span>
            Geo-Model v1.0
            <span class="text-gray-500">▼</span>
          </button>
        </div>
      </header>

      <div ref="chatContainer" class="flex-1 overflow-y-auto pt-20 pb-40 px-4 custom-scrollbar">
        <div class="max-w-3xl mx-auto w-full">
          
          <transition name="fade" appear>
            <div v-if="messages.length === 0" class="flex flex-col items-center justify-center min-h-[60vh] text-center space-y-10">
              <div class="space-y-2">
                <h1 class="text-5xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-purple-400 to-indigo-400 animate-gradient pb-2">
                  Hello, {{ userInfo.nickname }}
                </h1>
                <p class="text-xl text-gray-400/80">How can I assist with your spatial data today?</p>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4 w-full max-w-2xl px-4">
                <button v-for="(item, idx) in suggestions" :key="idx" 
                  @click="handleSend(item.text)"
                  class="text-left p-4 rounded-xl bg-white/5 hover:bg-white/10 border border-white/5 hover:border-white/20 transition-all duration-300 group relative overflow-hidden">
                  <div class="absolute inset-0 bg-gradient-to-r from-blue-500/10 to-purple-500/10 opacity-0 group-hover:opacity-100 transition-opacity"></div>
                  <div class="relative z-10 flex items-start gap-3">
                    <span class="p-2 rounded-lg bg-black/20 text-lg">{{ item.icon }}</span>
                    <div>
                      <div class="text-sm font-medium text-gray-200">{{ item.text }}</div>
                      <div class="text-xs text-gray-500 mt-0.5">{{ item.sub }}</div>
                    </div>
                  </div>
                </button>
              </div>
            </div>
          </transition>

          <div v-if="messages.length > 0" class="space-y-6 pb-4">
            <div v-for="msg in messages" :key="msg.id" class="flex gap-4" :class="msg.role === 'user' ? 'flex-row-reverse' : ''">
              
              <div class="flex-shrink-0 mt-1">
                <img v-if="msg.role === 'user'" :src="userInfo.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'" class="w-8 h-8 rounded-full border border-white/10 bg-gray-800" />
                <div v-else class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-600 to-purple-600 flex items-center justify-center shadow-lg shadow-blue-900/50">
                  <span class="text-xs">✨</span>
                </div>
              </div>

              <div class="flex flex-col max-w-[85%] md:max-w-[75%]" :class="msg.role === 'user' ? 'items-end' : 'items-start'">
                <div class="px-5 py-3.5 rounded-2xl text-sm leading-relaxed shadow-sm break-words"
                  :class="msg.role === 'user' 
                    ? 'bg-[#1e293b] text-white rounded-tr-sm border border-white/5' 
                    : 'bg-transparent text-gray-100 rounded-tl-sm'">
                  <p class="whitespace-pre-wrap">{{ msg.content }}</p>
                  <span v-if="msg.role === 'ai' && isTyping && msg.id === messages[messages.length - 1]?.id" class="inline-block w-1.5 h-4 ml-1 align-middle bg-blue-400 animate-pulse"></span>
                </div>
                <span class="text-[10px] text-gray-600 mt-1 px-1 select-none">{{ formatTime(msg.timestamp) }}</span>
              </div>

            </div>
          </div>

        </div>
      </div>

      <div class="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-[#0f172a] via-[#0f172a] to-transparent z-10">
        <div class="max-w-3xl mx-auto">
          <div class="relative group">
            <div class="absolute -inset-0.5 bg-gradient-to-r from-blue-500 to-purple-500 rounded-2xl opacity-20 group-focus-within:opacity-50 transition duration-500 blur"></div>
            
            <div class="relative flex items-end gap-2 bg-[#0b1121] rounded-2xl p-2 border border-white/10 shadow-2xl">
              
              <button class="p-3 text-gray-400 hover:text-white hover:bg-white/5 rounded-xl transition-colors flex-shrink-0">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
              </button>

              <textarea 
                ref="textareaRef"
                v-model="inputMessage"
                @input="adjustTextareaHeight"
                @keydown.enter.prevent="handleSend()"
                placeholder="Message Geo-GPT..."
                rows="1"
                class="w-full bg-transparent text-white placeholder-gray-500 text-sm px-2 py-3.5 focus:outline-none resize-none custom-scrollbar"
                style="min-height: 48px; max-height: 128px;"
              ></textarea>

              <button 
                @click="handleSend()"
                :disabled="!inputMessage.trim() || isTyping"
                class="p-3 rounded-xl transition-all duration-300 flex-shrink-0"
                :class="inputMessage.trim() && !isTyping ? 'bg-blue-600 text-white shadow-lg shadow-blue-900/30 hover:bg-blue-500' : 'bg-white/5 text-gray-600 cursor-not-allowed'"
              >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-5 h-5 transform transition-transform" :class="inputMessage.trim() ? '-rotate-45' : ''">
                  <path d="M3.478 2.404a.75.75 0 0 0-.926.941l2.432 7.905H13.5a.75.75 0 0 1 0 1.5H4.984l-2.432 7.905a.75.75 0 0 0 .926.94 60.519 60.519 0 0 0 18.445-8.986.75.75 0 0 0 0-1.218A60.517 60.517 0 0 0 3.478 2.404Z" />
                </svg>
              </button>
            </div>
            
            <div class="text-center mt-3">
              <p class="text-[10px] text-gray-500 select-none">
                Geo-GPT can make mistakes. Consider checking important spatial information.
              </p>
            </div>
          </div>
        </div>
      </div>

    </main>
  </div>
</template>

<style scoped>
/* 自定义滚动条 */
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
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

/* 渐变文字动画 */
@keyframes gradient {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
.animate-gradient {
  background-size: 200% auto;
  animation: gradient 4s linear infinite;
}

/* 简单的淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>