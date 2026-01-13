<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { userApi, type UserInfo, type ProfileUpdateParams } from '../api/user';

const router = useRouter();
const loading = ref(false);
const activeTab = ref<'general' | 'account'>('general');

// 响应式状态
const profile = ref<Partial<UserInfo>>({
  username: '',
  email: '',
  nickname: '',
  gender: 0,
  region: '',
  avatar: '',
  settings: { darkMode: false }
});

const quotaInfo = reactive({ val: 0, desc: '' });

// 交互反馈
const toast = reactive({ show: false, msg: '' });
const showToast = (msg: string) => {
  toast.msg = msg; toast.show = true;
  setTimeout(() => toast.show = false, 2500);
};

// 初始化数据
const initData = async () => {
  try {
    const [pRes, qRes] = await Promise.all([
      userApi.getProfile(),
      userApi.getQuota()
    ]);

    if (pRes.code === 200 && pRes.data) {
      profile.value = pRes.data;
      if (!profile.value.settings) {
        profile.value.settings = { darkMode: false };
      }
    }

    if (qRes.code === 200 && qRes.data) {
      quotaInfo.val = qRes.data.quota;
      quotaInfo.desc = qRes.data.quotaDesc;
    }
  } catch (error) {
    console.error('Failed to load profile:', error);
    showToast('数据加载失败');
  }
};

onMounted(initData);

// 上传头像逻辑
const fileInput = ref<HTMLInputElement>();
const triggerUpload = () => fileInput.value?.click();

const handleFileChange = async (e: Event) => {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = (e) => {
    if (profile.value && e.target?.result) {
      profile.value.avatar = e.target.result as string;
    }
  };
  reader.readAsDataURL(file);

  try {
    const res = await userApi.uploadAvatar(file);
    if (res.code === 200) {
      profile.value.avatar = res.data.avatarUrl;
      showToast('头像已更新');
    }
  } catch (err) {
    showToast('头像上传失败');
  }
};

// 保存修改逻辑
const handleSave = async () => {
  loading.value = true;
  try {
    const updateParams: ProfileUpdateParams = {
      nickname: profile.value.nickname,
      gender: profile.value.gender,
      region: profile.value.region,
      settings: profile.value.settings
    };

    const res = await userApi.updateProfile(updateParams);
    if (res.code === 200) {
      showToast('设置已保存');
    }
  } catch (err) {
    showToast('保存失败');
  } finally {
    loading.value = false;
  }
};

const handleLogout = () => {
  localStorage.removeItem('token');
  router.push('/login');
};

const handleCancelAccount = async () => {
  const pwd = window.prompt('请输入登录密码以确认注销（此操作不可恢复）：');
  if (!pwd) return;

  try {
    const res = await userApi.cancelAccount(pwd);
    if (res.code === 200) {
      alert('账号已注销');
      handleLogout();
    }
  } catch (err: any) {
    showToast(err.msg || '注销失败');
  }
};
</script>

<template>
  <div class="min-h-screen bg-[#1e1e1e] flex items-center justify-center p-6 font-sans text-white/90">
    
    <transition name="pop">
      <div v-if="toast.show" class="fixed top-10 right-10 bg-gray-800/90 backdrop-blur-md border border-white/10 px-4 py-3 rounded-xl shadow-2xl flex items-center gap-3 z-50">
        <div class="w-2 h-2 rounded-full bg-green-500"></div>
        <span class="text-sm font-medium">{{ toast.msg }}</span>
      </div>
    </transition>

    <div class="w-full max-w-5xl h-[700px] bg-[#2d2d2d]/80 backdrop-blur-2xl rounded-2xl shadow-2xl border border-white/[0.08] flex overflow-hidden ring-1 ring-black/20">
      
      <aside class="w-72 bg-[#262626]/90 border-r border-white/[0.05] flex flex-col pt-10 pb-6 px-4">
        <div class="flex flex-col items-center mb-10 group">
          <div class="relative cursor-pointer" @click="triggerUpload">
            <img 
              :src="profile.avatar || `https://ui-avatars.com/api/?name=${profile.nickname || 'User'}&background=007AFF&color=fff`" 
              class="w-24 h-24 rounded-full object-cover shadow-lg border-4 border-[#3a3a3a] group-hover:brightness-75 transition-all"
            />
            <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
              <span class="text-xs font-medium drop-shadow-md">编辑</span>
            </div>
          </div>
          <input type="file" ref="fileInput" class="hidden" accept=".jpg,.png" @change="handleFileChange" />
          
          <h2 class="mt-4 text-lg font-semibold tracking-wide">{{ profile.nickname || '未设置昵称' }}</h2>
          <p class="text-xs text-gray-500 font-mono mt-1">@{{ profile.username }}</p>
          
          <div class="mt-4 px-3 py-1 bg-white/5 rounded-full border border-white/5 flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.6)]"></span>
            <span class="text-xs text-gray-400">积分余额</span>
            <span class="text-xs font-bold font-mono">{{ quotaInfo.val }}</span>
          </div>
        </div>

        <nav class="flex-1 space-y-1">
          <button 
            @click="router.push('/home')"
            class="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium text-gray-400 hover:bg-white/5 hover:text-white transition-all duration-200"
          >
            <span class="text-lg"></span>
            返回主页
          </button>
          
          <div class="h-px bg-white/5 my-2 mx-3"></div>

          <button 
            v-for="tab in ['general', 'account']" 
            :key="tab"
            @click="activeTab = tab as any"
            :class="[
              'w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200',
              activeTab === tab ? 'bg-[#007AFF] text-white shadow-sm' : 'text-gray-400 hover:bg-white/5 hover:text-white'
            ]"
          >
            <span v-if="tab === 'general'" class="text-lg"></span>
            <span v-if="tab === 'account'" class="text-lg"></span>
            {{ tab === 'general' ? '通用设置' : '账户与安全' }}
          </button>
        </nav>

        <button @click="handleLogout" class="flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium text-red-400 hover:bg-red-500/10 transition-colors">
          <span class="text-lg"></span>
          退出登录
        </button>
      </aside>

      <main class="flex-1 overflow-y-auto bg-transparent relative custom-scrollbar">
        <header class="sticky top-0 z-10 px-8 py-6 bg-[#2d2d2d]/90 backdrop-blur-xl border-b border-white/[0.02] flex justify-between items-center">
          <h1 class="text-xl font-bold text-white tracking-tight">
            {{ activeTab === 'general' ? '通用设置' : '账户与安全' }}
          </h1>
          <button @click="handleSave" :disabled="loading" class="text-sm text-[#007AFF] font-medium hover:text-[#409cff] active:opacity-70 disabled:opacity-50 disabled:cursor-not-allowed transition-colors">
            {{ loading ? '保存中...' : '保存更改' }}
          </button>
        </header>

        <div class="p-8 max-w-2xl mx-auto space-y-8 animate-fade-in">
          
          <div v-if="activeTab === 'general'" class="space-y-6">
            
            <div class="bg-[#3a3a3a]/40 backdrop-blur-md rounded-xl border border-white/[0.05] overflow-hidden">
              <div class="flex items-center px-4 py-4 border-b border-black/20 text-sm">
                <label class="text-white/90 font-medium min-w-[5rem]">用户昵称</label>
                <input v-model="profile.nickname" type="text" placeholder="设置一个好听的名字" 
                       class="flex-1 bg-transparent border-none outline-none text-right text-white/70 focus:text-white placeholder-gray-600 transition-colors" />
              </div>
              
              <div class="flex items-center px-4 py-4 border-b border-black/20 text-sm">
                <label class="text-white/90 font-medium min-w-[5rem]">所在地区</label>
                <input v-model="profile.region" type="text" placeholder="例如：上海, 中国" 
                       class="flex-1 bg-transparent border-none outline-none text-right text-white/70 focus:text-white placeholder-gray-600 transition-colors" />
              </div>
              
              <div class="flex items-center px-4 py-4 text-sm">
                <label class="text-white/90 font-medium min-w-[5rem]">性别</label>
                <div class="flex gap-4 ml-auto">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input type="radio" :value="1" v-model="profile.gender" class="hidden peer">
                    <span class="w-4 h-4 rounded-full border border-gray-500 peer-checked:border-[#007AFF] peer-checked:bg-[#007AFF] transition-all"></span>
                    <span class="text-sm text-gray-400 peer-checked:text-white">男</span>
                  </label>
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input type="radio" :value="2" v-model="profile.gender" class="hidden peer">
                    <span class="w-4 h-4 rounded-full border border-gray-500 peer-checked:border-[#007AFF] peer-checked:bg-[#007AFF] transition-all"></span>
                    <span class="text-sm text-gray-400 peer-checked:text-white">女</span>
                  </label>
                </div>
              </div>
            </div>

            <p class="text-xs text-gray-500 ml-4">您的个人信息将用于在社区中展示。</p>

            <h3 class="text-xs font-bold text-gray-500 uppercase tracking-wider ml-4 mt-8 mb-2">偏好设置</h3>
            <div class="bg-[#3a3a3a]/40 backdrop-blur-md rounded-xl border border-white/[0.05] overflow-hidden">
              <div class="flex items-center justify-between px-4 py-4 text-sm cursor-pointer" 
                   @click="profile.settings && (profile.settings.darkMode = !profile.settings.darkMode)">
                <label class="text-white/90 font-medium cursor-pointer">深色模式</label>
                <div class="relative inline-block w-12 h-7 rounded-full bg-[#4a4a4a] transition-colors duration-200" 
                     :class="{'bg-[#30d158]': profile.settings?.darkMode}">
                   <span class="absolute top-[2px] left-[2px] bg-white w-6 h-6 rounded-full shadow-md transform transition-transform duration-200"
                         :class="{'translate-x-5': profile.settings?.darkMode}"></span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="activeTab === 'account'" class="space-y-6">
            <div class="bg-[#3a3a3a]/40 backdrop-blur-md rounded-xl border border-white/[0.05] overflow-hidden">
              <div class="flex items-center px-4 py-4 border-b border-black/20 text-sm">
                <label class="text-white/90 font-medium min-w-[6rem]">用户名</label>
                <span class="flex-1 text-right text-gray-400 font-mono select-all">{{ profile.username }}</span>
              </div>
              <div class="flex items-center px-4 py-4 text-sm">
                <label class="text-white/90 font-medium min-w-[6rem]">绑定邮箱</label>
                <span class="flex-1 text-right text-gray-400 font-mono">{{ profile.email }}</span>
              </div>
            </div>

            <div class="bg-[#3a3a3a]/40 backdrop-blur-md rounded-xl border border-white/[0.05] overflow-hidden mt-8">
              <div class="flex items-center justify-between px-4 py-4 text-sm cursor-pointer group hover:bg-white/[0.02] transition-colors"
                   @click="handleCancelAccount">
                <label class="text-red-400 font-medium group-hover:text-red-300 cursor-pointer">注销账号</label>
                <span class="text-gray-600 text-lg">›</span>
              </div>
            </div>
            <p class="text-xs text-gray-500 ml-4">注销操作不可逆，请谨慎操作。</p>
          </div>

        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 样式保持不变 */
.animate-fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

.pop-enter-active, .pop-leave-active {
  transition: all 0.3s cubic-bezier(0.18, 0.89, 0.32, 1.28);
}
.pop-enter-from, .pop-leave-to {
  opacity: 0;
  transform: translateY(-20px) scale(0.9);
}

.custom-scrollbar::-webkit-scrollbar {
  width: 0px;
  background: transparent;
}
</style>