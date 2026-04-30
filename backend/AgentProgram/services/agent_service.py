# services/agent_service.py
from database.vector_store import similarity_search, google_search
from langchain_google_genai import ChatGoogleGenerativeAI
import logging

logger = logging.getLogger(__name__)
llm = ChatGoogleGenerativeAI(model="gemini-2.5-flash")

def get_rag_answer(question: str):
    # 1. 먼저 내 DB에서 찾아봅니다.
    docs = similarity_search(question, k=3)

    context = ""
    source = ""

    if len(docs) > 0:
        logger.info("내부 DB에서 정보를 찾았습니다.")
        context = "\n".join([doc.page_content for doc in docs])
        source = "Internal Vector DB"

    # 3. Gemini에게 정보를 주고 답변 생성 요청

        prompt = f"""
        당신은 민상님의 AI 비서입니다. 제공된 [참고 정보]를 바탕으로 질문에 답변하세요.
        출처: {source}
        참고 정보: {context}
        질문: {question}
        """
        response = llm.invoke(prompt)
        return response.content
    else:
        # 2. DB에 없으면 구글 검색
        logger.warning("내부 데이터 부족. 실시간 구글 검색을 수행합니다.")
        search_answer = google_search(question)

        if search_answer:
            return search_answer
        else:
            return "죄송합니다. 내부 DB와 구글 검색 모두에서 정보를 찾지 못했습니다."

