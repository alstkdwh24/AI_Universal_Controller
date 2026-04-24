# 이 코드는 FastAPI에서 **API 엔드포인트(라우터)**를 정의하는 부분이다. Spring의 @RestController와 유사한 역할을 하는 FastAPI의 APIRouter를 사용하여 API 경로와 핸들러 함수를 정의한다.

# 여러 API 엔드포인트를 그룹핑할 수 있는 라우터 객체를 가져옵니다.
from fastapi import APIRouter

# 요청(Request)과 응답(Response) 데이터 구조(DTO, paydantic 모델)를 정의한 스키마를 가져옵니다.
from schemas.chat_schema import ChatRequest, ChatResponse

# 실제로 질문을 받아 RAG/LLM 답변을 생성하는 서비스 함수입니다.
from services.agent_service import get_rag_answer

# 이 파일에서 사용할 라우터 함수 생성
router = APIRouter(prefix="/agent", tags=["Agent Operations"])
# 이 함수가 HTTP POST 방식의 "/agent/chat" 엔트포인트를 처리하도록 지정

@router.post("/chat", response_model=ChatResponse)
def chat_with_agent(request: ChatRequest):
    answer = get_rag_answer(request.question)
    return ChatResponse(answer=answer)