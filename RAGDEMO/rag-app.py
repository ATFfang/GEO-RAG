import os

from langchain_community.chat_models import ChatTongyi
from langchain_postgres.vectorstores import PGVector
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser
from langchain_core.runnables import RunnablePassthrough

os.environ["DASHSCOPE_API_KEY"] = "sk-873463eacac84219b830314fb5f10992"

# 数据库配置
CONNECTION_STRING = "postgresql+psycopg2://postgres:postgres@localhost:5432/rag"
COLLECTION_NAME = "recipe_collection_bge"

# 初始化 Embedding 模型
print("正在加载本地 BGE 模型...")
embeddings = HuggingFaceEmbeddings(
    model_name="BAAI/bge-small-zh-v1.5",
    model_kwargs={'device': 'cpu'},
    encode_kwargs={'normalize_embeddings': True}
)

# 连接现有的向量数据库
print(f"正在连接数据库集合: {COLLECTION_NAME}...")
vector_store = PGVector(
    connection=CONNECTION_STRING,
    embeddings=embeddings,
    collection_name=COLLECTION_NAME,
    use_jsonb=True,
)

# 创建检索器 (Retriever)
# k=3 表示每次检索最相似的 3 条菜谱
retriever = vector_store.as_retriever(search_kwargs={"k": 3})

# 定义 LLM 和 Prompt (LCEL 风格) 
# 初始化通义千问 (Qwen-Max)
llm = ChatTongyi(model_name="qwen-max")

# 编写提示词模板
template = """你是一个专业的智能大厨。请基于下面的【参考资料】回答用户的问题。
如果参考资料里没有提到的内容，请诚实地说“资料中未提及”，不要编造。

【参考资料】：
{context}

【用户问题】：
{question}
"""
prompt = ChatPromptTemplate.from_template(template)

# 辅助函数：将检索到的文档列表合并成一个字符串
def format_docs(docs):
    return "\n\n".join([doc.page_content for doc in docs])

# 构建 RAG 链 (The Chain) 
rag_chain = (
    {"context": retriever | format_docs, "question": RunnablePassthrough()}
    | prompt
    | llm
    | StrOutputParser()
)

# --- 7. 执行对话 ---
if __name__ == "__main__":
    
    while True:
        question = input("\n请问有什么可以帮您: ")
        if question.lower() in ["quit", "exit"]:
            break
        
        print("\n正在思考并检索数据库...", end="")
        
        try:
            # 使用流式输出 (Stream) 提升体验
            print("\n回答: ", end="")
            for chunk in rag_chain.stream(question):
                print(chunk, end="", flush=True)
            print("\n")
        except Exception as e:
            print(f"\n发生错误: {e}")