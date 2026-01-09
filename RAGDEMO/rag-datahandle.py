import os
from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.embeddings import TongyiEmbeddings
from langchain_postgres.vectorstores import PGVector

# --- 配置区 ---
os.environ["DASHSCOPE_API_KEY"] = "sk-873463eacac84219b830314fb5f10992"
FILE_PATH = "/Users/fangtianyao/Downloads/家庭实用菜谱大全.pdf"
# 修改为你的数据库连接信息
CONNECTION_STRING = "postgresql+psycopg2://postgres:postgres@localhost:5432/rag"
COLLECTION_NAME = "recipe_knowledge_base"

def ingest_pdf():
    # 1. 加载 PDF (按照我们之前讨论的，PyPDFLoader 适合处理多页菜谱)
    print(f"正在加载文件: {FILE_PATH}...")
    loader = PyPDFLoader(FILE_PATH)
    documents = loader.load()

    # 2. 文本切分 (Chunking)
    # 菜谱通常由 菜名+做法 组成，建议 Chunk Size 设为 500 左右，保证一个菜谱尽量完整
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=500, 
        chunk_overlap=50
    )
    chunks = text_splitter.split_documents(documents)
    print(f"切分完成，共计 {len(chunks)} 个文本块。")

    # 3. 初始化通义千问 Embedding 模型
    embeddings = TongyiEmbeddings()

    # 4. 初始化 PGVector 并存入数据
    # 如果表不存在，LangChain 会自动在数据库中创建表
    print("正在计算向量并写入 PostgreSQL...")
    vector_store = PGVector.from_documents(
        embedding=embeddings,
        documents=chunks,
        collection_name=COLLECTION_NAME,
        connection=CONNECTION_STRING,
        use_jsonb=True,
    )
    print("入库成功！现在你可以开始进行菜谱问答了。")

if __name__ == "__main__":
    ingest_pdf()