import { ref, nextTick } from 'vue';
import { chatApi, type ChatSession, type ChatMessage } from '../api/chat';

export function useChat() {
    // --- 核心状态 ---
    const inputMessage = ref('');
    const isTyping = ref(false); // 是否正在接收流
    const chatContainer = ref<HTMLElement | null>(null);

    const sessionList = ref<ChatSession[]>([]);
    const currentSessionId = ref<string>('');
    const messages = ref<ChatMessage[]>([]);
    const loadingHistory = ref(false);

    // --- 辅助：滚动到底部 ---
    const scrollToBottom = async () => {
        await nextTick();
        if (chatContainer.value) {
            chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
        }
    };

    // --- 逻辑 1: 加载会话列表 ---
    const loadSessions = async () => {
        try {
            const res = await chatApi.getSessions();
            if (res.code === 200 && res.data && Array.isArray(res.data.records)) {
                sessionList.value = res.data.records;
                console.log('Sessions loaded:', res.data.records);
            }
        } catch (error) {
            console.error('Failed to load sessions:', error);
        }
    };

    // --- 逻辑 2: 切换会话 ---
    const switchSession = async (sessionId: string) => {
        if (currentSessionId.value === sessionId) return;

        currentSessionId.value = sessionId;
        messages.value = [];
        loadingHistory.value = true;

        try {
            const res = await chatApi.getSessionMessages(sessionId);
            if (res.code === 200) {
                messages.value = res.data.map((item: any) => ({
                    ...item,
                    content: item.context // 适配后端字段 context -> content
                }));
                await scrollToBottom();
            }
        } catch (error) {
            console.error('History Error:', error);
        } finally {
            loadingHistory.value = false;
        }
    };

    // --- 逻辑 3: 新建会话 ---
    const createNewChat = () => {
        currentSessionId.value = '';
        messages.value = [];
        inputMessage.value = '';
        isTyping.value = false;
    };

    // --- 逻辑 4: 删除会话 ---
    const deleteSession = async (sessionId: string) => {
        try {
            const res = await chatApi.deleteSession(sessionId);
            if (res.code === 200) {
                sessionList.value = sessionList.value.filter(s => s.id !== sessionId);
                if (currentSessionId.value === sessionId) createNewChat();
            }
        } catch (e) { alert('删除失败'); }
    };

    // --- 逻辑 5: 发送消息 (SSE 流式) ---
    const handleSend = async (textOverride?: string) => {
        const text = textOverride || inputMessage.value;
        const content = text.trim();
        if (!content || isTyping.value) return;

        // 1. 用户消息上屏
        const tempUserMsg = {
            id: `temp-${Date.now()}`,
            role: 'user',
            context: content,
            createTime: new Date().toISOString()
        };
        messages.value.push(tempUserMsg as any);
        inputMessage.value = '';
        await scrollToBottom();
        isTyping.value = true;

        // 2. AI 消息占位
        const aiMsg = {
            id: `ai-${Date.now()}`,
            role: 'assistant',
            context: '',
            createTime: new Date().toISOString()
        };
        messages.value.push(aiMsg as any);

        // 修改原始 aiMsg 变量不会触发 UI 更新，必须修改 messages.value 里的东西
        const aiMsgIndex = messages.value.length - 1;

        try {
            const token = localStorage.getItem('token');
            const requestBody: any = { content };
            if (currentSessionId.value) {
                requestBody.sessionId = currentSessionId.value;
            }
            const response = await fetch('/api/v1/chat/completions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.body) throw new Error('No body');
            const reader = response.body.getReader();
            const decoder = new TextDecoder('utf-8');
            let buffer = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });

                // 处理可能得粘包
                const lines = buffer.split('\n');
                // 暂时保留最后一行（可能不完整）
                buffer = lines.pop() || '';

                for (const line of lines) {
                    let trimmed = line.trim();
                    if (!trimmed) continue;

                    // 兼容处理
                    if (trimmed.startsWith('data:')) {
                        trimmed = trimmed.replace(/^data:/, '').trim();
                    }

                    // 处理 {"text":"A"}{"text":"B"} 这种粘连的 JSON
                    const jsonObjects = trimmed.split(/}(?={)/).map((s, i, a) => {
                        if (i < a.length - 1) return s + '}';
                        return s;
                    });

                    for (const jsonObj of jsonObjects) {
                        try {
                            const data = JSON.parse(jsonObj);

                            // 1. 捕获 SessionId
                            if (data.sessionId && !currentSessionId.value) {
                                currentSessionId.value = data.sessionId;
                                loadSessions();
                            }

                            // 2. 核心：更新文本
                            if (data.text && messages.value[aiMsgIndex]) {
                                // ★★★ 核心修复 2：通过索引直接修改响应式数组中的对象 ★★★
                                messages.value[aiMsgIndex].context += data.text;

                                // 强制滚动
                                if (chatContainer.value) {
                                    chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
                                }
                            }

                            // 3. 结束标志
                            if (data.finish === true) {
                                isTyping.value = false;
                            }
                        } catch (e) {
                            console.warn("Partial JSON chunk received, waiting for next packet");
                        }
                    }
                }
            }
        } catch (error) {
            console.error('Stream processing failed:', error);
            // 同样使用索引更新报错信息
            if (messages.value[aiMsgIndex]) {
                messages.value[aiMsgIndex].context += '\n[流式解析失败]';
            }
        } finally {
            isTyping.value = false;
            await scrollToBottom();
        }
    };

    // --- 逻辑 6: 修改标题 ---
    const updateSessionTitle = async (sessionId: string, title: string) => {
        try {
            const res = await chatApi.updateSessionTitle(sessionId, title);
            if (res.code === 200) {
                const session = sessionList.value.find(s => s.id === sessionId);
                if (session) session.title = title;
            }
        } catch (e) { alert('修改失败'); }
    };

    return {
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
        handleSend,
        updateSessionTitle
    };
}