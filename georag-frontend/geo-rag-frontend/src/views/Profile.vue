<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { userApi } from '../api/user';

const router = useRouter();
const loading = ref(false);
const activeTab = ref('basic'); // basic | settings | security

// 响应式数据：个人信息
const profile = ref<any>({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: '',
  gender: 0,
  genderDesc: '',
  region: '',
  statusDesc: '',
  createTime: '',
  settings: {
    darkMode: true,
    defaultMapStyle: 'normal'
  }
});

// 响应式数据：积分
const quotaInfo = ref({
  quota: 0,
  quotaDesc: ''
});

// 消息提示逻辑
const toast = reactive({ show: false, msg: '', type: 'success' });
const showToast = (msg: string, type: 'success' | 'error' = 'success') => {
  toast.msg = msg; toast.type = type; toast.show = true;
  setTimeout(() => toast.show = false, 3000);
};

// --- 初始化数据 ---
const initData = async () => {
  try {
    const [pRes, qRes] = await Promise.all([
      userApi.getProfile(),
      userApi.getQuota()
    ]);
    if (pRes.code === 200) profile.value = pRes.data;
    if (qRes.code === 200) quotaInfo.value = qRes.data;
  } catch (err: any) {
    showToast(err.msg || '获取信息失败', 'error');
  }
};

onMounted(initData);

// --- 交互逻辑 ---

// 1. 上传头像
const avatarInput = ref<HTMLInputElement | null>(null);
const handleAvatarClick = () => avatarInput.value?.click();
const onFileChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) return showToast('图片不能超过5MB', 'error');

  loading.value = true;
  try {
    const res = await userApi.uploadAvatar(file);
    if (res.code === 200) {
      profile.value.avatar = res.data.avatarUrl;
      showToast('头像更新成功');
    }
  } catch (err: any) {
    showToast(err.msg || '上传失败', 'error');
  } finally {
    loading.value = false;
  }
};

// 2. 保存修改
const saveProfile = async () => {
  loading.value = true;
  try {
    const res = await userApi.updateProfile({
      nickname: profile.value.nickname,
      gender: profile.value.gender,
      region: profile.value.region,
      settings: profile.value.settings
    });
    if (res.code === 200) showToast('个人信息已更新');
  } catch (err: any) {
    showToast(err.msg || '保存失败', 'error');
  } finally {
    loading.value = false;
  }
};

// 3. 退出登录
const logout = () => {
  localStorage.clear();
  router.push('/login');
};

// 4. 注销账号
const handleCancelAccount = async () => {
  const pwd = prompt('请输入密码以确认注销（操作不可恢复）：');
  if (!pwd) return;
  try {
    const res = await userApi.cancelAccount(pwd);
    if (res.code === 200) {
      showToast('账号已注销', 'success');
      logout();
    }
  } catch (err: any) {
    showToast(err.msg || '注销失败', 'error');
  }
};
</script>

<template>
  <div class="min-h-screen bg-[#050b14] text-gray-200 font-sans selection:bg-blue-500/30">
    
    <transition name="fade">
      <div v-if="toast.show" class="fixed top-8 left-1/2 -translate-x-1/2 z-[100] px-6 py-3 rounded-full border backdrop-blur-md"
        :class="toast.type === 'success' ? 'bg-green-500/20 border-green-500/50 text-green-200' : 'bg-red-500/20 border-red-500/50 text-red-200'">
        {{ toast.msg }}
      </div>
    </transition>

    <div class="max-w-6xl mx-auto px-6 py-12 flex flex-col md:flex-row gap-8">
      
      <aside class="w-full md:w-80 flex flex-col gap-6">
        <div class="bg-white/[0.03] border border-white/10 rounded-3xl p-8 backdrop-blur-xl flex flex-col items-center text-center">
          <div class="relative group cursor-pointer mb-4" @click="handleAvatarClick">
            <img :src="profile.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=default'" 
              class="w-32 h-32 rounded-full border-4 border-white/5 object-cover group-hover:opacity-70 transition-all" />
            <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
              <span class="bg-black/60 px-3 py-1 rounded-full text-xs">更换头像</span>
            </div>
            <input type="file" ref="avatarInput" class="hidden" accept="image/*" @change="onFileChange" />
          </div>
          <h2 class="text-xl font-bold text-white">{{ profile.nickname }}</h2>
          <p class="text-xs text-gray-500 mt-1">@{{ profile.username }}</p>
          
          <div class="mt-6 w-full py-3 px-4 bg-blue-500/10 border border-blue-500/20 rounded-2xl flex justify-between items-center">
            <span class="text-xs text-blue-300 font-medium">剩余积分</span>
            <span class="text-lg font-mono font-bold text-blue-400">{{ quotaInfo.quota }}</span>
          </div>
        </div>

        <nav class="bg-white/[0.03] border border-white/10 rounded-3xl p-2 backdrop-blur-xl">
          <button @click="activeTab = 'basic'" :class="['w-full flex items-center gap-3 px-6 py-4 rounded-2xl transition-all', activeTab === 'basic' ? 'bg-blue-600 text-white shadow-lg' : 'hover:bg-white/5 text-gray-400']">
            <span>👤</span> 基本信息
          </button>
          <button @click="activeTab = 'settings'" :class="['w-full flex items-center gap-3 px-6 py-4 rounded-2xl transition-all', activeTab === 'settings' ? 'bg-blue-600 text-white shadow-lg' : 'hover:bg-white/5 text-gray-400']">
            <span>⚙️</span> 系统偏好
          </button>
          <button @click="activeTab = 'security'" :class="['w-full flex items-center gap-3 px-6 py-4 rounded-2xl transition-all', activeTab === 'security' ? 'bg-blue-600 text-white shadow-lg' : 'hover:bg-white/5 text-gray-400']">
            <span>🛡️</span> 账号安全
          </button>
        </nav>

        <button @click="logout" class="w-full py-4 px-6 rounded-3xl border border-red-500/30 text-red-400 hover:bg-red-500/10 transition-all text-sm font-medium">
          退出当前账号
        </button>
      </aside>

      <main class="flex-1 bg-white/[0.03] border border-white/10 rounded-[2.5rem] backdrop-blur-xl overflow-hidden flex flex-col">
        
        <header class="px-10 py-8 border-b border-white/5 flex justify-between items-center">
          <h1 class="text-2xl font-bold text-white capitalize">{{ activeTab === 'basic' ? 'Basic Information' : activeTab }}</h1>
          <button @click="router.push('/')" class="text-sm text-gray-500 hover:text-white transition-colors">返回对话 ➔</button>
        </header>

        <div class="flex-1 p-10 overflow-y-auto">
          <div v-if="activeTab === 'basic'" class="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div class="space-y-2">
                <label class="text-xs font-bold text-gray-500 uppercase tracking-widest pl-1">昵称 (Nickname)</label>
                <input v-model="profile.nickname" class="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 focus:border-blue-500 outline-none transition-all" />
              </div>
              <div class="space-y-2">
                <label class="text-xs font-bold text-gray-500 uppercase tracking-widest pl-1">地区 (Region)</label>
                <input v-model="profile.region" class="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 focus:border-blue-500 outline-none transition-all" />
              </div>
              <div class="space-y-2">
                <label class="text-xs font-bold text-gray-500 uppercase tracking-widest pl-1">性别 (Gender)</label>
                <select v-model="profile.gender" class="w-full bg-white/5 border border-white/10 rounded-2xl px-5 py-4 focus:border-blue-500 outline-none transition-all appearance-none">
                  <option :value="0">保密</option>
                  <option :value="1">男</option>
                  <option :value="2">女</option>
                </select>
              </div>
              <div class="space-y-2">
                <label class="text-xs font-bold text-gray-400/50 uppercase tracking-widest pl-1">注册时间 (Create Time)</label>
                <div class="w-full bg-black/20 border border-white/5 rounded-2xl px-5 py-4 text-gray-500 cursor-not-allowed">
                  {{ profile.createTime }}
                </div>
              </div>
            </div>

            <div class="pt-8 border-t border-white/5 flex flex-col gap-4">
               <div class="flex items-center gap-4 text-sm">
                  <span class="text-gray-500">登录账号 (不可修改):</span>
                  <span class="text-white font-mono">{{ profile.username }}</span>
               </div>
               <div class="flex items-center gap-4 text-sm">
                  <span class="text-gray-500">绑定邮箱:</span>
                  <span class="text-white font-mono">{{ profile.email }}</span>
               </div>
            </div>
          </div>

          <div v-if="activeTab === 'settings'" class="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <div class="flex justify-between items-center p-6 bg-white/5 rounded-3xl border border-white/5">
              <div>
                <h3 class="font-bold text-white">深色模式 (Dark Mode)</h3>
                <p class="text-xs text-gray-500 mt-1">开启后界面将以深蓝和黑色调为主</p>
              </div>
              <input type="checkbox" v-model="profile.settings.darkMode" class="w-12 h-6 rounded-full appearance-none bg-gray-700 checked:bg-blue-600 transition-all cursor-pointer relative after:content-[''] after:absolute after:top-1 after:left-1 after:bg-white after:w-4 after:h-4 after:rounded-full after:transition-all checked:after:translate-x-6" />
            </div>

            <div class="space-y-2">
              <label class="text-xs font-bold text-gray-500 uppercase tracking-widest pl-1">默认地图底图风格</label>
              <div class="grid grid-cols-2 gap-4">
                <button v-for="style in ['normal', 'satellite', 'dark', 'light']" :key="style"
                  @click="profile.settings.defaultMapStyle = style"
                  :class="['py-4 rounded-2xl border transition-all capitalize', profile.settings.defaultMapStyle === style ? 'border-blue-500 bg-blue-500/10 text-white' : 'border-white/10 bg-white/5 text-gray-400']">
                  {{ style }}
                </button>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'security'" class="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
             <div class="p-8 border border-red-500/20 bg-red-500/5 rounded-[2rem] space-y-4">
                <h3 class="text-lg font-bold text-red-400">危险区域 (Danger Zone)</h3>
                <p class="text-sm text-gray-500">注销账号将永久删除您的所有数据，包括 API 积分和历史对话记录，此操作无法撤销。</p>
                <button @click="handleCancelAccount" class="px-6 py-3 bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 text-red-500 rounded-xl transition-all font-medium">
                  注销我的账号
                </button>
             </div>
          </div>
        </div>

        <footer class="p-8 border-t border-white/5 bg-white/[0.02]">
           <button @click="saveProfile" :disabled="loading"
            class="w-full md:w-auto px-10 py-4 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white rounded-2xl font-bold shadow-xl shadow-blue-900/40 transition-all active:scale-95 disabled:opacity-50">
            {{ loading ? '正在保存...' : '保存更改 (Save Changes)' }}
          </button>
        </footer>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 简单的进入动画 */
.animate-in {
  animation-duration: 0.5s;
}

/* 自定义下拉箭头 */
select {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='rgba(255,255,255,0.3)'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1.25rem center;
  background-size: 1.25rem;
}
</style>
