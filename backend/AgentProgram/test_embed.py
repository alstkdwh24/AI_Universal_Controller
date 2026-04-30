from langchain_google_genai import GoogleGenerativeAIEmbeddings
import os
from dotenv import load_dotenv
from langchain_text_splitters import RecursiveCharacterTextSplitter

DOC_PATH = "AI_유니버설_컨트롤러_명세서.txt"

print(f"파일 존재: {os.path.exists(DOC_PATH)}")

with open(DOC_PATH, "r", encoding="utf-8") as f:
    raw_text = f.read()

print(f"파일 크기: {len(raw_text)}자")

splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
docs = splitter.create_documents([raw_text])
print(f"청크 수: {len(docs)}")


load_dotenv(override=True)

embeddings = GoogleGenerativeAIEmbeddings(model="gemini-embedding-001", task_type="RETRIEVAL_DOCUMENT", api_key=os.getenv("GOOGLE_API_KEY"))

result = embeddings.embed_query("What is the capital of France?")
print(result)
print(len(result))

# 실제 문서 청크 임베딩 테스트
result2 = embeddings.embed_documents([docs[0].page_content])
print(f"embed_documents 결과: {len(result2)}개")