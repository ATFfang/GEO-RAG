from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_postgres.vectorstores import PGVector

FILE_PATH = "/Users/fangtianyao/Downloads/家庭实用菜谱大全.pdf"
CONNECTION_STRING = "postgresql+psycopg2://postgres:postgres@localhost:5432/rag"
COLLECTION_NAME = "recipe_collection_bge"

def main():
    # 1. 加载本地 Embedding 模型 (BGE-small-zh)
    print("正在加载本地 BGE Embedding 模型...")
    embeddings = HuggingFaceEmbeddings(
        model_name="BAAI/bge-small-zh-v1.5",
        model_kwargs={'device': 'cpu'},
        encode_kwargs={'normalize_embeddings': True}
    )

    # 2. 加载 PDF
    print(f"正在读取 PDF: {FILE_PATH}")
    loader = PyPDFLoader(FILE_PATH)
    documents = loader.load()

    # 3. 切分文档
    text_splitter = RecursiveCharacterTextSplitter(chunk_size=600, chunk_overlap=50)
    chunks = text_splitter.split_documents(documents)
    print(f"文档切分完成，共 {len(chunks)} 个片段。")

    # 4. 存入 PostgreSQL
    print("正在计算向量并存入数据库...")
    vector_store = PGVector.from_documents(
        embedding=embeddings,
        documents=chunks,
        collection_name=COLLECTION_NAME,
        connection=CONNECTION_STRING,
        use_jsonb=True,
    )
    print("数据入库成功！")

if __name__ == "__main__":
    main()