

from database import vector_store


def get_rag_answer(question: str) -> str:
    docs = vector_store.similarity_search(question, k=5)
    context = "\n".join([doc.page_content for doc in docs])
    # RAG/LLM 모델과의 통신을 통해 답변을 생성하는 로직을 구현합니다.
    # 예시로 간단한 답변을 반환하도록 하겠습니다.
    return f"RAG/LLM 모델이 생성한 답변: {context}"