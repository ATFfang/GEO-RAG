import os
from langchain_community.chat_models import ChatTongyi
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# 设置 API Key
os.environ["DASHSCOPE_API_KEY"] = "sk-873463eacac84219b830314fb5f10992"

# 初始化模型
llm = ChatTongyi(model_name="qwen-max")

# 简单的测试链路
prompt = ChatPromptTemplate.from_template("讲个关于{topic}的笑话")
chain = prompt | llm | StrOutputParser()

print(chain.invoke({"topic": "程序员"}))