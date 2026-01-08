<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue';
import * as THREE from 'three';

const container = ref<HTMLElement | null>(null);
let scene: THREE.Scene, camera: THREE.PerspectiveCamera, renderer: THREE.WebGLRenderer;
let earth: THREE.Mesh, atmosphere: THREE.Mesh, stars: THREE.Points;
let animationId: number;

const init = () => {
  if (!container.value) return;

  // 1. 场景
  scene = new THREE.Scene();
  
  // 2. 相机
  camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 1000);
  camera.position.z = 15; // 距离适中

  // 3. 渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setSize(window.innerWidth, window.innerHeight);
  renderer.setPixelRatio(window.devicePixelRatio);
  container.value.appendChild(renderer.domElement);

  // 4. 材质 (使用在线 NASA 纹理，实际项目中请下载到 assets)
  const textureLoader = new THREE.TextureLoader();
  const earthMap = textureLoader.load('https://unpkg.com/three-globe/example/img/earth-blue-marble.jpg');
  const cloudMap = textureLoader.load('https://unpkg.com/three-globe/example/img/earth-topology.png');

  // 地球本体
  const earthGeometry = new THREE.SphereGeometry(5, 64, 64);
  const earthMaterial = new THREE.MeshPhongMaterial({
    map: earthMap,
    bumpMap: cloudMap,
    bumpScale: 0.05,
    specularMap: cloudMap,
    specular: new THREE.Color('grey'),
    shininess: 10
  });
  earth = new THREE.Mesh(earthGeometry, earthMaterial);
  scene.add(earth);

  // 大气层光晕 (Shader 模拟)
  const atmoGeometry = new THREE.SphereGeometry(5.2, 64, 64);
  const atmoMaterial = new THREE.MeshBasicMaterial({
    color: 0x3b82f6,
    transparent: true,
    opacity: 0.1,
    side: THREE.BackSide,
    blending: THREE.AdditiveBlending
  });
  atmosphere = new THREE.Mesh(atmoGeometry, atmoMaterial);
  scene.add(atmosphere);

  // 星空背景
  const starGeometry = new THREE.BufferGeometry();
  const starMaterial = new THREE.PointsMaterial({ color: 0xffffff, size: 0.05 });
  const starVertices = [];
  for(let i=0; i<2000; i++) {
    const x = (Math.random() - 0.5) * 200;
    const y = (Math.random() - 0.5) * 200;
    const z = - (Math.random()) * 100; // 只在背面
    starVertices.push(x,y,z);
  }
  starGeometry.setAttribute('position', new THREE.Float32BufferAttribute(starVertices, 3));
  stars = new THREE.Points(starGeometry, starMaterial);
  scene.add(stars);

  // 灯光
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.2);
  scene.add(ambientLight);
  const sunLight = new THREE.DirectionalLight(0xffffff, 1.5);
  sunLight.position.set(10, 5, 10);
  scene.add(sunLight);

  animate();
};

const animate = () => {
  animationId = requestAnimationFrame(animate);
  
  // 自转
  if (earth) earth.rotation.y += 0.002;
  if (atmosphere) atmosphere.rotation.y += 0.002;
  if (stars) stars.rotation.z -= 0.0002;

  renderer.render(scene, camera);
};

const onResize = () => {
  if (!container.value) return;
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
};

onMounted(() => {
  init();
  window.addEventListener('resize', onResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  cancelAnimationFrame(animationId);
  renderer.dispose();
});
</script>

<template>
  <div ref="container" class="fixed inset-0 z-0 bg-gemini-dark pointer-events-none"></div>
</template>