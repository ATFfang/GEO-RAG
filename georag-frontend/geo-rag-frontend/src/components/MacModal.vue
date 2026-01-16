<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';

const props = defineProps<{
  show: boolean;
  type: 'confirm' | 'input' | 'danger'; // danger 用于删除
  title: string;
  content?: string;
  placeholder?: string;
  initialValue?: string;
}>();

const emit = defineEmits(['close', 'confirm']);
const inputValue = ref('');
const inputRef = ref<HTMLInputElement | null>(null);

// 打开时如果是输入模式，自动聚焦
watch(() => props.show, async (newVal) => {
  if (newVal && props.type === 'input') {
    inputValue.value = props.initialValue || '';
    await nextTick();
    inputRef.value?.focus();
  }
});

const handleConfirm = () => {
  if (props.type === 'input') {
    emit('confirm', inputValue.value);
  } else {
    emit('confirm');
  }
};
</script>

<template>
  <transition name="modal">
    <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/40 backdrop-blur-[2px] transition-opacity" @click="$emit('close')"></div>

      <div class="relative w-full max-w-[320px] bg-[#1c1c1e]/90 backdrop-blur-xl border border-white/10 rounded-2xl shadow-2xl overflow-hidden transform transition-all scale-100">
        
        <div class="p-6 text-center space-y-4">
          <div class="space-y-1">
            <h3 class="text-[17px] font-semibold text-white leading-6">{{ title }}</h3>
            <p v-if="content" class="text-[13px] text-gray-400 leading-5">{{ content }}</p>
          </div>

          <div v-if="type === 'input'" class="mt-2">
            <input 
              ref="inputRef"
              v-model="inputValue"
              type="text" 
              :placeholder="placeholder"
              @keydown.enter="handleConfirm"
              class="w-full bg-black/20 border border-white/10 text-white text-[13px] rounded-lg px-3 py-2 focus:outline-none focus:border-blue-500/50 focus:bg-black/40 transition-all placeholder-gray-600"
            />
          </div>
        </div>

        <div class="grid grid-cols-2 border-t border-white/10 divide-x divide-white/10">
          <button 
            @click="$emit('close')"
            class="py-3 text-[15px] text-gray-400 hover:bg-white/5 active:bg-white/10 transition-colors font-normal"
          >
            Cancel
          </button>
          <button 
            @click="handleConfirm"
            :class="[
              'py-3 text-[15px] hover:bg-white/5 active:bg-white/10 transition-colors font-medium',
              type === 'danger' ? 'text-red-400' : 'text-[#0a84ff]'
            ]"
          >
            {{ type === 'input' ? 'Save' : (type === 'danger' ? 'Delete' : 'Confirm') }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
/* 弹窗动画 */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>