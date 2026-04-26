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

def google_search(query: str):
    search_url = perform_google_web_search(query)
    logger.info(f"Google 검색 결과: {search_url}")
    return search_url

def fetch_text_from_url(url):
    try:
        res = requests.get(url, timeout=5)
        soup = BeautifulSoup(res.text, "html.parser")
        paragraphs = [p.get_text() for p in soup.find_all("p")]
        return "\n".join(paragraphs)
    except Exception as e:
        logger.error(f"URL에서 텍스트를 가져오는 중 오류 발생: {e}")
        return ""

def similarity_search(query: str, k: int = 5):
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