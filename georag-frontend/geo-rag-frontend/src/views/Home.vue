<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { userApi } from '../api/user';
import { useChat } from '../hooks/useChat';
import MacModal from '../components/MacModal.vue';

const router = useRouter();

const {
  inputMessage,
  isTyping,
  chatContainer,
  sessionList,
  currentSessionId,
  messages,
  loadingHistory,
  loadSessions,
  switchSession,
  createNewChat,
  deleteSession,
  updateSessionTitle,
  handleSend
} = useChat();

// --- 用户信息 ---
interface LocalUserInfo { nickname: string; avatar: string; }
const userInfo = ref<LocalUserInfo>({ nickname: 'Traveler', avatar: '' });

const fetchUserProfile = async () => {
  try {
    const res = await userApi.getProfile();
    if (res.code === 200 && res.data) {
      userInfo.value = {
        nickname: res.data.nickname || 'Traveler',
        avatar: res.data.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=Felix'
      };
    }
  } catch (error) { console.error(error); }
};

// --- 弹窗状态管理 ---
const modalState = reactive({
  show: false,
  type: 'confirm' as 'confirm' | 'input' | 'danger',
  title: '',
  content: '',
  inputValue: '',
  targetId: ''
});

// 打开删除弹窗
const openDeleteModal = (id: string, title: string) => {
  modalState.type = 'danger';
  modalState.title = 'Delete Chat?';
  modalState.content = `Are you sure you want to delete "${title || 'this chat'}"? This action cannot be undone.`;
  modalState.targetId = id;
  modalState.show = true;
};

// 打开重命名弹窗
const openRenameModal = (id: string, currentTitle: string) => {
  modalState.type = 'input';
  modalState.title = 'Rename Chat';
  modalState.content = '';
  modalState.inputValue = currentTitle; // 传入初始值
  modalState.targetId = id;
  modalState.show = true;
};

// 处理弹窗确认
const handleModalConfirm = (val?: string) => {
  if (modalState.type === 'danger') {
    deleteSession(modalState.targetId);
  } else if (modalState.type === 'input' && val) {
    updateSessionTitle(modalState.targetId, val);
  }
  modalState.show = false;
};

const showSidebar = ref(true);
const suggestions = [
  { icon: '🗺️', text: '雨姐是东北哪里人？', sub: 'Analyze terrain data' },
  { icon: '🛰️', text: '查询最新的giser工资', sub: 'Satellite schedule' },
  { icon: '📊', text: '生成我的个人分析画像', sub: 'Generate heatmap' },
  { icon: '📝', text: '帮我写一份低空经济报告', sub: 'Draft a report' },
];

const formatTime = (timeStr: string) => {
  if (!timeStr) return '';
  return new Date(timeStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

onMounted(() => {
  loadSessions();
  fetchUserProfile();
});
</script>

<template>
  <div class="flex h-screen w-full bg-[#050b14] text-white overflow-hidden font-sans selection:bg-blue-500/30">
    
    <MacModal 
      :show="modalState.show"
      :type="modalState.type"
      :title="modalState.title"
      :content="modalState.content"
      :initial-value="modalState.inputValue"
      placeholder="Enter new chat name"
      @close="modalState.show = false"
      @confirm="handleModalConfirm"
    />

    <aside :class="['flex-shrink-0 bg-[#0b1118]/80 backdrop-blur-xl border-r border-white/5 transition-all duration-300 flex flex-col', showSidebar ? 'w-72' : 'w-0 overflow-hidden']">
      
      <div class="p-5 border-b border-white/5 flex flex-col gap-4">
        <div class="flex items-center gap-2 px-1">
          <span class="text-lg font-black tracking-[0.15em] font-mono bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-indigo-400">
            GEO-RAG
          </span>
        </div>
        
        <button @click="createNewChat" 
          class="group w-full flex items-center gap-3 px-4 py-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/5 hover:border-white/10 transition-all duration-200 active:scale-[0.98]">
          <div class="p-1.5 rounded-lg bg-blue-500/20 text-blue-400 group-hover:bg-blue-500 group-hover:text-white transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" class="w-4 h-4">
              <path d="M10.75 4.75a.75.75 0 00-1.5 0v4.5h-4.5a.75.75 0 000 1.5h4.5v4.5a.75.75 0 001.5 0v-4.5h4.5a.75.75 0 000-1.5h-4.5v-4.5z" />
            </svg>
          </div>
          <span class="text-sm font-medium text-gray-300 group-hover:text-white">New Chat</span>
        </button>
      </div>

      <div class="flex-1 overflow-y-auto p-3 space-y-1 custom-scrollbar">
        <div class="text-[10px] font-bold text-gray-500 px-3 mb-2 uppercase tracking-widest opacity-60">Recents</div>
        
        <div v-if="sessionList.length === 0" class="text-center text-gray-600 text-xs py-8 italic">
          No history yet.
        </div>

        <div v-for="session in sessionList" :key="session.id" 
          @click="switchSession(session.id)"
          class="group relative flex items-center gap-3 px-3 py-3 rounded-xl text-sm cursor-pointer transition-all border border-transparent"
          :class="currentSessionId === session.id ? 'bg-[#1e293b]/60 text-white border-white/5 shadow-sm' : 'text-gray-400 hover:bg-white/5 hover:text-gray-200'">
          
          <svg v-if="currentSessionId === session.id" class="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" /></svg>
          <svg v-else class="w-4 h-4 opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
          
          <span class="truncate flex-1 font-medium">{{ session.title || 'Untitled Session' }}</span>
          
          <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
            <button @click.stop="openRenameModal(session.id, session.title)" class="p-1.5 hover:bg-white/10 rounded-md text-gray-400 hover:text-blue-400 transition-colors">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" /></svg>
            </button>
            <button @click.stop="openDeleteModal(session.id, session.title)" class="p-1.5 hover:bg-white/10 rounded-md text-gray-400 hover:text-red-400 transition-colors">
              <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
            </button>
          </div>
        </div>
      </div>

      <div class="p-4 border-t border-white/5">
        <div @click="router.push('/profile')" class="flex items-center gap-3 p-2 rounded-xl hover:bg-white/5 cursor-pointer transition-colors group">
          <img :src="userInfo.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=default'" class="w-8 h-8 rounded-full border border-white/10 group-hover:border-blue-400 transition-colors" />
          <div class="flex flex-col overflow-hidden">
            <span class="text-sm font-medium text-gray-200 truncate group-hover:text-white">{{ userInfo.nickname }}</span>
            <span class="text-[10px] text-gray-500">View Profile</span>
          </div>
        </div>
      </div>
    </aside>

    <main class="flex-1 flex flex-col relative min-w-0 bg-gradient-to-b from-[#050b14] to-black">
      
      <header class="absolute top-0 left-0 right-0 z-20 h-16 flex items-center justify-between px-6 pointer-events-none lg:hidden">
        <div class="pointer-events-auto">
          <button @click="showSidebar = !showSidebar" class="p-2 rounded-lg bg-white/5 border border-white/5 text-gray-400 hover:text-white">☰</button>
        </div>
      </header>

      <div ref="chatContainer" class="flex-1 overflow-y-auto pt-20 pb-40 px-4 scroll-smooth custom-scrollbar">
        <div class="max-w-3xl mx-auto w-full">
          
          <div v-if="loadingHistory" class="flex justify-center py-20">
            <div class="animate-spin rounded-full h-8 w-8 border-2 border-blue-500 border-t-transparent"></div>
          </div>

          <div v-else-if="messages.length === 0" class="flex flex-col items-center justify-center min-h-[60vh] text-center space-y-12 animate-fade-in">
            <div class="space-y-4">
              <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-blue-500/10 mb-4 border border-blue-500/20 shadow-[0_0_40px_-10px_rgba(59,130,246,0.5)]">
                <span class="text-3xl">🌏</span>
              </div>
              <h1 class="text-4xl font-bold text-white tracking-tight">
                Hello, {{ userInfo.nickname }}
              </h1>
              <p class="text-lg text-gray-400">Ready to explore spatial intelligence?</p>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-3 w-full max-w-2xl">
              <button v-for="(item, idx) in suggestions" :key="idx" 
                @click="handleSend(item.text)"
                class="text-left p-4 rounded-xl bg-white/[0.03] hover:bg-white/[0.07] border border-white/5 hover:border-white/10 transition-all duration-300 group">
                <div class="flex items-start gap-3">
                  <span class="text-xl opacity-70 group-hover:scale-110 transition-transform duration-300">{{ item.icon }}</span>
                  <div>
                    <div class="text-sm font-medium text-gray-200 group-hover:text-blue-300 transition-colors">{{ item.text }}</div>
                    <div class="text-xs text-gray-500 mt-1">{{ item.sub }}</div>
                  </div>
                </div>
              </button>
            </div>
          </div>

          <div v-else class="space-y-8">
            <div v-for="msg in messages" :key="msg.id" class="flex gap-5" :class="msg.role === 'user' ? 'flex-row-reverse' : ''">
              
              <div class="flex-shrink-0 mt-1">
                <img v-if="msg.role === 'user'" :src="userInfo.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=default'" class="w-9 h-9 rounded-full border border-white/10 shadow-lg" />
                <div v-else class="w-9 h-9 rounded-full bg-gradient-to-br from-blue-600 to-indigo-600 flex items-center justify-center shadow-[0_0_15px_rgba(37,99,235,0.4)]">
                  <span class="text-sm font-bold">G</span>
                </div>
              </div>

              <div class="flex flex-col max-w-[80%]" :class="msg.role === 'user' ? 'items-end' : 'items-start'">
                <div class="px-5 py-3.5 rounded-[1.2rem] text-[15px] leading-7 shadow-sm break-words border"
                  :class="msg.role === 'user' 
                    ? 'bg-[#2563eb] text-white border-blue-500/50 rounded-tr-sm' 
                    : 'bg-[#1e1e20] text-gray-100 border-white/5 rounded-tl-sm'">
                  <p class="whitespace-pre-wrap">{{ (msg as any).content || msg.context }}</p>
                  <span v-if="msg.role === 'assistant' && isTyping && msg.id === messages[messages.length-1]?.id" 
                    class="inline-block w-2 h-4 ml-1 bg-blue-400 animate-pulse align-middle"></span>
                </div>
                <span class="text-[11px] text-gray-600 mt-1.5 px-1 font-mono opacity-60">
                  {{ formatTime(msg.createTime) }}
                </span>
              </div>
            </div>
          </div>

        </div>
      </div>

      <div class="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-black via-black/90 to-transparent">
        <div class="max-w-3xl mx-auto">
          <div class="relative flex items-end gap-2 bg-[#1c1c1e] rounded-2xl p-2 border border-white/10 shadow-2xl transition-colors focus-within:border-blue-500/30 focus-within:bg-[#252527]">
            
            <button class="p-3 text-gray-500 hover:text-white hover:bg-white/5 rounded-xl transition-colors">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-6 h-6"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" /></svg>
            </button>

            <textarea 
              v-model="inputMessage"
              @keydown.enter.prevent="handleSend()"
              placeholder="Message Geo-RAG..."
              rows="1"
              class="w-full bg-transparent text-white placeholder-gray-500 text-[15px] px-2 py-3.5 focus:outline-none resize-none max-h-32 custom-scrollbar"
              style="min-height: 48px;"
            ></textarea>

            <button 
              @click="handleSend()"
              :disabled="!inputMessage.trim() || isTyping"
              class="p-2.5 rounded-xl transition-all duration-300"
              :class="inputMessage.trim() ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/20 hover:scale-105' : 'bg-white/5 text-gray-600 cursor-not-allowed'"
            >
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-5 h-5"><path d="M3.478 2.404a.75.75 0 0 0-.926.941l2.432 7.905H13.5a.75.75 0 0 1 0 1.5H4.984l-2.432 7.905a.75.75 0 0 0 .926.94 60.519 60.519 0 0 0 18.445-8.986.75.75 0 0 0 0-1.218A60.517 60.517 0 0 0 3.478 2.404Z" /></svg>
            </button>
          </div>
          <div class="text-center mt-3">
            <p class="text-[10px] text-gray-600 font-medium tracking-wide">
              Geo-RAG can make mistakes. Verify important spatial data.
            </p>
          </div>
        </div>
      </div>

    </main>
  </div>
</template>

<style scoped>
/* 滚动条美化 */
.custom-scrollbar::-webkit-scrollbar { width: 5px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
.custom-scrollbar::-webkit-scrollbar-thumb { background-color: rgba(255, 255, 255, 0.1); border-radius: 20px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background-color: rgba(255, 255, 255, 0.2); }

.animate-fade-in { animation: fadeIn 0.6s cubic-bezier(0.16, 1, 0.3, 1); }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>