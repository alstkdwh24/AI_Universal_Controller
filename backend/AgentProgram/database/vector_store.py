# 벡터 데이터베이스 관련 파일
import os
import logging
import requests
from dotenv import load_dotenv
from langchain_chroma import Chroma
from bs4 import BeautifulSoup
from schemas.google_search_util import perform_google_web_search
from langchain_google_genai import GoogleGenerativeAIEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter

load_dotenv(override=True)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 문서 저장용 임베딩 (retrieval_document)
doc_embeddings = GoogleGenerativeAIEmbeddings(
    model="gemini-embedding-001",
    task_type="retrieval_document",
    google_api_key=os.getenv("GOOGLE_API_KEY")
)

# 검색 쿼리용 임베딩 (retrieval_query)
query_embeddings = GoogleGenerativeAIEmbeddings(
    model="gemini-embedding-001",
    task_type="retrieval_query",
    google_api_key=os.getenv("GOOGLE_API_KEY")
)

# Chroma 벡터스토어 생성
vector_store = Chroma(
    embedding_function=doc_embeddings,
    collection_name="agent_documents",
    persist_directory="./chroma_db"
)

# Google 검색을 수행하는 함수
def google_search(query: str):
    search_url = perform_google_web_search(query)
    logger.info(f"Google 검색 결과: {search_url}")
    return search_url



# 벡터스토어에서 유사한 문서를 검색하는 함수
def similarity_search(query: str, k: int = 5):
    # 검색 쿼리를 임베딩하여 벡터스토어에서 유사한 문서를 검색합니다.
    query_vector_store = Chroma(
        embedding_function=query_embeddings,
        collection_name="agent_documents",
        persist_directory="./chroma_db"
    )
    return query_vector_store.similarity_search(query, k=k)

# 초기 데이터 주입 로직
all_docs_count = vector_store._collection.count()

if all_docs_count == 0:
    DOC_PATH = "AI_유니버설_컨트롤러_명세서.txt"
    # 문서 파일이 존재하지 않으면 경고 로그를 출력하고 빈 벡터 DB로 시작합니다.
    if not os.path.exists(DOC_PATH):
        logger.warning(f"문서 파일을 찾을 수 없습니다: {DOC_PATH}")
        logger.warning("벡터 DB가 비어 있는 상태로 시작합니다.")
    else:
        with open(DOC_PATH, "r", encoding="utf-8") as f:
            raw_text = f.read()

        splitter = RecursiveCharacterTextSplitter(
            chunk_size=500,
            chunk_overlap=50
        )
        docs = splitter.create_documents([raw_text])
        try:
            vector_store.add_documents(docs)
            logger.info(f"{len(docs)}개 문서 저장 완료!")
        except Exception as e:
            logger.error(f"문서 임베딩 실패 (GOOGLE_API_KEY 확인 필요): {e}")
            logger.warning("벡터 DB가 비어 있는 상태로 시작합니다.")

# 문서 추가 함수
def add_document(text: str):
    # 긴 텍스트를 일정크기로 잘라 준다.
    splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
    # 잘라진 텍스트를 문서 객체로 만들어 벡터스토어에 저장한다.
    docs = splitter.create_documents([text])
    vector_store.add_documents(docs)

# 문서 갯수 조회
def get_document_count():
    return vector_store._collection.count()

# 모든 문서 삭제
def delete_all_documents():
    vector_store._collection.delete(where={"source":{"$ne": ""}})