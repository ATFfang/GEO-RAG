import uvicorn
import asyncio
import time
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import List, Dict, Any

app = FastAPI()

# 1. 定义与 Java 端匹配的数据模型
class ChatRequest(BaseModel):
    query: str                       # 用户当前问题
    history: List[Dict[str, str]] = [] # 历史上下文 [{"role": "user", "content": "..."}]

# 2. 模拟 AI 生成器
async def mock_ai_generator(query: str, history: List[Dict[str, str]]):
    """
    生成器函数：模拟大模型推理过程
    """
    
    # 模拟 1：打印接收到的上下文，方便你调试 Java 端是否传对
    print(f"\n[Python] 收到请求: {query}")
    print(f"[Python] 历史记录条数: {len(history)}")
    
    # 模拟 2：首字延迟 (Time to First Token)，模拟"思考中"
    await asyncio.sleep(1.0) 
    
    # 模拟 3：构建一个回复内容
    response_text = (
        f"你好！我收到了你的问题：“{query}”。\n"
        f"结合你之前的 {len(history)} 条历史记录，"
        f"经过复杂的地理空间分析，结果如下：\n"
        f"1. 这是一个模拟的流式回复。\n"
        f"2. 我正在一个字一个字地吐出数据。\n"
        f"3. 你的 Spring Boot 后端应该能实时收到这些字符。"
    )

    # 模拟 4：流式输出 (每隔 0.1 秒吐一个字)
    for char in response_text:
        yield char
        # 控制流速，模拟真实打字机效果
        await asyncio.sleep(0.05)

# 3. 定义接口
@app.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    """
    流式对话接口
    """
    return StreamingResponse(
        mock_ai_generator(request.query, request.history),
        media_type="text/event-stream" # 或者 "text/plain"，Java 端都能处理
    )

# 4. 启动服务
if __name__ == "__main__":
    print("启动模拟 AI 服务，监听端口 8000...")
    # 对应 Java 代码里的 .uri("/chat/stream") 和 baseUrl
    uvicorn.run(app, host="0.0.0.0", port=8000)