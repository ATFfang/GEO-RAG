<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import * as THREE from 'three';
import { userApi } from '../api/user';

const router = useRouter();

// --- 状态管理 ---
const mode = ref<'login' | 'register'>('login'); 
const loading = ref(false);
const countdown = ref(0); 

// 密码可见性状态
const visibility = reactive({
  login: false,
  register: false,
  confirm: false
});

const form = reactive({
  account: '',    
  password: '',   
  confirmPassword: '', // 新增：确认密码
  username: '',   
  nickname: '',   
  email: '',      
  verifyCode: ''  
});

// --- 自定义 Toast 弹窗 ---
const toast = reactive({
  show: false,
  message: '',
  type: 'error' as 'success' | 'error'
});

const showToast = (msg: string, type: 'success' | 'error' = 'error') => {
  toast.message = msg;
  toast.type = type;
  toast.show = true;
  setTimeout(() => {
    toast.show = false;
  }, 3000);
};

// --- 校验逻辑 ---
const isValidEmail = (email: string) => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
};

// --- 交互逻辑 ---
const handleSendCode = async () => {
  if (!form.email) return showToast('请输入邮箱地址');
  if (!isValidEmail(form.email)) return showToast('邮箱格式不正确');

  try {
    await userApi.sendRegisterCode(form.email);
    countdown.value = 60;
    const timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) clearInterval(timer);
    }, 1000);
    showToast('验证码已发送，请查收', 'success');
  } catch (error: any) {
    showToast(error.msg || '发送失败'); 
  }
};

const handleSubmit = async () => {
  // 基础非空校验
  if (mode.value === 'login' && (!form.account || !form.password)) {
    return showToast('请输入账号和密码');
  }
  
  if (mode.value === 'register') {
    if (!form.username || !form.nickname || !form.email || !form.verifyCode || !form.password) {
      return showToast('请填写完整注册信息');
    }
    if (!isValidEmail(form.email)) {
      return showToast('邮箱格式不正确');
    }
    // 双重密码校验
    if (form.password !== form.confirmPassword) {
      return showToast('两次输入的密码不一致');
    }
  }

  loading.value = true;
  try {
    if (mode.value === 'login') {
      const res = await userApi.login({ account: form.account, password: form.password });
      if(res.code === 200) {
        localStorage.setItem('token', res.data.token);
        localStorage.setItem('userInfo', JSON.stringify(res.data));
        showToast('登录成功，正在跳转...', 'success');
        setTimeout(() => router.push('/'), 1000);
      }
    } else {
      const res = await userApi.register({
        username: form.username,
        password: form.password,
        email: form.email,
        verifyCode: form.verifyCode,
        nickname: form.nickname
      });
      if(res.code === 200) {
        localStorage.setItem('token', res.data.token);
        showToast('注册成功', 'success');
        setTimeout(() => router.push('/'), 1000);
      }
    }
  } catch (error: any) {
    showToast(error.msg || '操作失败');
  } finally {
    loading.value = false;
  }
};

// --- Three.js 3D 场景 (保持不变) ---
const canvasRef = ref<HTMLElement | null>(null);
let scene: THREE.Scene, camera: THREE.PerspectiveCamera, renderer: THREE.WebGLRenderer;
let earthMesh: THREE.Mesh, starsMesh: THREE.Points, atmosphereMesh: THREE.Mesh;
let animationId: number;

const initThree = () => {
  if (!canvasRef.value) return;

  scene = new THREE.Scene();
  scene.fog = new THREE.FogExp2(0x0f172a, 0.002);

  camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 2000);
  camera.position.set(0, 0, 18);

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(window.devicePixelRatio);
  canvasRef.value.appendChild(renderer.domElement);

  const textureLoader = new THREE.TextureLoader();
  const earthGeo = new THREE.SphereGeometry(10, 64, 64);
  const earthMat = new THREE.MeshPhongMaterial({
    map: textureLoader.load('https://unpkg.com/three-globe/example/img/earth-blue-marble.jpg'),
    bumpMap: textureLoader.load('https://unpkg.com/three-globe/example/img/earth-topology.png'),
    bumpScale: 0.15,
    specular: new THREE.Color(0x222222),
    shininess: 5,
    emissive: new THREE.Color(0x112244),
    emissiveIntensity: 0.1
  });
  earthMesh = new THREE.Mesh(earthGeo, earthMat);
  earthMesh.position.x = -8; 
  scene.add(earthMesh);

  const atmoGeo = new THREE.SphereGeometry(7.4, 64, 64);
  const atmoMat = new THREE.MeshBasicMaterial({
    color: 0x4aa8ff, 
    transparent: true,
    opacity: 0.05,
    side: THREE.BackSide,
    blending: THREE.AdditiveBlending
  });
  atmosphereMesh = new THREE.Mesh(atmoGeo, atmoMat);
  atmosphereMesh.position.x = -8;
  scene.add(atmosphereMesh);

  const starsGeo = new THREE.BufferGeometry();
  const starCount = 8000;
  const posArray = new Float32Array(starCount * 3);
  const sizeArray = new Float32Array(starCount);

  for(let i = 0; i < starCount; i++) {
    const x = (Math.random() - 0.5) * 600; 
    const y = (Math.random() - 0.5) * 600;
    const z = (Math.random() - 0.5) * 600 - 100; 
    posArray[i * 3] = x;
    posArray[i * 3 + 1] = y;
    posArray[i * 3 + 2] = z;
    sizeArray[i] = Math.random() < 0.1 ? 0.25 : 0.1; 
  }

  starsGeo.setAttribute('position', new THREE.BufferAttribute(posArray, 3));
  starsGeo.setAttribute('size', new THREE.BufferAttribute(sizeArray, 1));

  const starsMat = new THREE.PointsMaterial({
    size: 0.12, 
    color: 0xffffff,
    transparent: true,
    opacity: 0.9,
    sizeAttenuation: true
  });
  starsMesh = new THREE.Points(starsGeo, starsMat);
  scene.add(starsMesh);

  const ambientLight = new THREE.AmbientLight(0xffffff, 1.5);
  scene.add(ambientLight);

  animate();
};

const animate = () => {
  animationId = requestAnimationFrame(animate);
  if (earthMesh) earthMesh.rotation.y += 0.0008;
  if (atmosphereMesh) atmosphereMesh.rotation.y += 0.0008;
  if (starsMesh) starsMesh.rotation.y -= 0.0001;
  renderer.render(scene, camera);
};

const handleResize = () => {
  if (!canvasRef.value) return;
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
};

onMounted(() => {
  initThree();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  cancelAnimationFrame(animationId);
  window.removeEventListener('resize', handleResize);
  renderer.dispose();
});
</script>

<template>
  <div class="relative w-full h-screen overflow-hidden bg-[#050b14] text-white font-sans">
    
    <transition enter-active-class="transition ease-out duration-300" enter-from-class="transform -translate-y-10 opacity-0" enter-to-class="transform translate-y-0 opacity-100" leave-active-class="transition ease-in duration-200" leave-from-class="transform translate-y-0 opacity-100" leave-to-class="transform -translate-y-10 opacity-0">
      <div v-if="toast.show" class="fixed top-6 left-1/2 transform -translate-x-1/2 z-50 flex items-center px-6 py-3 rounded-full shadow-2xl backdrop-blur-md border border-white/10" 
           :class="toast.type === 'success' ? 'bg-green-500/20 text-green-200' : 'bg-red-500/20 text-red-200'">
        <span class="mr-2 text-lg">{{ toast.type === 'success' ? '✓' : '!' }}</span>
        <span class="text-sm font-medium tracking-wide">{{ toast.message }}</span>
      </div>
    </transition>

    <div class="absolute inset-0 bg-black z-0"></div>
    <div ref="canvasRef" class="absolute inset-0 z-10 pointer-events-none"></div>

    <div class="relative z-20 w-full h-full flex">
      <div class="hidden lg:block lg:w-1/2 h-full"></div>

      <div class="w-full lg:w-1/2 h-full flex flex-col justify-center items-center px-8 relative">
        
        <div class="absolute top-16 left-8 lg:left-12">
          <h1 class="text-5xl font-black tracking-[0.2em] font-mono text-white/90 drop-shadow-2xl">
            GEO-RAG
          </h1>
          <p class="text-xs text-blue-400 mt-2 tracking-widest uppercase opacity-80">Spatial Intelligence Engine</p>
        </div>

        <div class="w-full max-w-[400px] backdrop-blur-3xl bg-white/[0.02] border border-white/10 rounded-2xl shadow-2xl p-8 mt-12 transition-all duration-500">
          
          <h2 class="text-2xl font-medium mb-1 text-white">
            {{ mode === 'login' ? 'Welcome Back' : 'Initialize Account' }}
          </h2>
          <p class="text-sm text-gray-400 mb-8">
            {{ mode === 'login' ? 'Please enter your credentials.' : 'Set up your spatial access.' }}
          </p>

          <form @submit.prevent="handleSubmit" class="space-y-6">
            
            <template v-if="mode === 'login'">
              <div class="relative group">
                <input v-model="form.account" type="text" id="account" placeholder=" "
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                <label for="account" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 z-10 origin-[0] left-4 px-1 bg-transparent peer-focus:bg-[#0f172a] peer-focus:px-2 peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-2 pointer-events-none">
                  Account / Email
                </label>
              </div>

              <div class="relative group">
                <input v-model="form.password" :type="visibility.login ? 'text' : 'password'" id="password" placeholder=" "
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer pr-10" />
                <label for="password" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 z-10 origin-[0] left-4 px-1 bg-transparent peer-focus:bg-[#0f172a] peer-focus:px-2 peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-2 pointer-events-none">
                  Password
                </label>
                <button type="button" @click="visibility.login = !visibility.login" 
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition-colors focus:outline-none">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path v-if="visibility.login" stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                    <path v-if="visibility.login" stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path v-else stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                  </svg>
                </button>
              </div>
            </template>

            <template v-else>
               <div class="grid grid-cols-2 gap-4">
                <div class="relative group">
                  <input v-model="form.username" type="text" id="username" placeholder=" "
                    class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                  <label for="username" 
                    class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                    Username
                  </label>
                </div>
                <div class="relative group">
                  <input v-model="form.nickname" type="text" id="nickname" placeholder=" "
                    class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                  <label for="nickname" 
                    class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                    Nickname
                  </label>
                </div>
              </div>

              <div class="relative group">
                <input v-model="form.email" type="email" id="email" placeholder=" "
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                <label for="email" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                  Email
                </label>
                 <button type="button" @click="handleSendCode" :disabled="countdown > 0"
                  class="absolute right-2 top-2 px-3 py-1.5 text-xs rounded border border-white/20 text-gray-300 hover:text-white hover:border-white/50 transition-colors disabled:opacity-50">
                  {{ countdown > 0 ? `${countdown}s` : 'Code' }}
                </button>
              </div>

               <div class="relative group">
                <input v-model="form.verifyCode" type="text" id="vcode" placeholder=" "
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                <label for="vcode" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                  Verify Code
                </label>
              </div>

              <div class="relative group">
                <input v-model="form.password" :type="visibility.register ? 'text' : 'password'" id="regpass" placeholder=" "
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer pr-10" />
                <label for="regpass" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                  Password
                </label>
                <button type="button" @click="visibility.register = !visibility.register" 
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition-colors focus:outline-none">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path v-if="visibility.register" stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                    <path v-if="visibility.register" stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path v-else stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                  </svg>
                </button>
              </div>

              <div class="relative group">
                <input v-model="form.confirmPassword" :type="visibility.confirm ? 'text' : 'password'" id="confpass" placeholder=" "
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer pr-10" />
                <label for="confpass" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                  Confirm Password
                </label>
                <button type="button" @click="visibility.confirm = !visibility.confirm" 
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition-colors focus:outline-none">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-5 h-5">
                    <path v-if="visibility.confirm" stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                    <path v-if="visibility.confirm" stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path v-else stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                  </svg>
                </button>
              </div>
            </template>

            <button type="submit" :disabled="loading"
              class="w-full py-3.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-medium shadow-lg transition-all active:scale-[0.98] disabled:opacity-70 disabled:cursor-wait tracking-wide text-sm">
              {{ loading ? 'PROCESSING...' : (mode === 'login' ? 'ENTER SYSTEM' : 'REGISTER') }}
            </button>
          </form>

          <div class="mt-8 flex justify-center">
            <div class="flex items-center space-x-6 text-sm">
              <button @click="mode = 'login'" 
                :class="['transition-colors duration-300', mode === 'login' ? 'text-white font-bold border-b-2 border-blue-500 pb-1' : 'text-gray-500 hover:text-gray-300']">
                LOG IN
              </button>
              <button @click="mode = 'register'" 
                :class="['transition-colors duration-300', mode === 'register' ? 'text-white font-bold border-b-2 border-blue-500 pb-1' : 'text-gray-500 hover:text-gray-300']">
                SIGN UP
              </button>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.peer-focus\:bg-\[\#0f172a\] {
  background-color: #0f172a; 
}
</style>