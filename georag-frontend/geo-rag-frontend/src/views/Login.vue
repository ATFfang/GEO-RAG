<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import * as THREE from 'three';
import { userApi, type LoginParams, type RegisterParams } from '../api/user';

const router = useRouter();

// --- 状态管理 ---
const mode = ref<'login' | 'register'>('login'); 
const loading = ref(false);
const countdown = ref(0); 

const form = reactive({
  account: '',    
  password: '',   
  username: '',   
  nickname: '',   
  email: '',      
  verifyCode: ''  
});

// --- 交互逻辑 (保持不变) ---
const handleSendCode = async () => {
  if (!form.email) return alert('请输入邮箱');
  try {
    await userApi.sendRegisterCode(form.email);
    countdown.value = 60;
    const timer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) clearInterval(timer);
    }, 1000);
    alert('验证码已发送');
  } catch (error: any) {
    alert(error.msg || '发送失败'); 
  }
};

const handleSubmit = async () => {
  loading.value = true;
  try {
    if (mode.value === 'login') {
      const res = await userApi.login({ account: form.account, password: form.password });
      if(res.code === 200) {
        localStorage.setItem('token', res.data.token);
        localStorage.setItem('userInfo', JSON.stringify(res.data));
        router.push('/');
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
        alert('注册成功');
        router.push('/');
      }
    }
  } catch (error: any) {
    alert(error.msg || '操作失败');
  } finally {
    loading.value = false;
  }
};

// --- Three.js 3D 场景调整 ---
const canvasRef = ref<HTMLElement | null>(null);
let scene: THREE.Scene, camera: THREE.PerspectiveCamera, renderer: THREE.WebGLRenderer;
let earthMesh: THREE.Mesh, starsMesh: THREE.Points, atmosphereMesh: THREE.Mesh;
let animationId: number;

const initThree = () => {
  if (!canvasRef.value) return;

  scene = new THREE.Scene();
  // 1. 降低雾化浓度，让远处的星星也能看见一点
  scene.fog = new THREE.FogExp2(0x0f172a, 0.002);

  camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 2000);
  camera.position.set(0, 0, 18);

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(window.devicePixelRatio);
  canvasRef.value.appendChild(renderer.domElement);

  // --- 地球 (位置 x=-8, 整体发光) ---
  const textureLoader = new THREE.TextureLoader();
  const earthGeo = new THREE.SphereGeometry(7, 64, 64);
  const earthMat = new THREE.MeshPhongMaterial({
    map: textureLoader.load('https://unpkg.com/three-globe/example/img/earth-blue-marble.jpg'),
    bumpMap: textureLoader.load('https://unpkg.com/three-globe/example/img/earth-topology.png'),
    bumpScale: 0.15,
    specular: new THREE.Color(0x222222),
    shininess: 5,
    emissive: new THREE.Color(0x112244), // 自发光，去除黑暗面
    emissiveIntensity: 0.1
  });
  earthMesh = new THREE.Mesh(earthGeo, earthMat);
  earthMesh.position.x = -8; 
  scene.add(earthMesh);

  // --- 大气层 (增强光晕) ---
  const atmoGeo = new THREE.SphereGeometry(7.4, 64, 64);
  const atmoMat = new THREE.MeshBasicMaterial({
    color: 0x4aa8ff, 
    transparent: true,
    opacity: 0.2, // 增加不透明度，让光晕更明显
    side: THREE.BackSide,
    blending: THREE.AdditiveBlending
  });
  atmosphereMesh = new THREE.Mesh(atmoGeo, atmoMat);
  atmosphereMesh.position.x = -8;
  scene.add(atmosphereMesh);

  // --- 星星 (增量 + 随机大小) ---
  const starsGeo = new THREE.BufferGeometry();
  const starCount = 8000; // 数量翻倍
  const posArray = new Float32Array(starCount * 3);
  const sizeArray = new Float32Array(starCount); // 增加大小属性

  for(let i = 0; i < starCount; i++) {
    // 扩大分布范围，避免看起来像个盒子
    const x = (Math.random() - 0.5) * 600; 
    const y = (Math.random() - 0.5) * 600;
    const z = (Math.random() - 0.5) * 600 - 100; 
    
    posArray[i * 3] = x;
    posArray[i * 3 + 1] = y;
    posArray[i * 3 + 2] = z;

    // 随机大小: 大部分是小星星，少量大星星
    sizeArray[i] = Math.random() < 0.1 ? 0.25 : 0.1; 
  }

  starsGeo.setAttribute('position', new THREE.BufferAttribute(posArray, 3));
  starsGeo.setAttribute('size', new THREE.BufferAttribute(sizeArray, 1)); // 如果使用ShaderMaterial才生效，这里用PointsMaterial做简单处理

  const starsMat = new THREE.PointsMaterial({
    size: 0.12, 
    color: 0xffffff,
    transparent: true,
    opacity: 0.9,
    sizeAttenuation: true // 开启近大远小
  });
  starsMesh = new THREE.Points(starsGeo, starsMat);
  scene.add(starsMesh);

  // --- 灯光 (去除晨昏线，改用全亮) ---
  const ambientLight = new THREE.AmbientLight(0xffffff, 1.2); // 强环境光
  scene.add(ambientLight);

  // 补充一个正面柔光，让纹理更清晰
  const frontLight = new THREE.DirectionalLight(0xffffff, 0.5);
  frontLight.position.set(0, 0, 20);
  scene.add(frontLight);

  animate();
};

const animate = () => {
  animationId = requestAnimationFrame(animate);
  if (earthMesh) earthMesh.rotation.y += 0.0008; // 转速稍快一点点
  if (atmosphereMesh) atmosphereMesh.rotation.y += 0.0008;
  if (starsMesh) starsMesh.rotation.y -= 0.0001; // 星空极慢旋转
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

        <div class="w-full max-w-[400px] backdrop-blur-3xl bg-white/[0.02] border border-white/10 rounded-2xl shadow-2xl p-8 mt-12">
          
          <h2 class="text-2xl font-medium mb-1 text-white">
            {{ mode === 'login' ? 'Welcome Back' : 'Initialize Account' }}
          </h2>
          <p class="text-sm text-gray-400 mb-8">
            {{ mode === 'login' ? 'Please enter your credentials.' : 'Set up your spatial access.' }}
          </p>

          <form @submit.prevent="handleSubmit" class="space-y-6">
            
            <template v-if="mode === 'login'">
              <div class="relative group">
                <input v-model="form.account" type="text" id="account" placeholder=" " required
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                <label for="account" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 z-10 origin-[0] left-4 px-1 bg-transparent peer-focus:bg-[#0f172a] peer-focus:px-2 peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-2 pointer-events-none">
                  Account / Email
                </label>
              </div>

              <div class="relative group">
                <input v-model="form.password" type="password" id="password" placeholder=" " required
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                <label for="password" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 z-10 origin-[0] left-4 px-1 bg-transparent peer-focus:bg-[#0f172a] peer-focus:px-2 peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-2 pointer-events-none">
                  Password
                </label>
              </div>
            </template>

            <template v-else>
               <div class="grid grid-cols-2 gap-4">
                <div class="relative group">
                  <input v-model="form.username" type="text" id="username" placeholder=" " required
                    class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                  <label for="username" 
                    class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                    Username
                  </label>
                </div>
                <div class="relative group">
                  <input v-model="form.nickname" type="text" id="nickname" placeholder=" " required
                    class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                  <label for="nickname" 
                    class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                    Nickname
                  </label>
                </div>
              </div>

              <div class="relative group">
                <input v-model="form.email" type="email" id="email" placeholder=" " required
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
                <input v-model="form.verifyCode" type="text" id="vcode" placeholder=" " required
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                <label for="vcode" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                  Verify Code
                </label>
              </div>

               <div class="relative group">
                <input v-model="form.password" type="password" id="regpass" placeholder=" " required
                  class="block px-4 py-3 w-full text-sm text-white bg-transparent border border-white/20 rounded-lg appearance-none focus:outline-none focus:ring-0 focus:border-blue-500 peer" />
                <label for="regpass" 
                  class="absolute text-sm text-gray-400 duration-300 transform -translate-y-1/2 scale-100 top-1/2 origin-[0] left-4 bg-transparent peer-focus:text-blue-500 peer-focus:scale-90 peer-focus:-translate-y-[2.4rem] peer-focus:bg-[#0f172a] peer-focus:px-1 peer-[:not(:placeholder-shown)]:scale-90 peer-[:not(:placeholder-shown)]:-translate-y-[2.4rem] peer-[:not(:placeholder-shown)]:bg-[#0f172a] peer-[:not(:placeholder-shown)]:px-1 pointer-events-none">
                  Password
                </label>
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
/* 确保背景色和 Tailwind 变量一致，防止上浮文字背景穿帮 */
.peer-focus\:bg-\[\#0f172a\] {
  background-color: #0f172a; /* 对应 Login 面板的视觉背景色，这里为了遮挡边框线 */
}
</style>