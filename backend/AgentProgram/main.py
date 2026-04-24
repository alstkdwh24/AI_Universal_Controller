# 메인 py파일



from contextlib import asynccontextmanager
from logging import log

from fastapi import FastAPI

from backend.AgentProgram.api import agent_router

# 이 코드는 FastAPI 서버의 앱 인스턴스 생성과 동시에 API 문서에 표시될 앱 정보를 성정하는 부분
app = FastAPI(
    title="AI Agent Controller API and local LLM",
    description="AI Agent Controller API와 로컬 LLM을 활용한 에이전트 프로그램",
    version="1.0.0"
)

# 앱 라우터 등록 (엔드 포인트 그룹핑)
app.include_router(agent_router, tags=["Agent Operations"])

# 클라우드 AI 에이전트 명령 및 로컬 LLM 명령을 처리하는 라우터는 agent_router에 정의되어 있으며, "/agent" 경로로 접근할 수 있습니다.
@app.get("/")
def agent_order():
    return {"message": "AI Agent Controller API is running!"}

# 서버 시작 시 토기화 코드
@asynccontextmanager
async def lifespan(app: FastAPI):
    log.debug("Starting AI Agent Controller API...")
    yield
    log.debug("Shutting down AI Agent Controller API...")

app.router.lifespan_context = lifespan