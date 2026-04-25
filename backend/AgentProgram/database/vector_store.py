#벡터 데이터 베이스 관련한 파일
import os
from dotenv import load_dotenv
load_dotenv()  # .env 파일에서 GOOGLE_API_KEY를 로드합니다.

from langchain_chroma import Chroma
from langchain_google_genai import GoogleGenerativeAIEmbeddings

    
embeddings = GoogleGenerativeAIEmbeddings(model = "gemini-flash3.5", google_api_key=os.getenv("GOOGLE_API_KEY"))  # Gemini Embeddings 사용용
vector_store = Chroma(embedding_function=embeddings, collection_name="agent_documents", persist_directory="./chroma_db")