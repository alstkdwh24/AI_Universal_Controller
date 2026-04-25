# 메인 py파일

# 실행 명령어 python main.py (이 명령은 파이썬 인터프리터가 main.py 전체를 실행합니다.
# 따라서 if __name__ == "__main__": 블록 안의 코드가 실행됩니다. 이 블록은 모듈이 직접 실행될 때만 실행되도록 보장합니다. config.yml 파일에서 읽은 host와 port 설정을 사용하여 FastAPI 서버가 시작됩니다. 또한, 앱의 라우터가 등록되고, 서버가 시작될 때와 종료될 때 로그 메시지가 출력됩니다. uvicorn.run() 함수는 FastAPI 앱을 실행하는 데 사용되며, host와 port를 지정하여 서버가 어디에서 실행될지 결정합니다.

# Unicorn main:app --reload 
# 이 명령은 터미널에서 직접 uvicorn을 실행합니다.
# 이때 uvicorn은 단순히 main.py의 app객체만 가져와서 실행합니다.
# main.py의 if __name__ == "__main__": 블록은 실행되지 않습니다. 따라서 config.yml에서 설정을 읽는 부분과 로그 메시지를 출력하는 부분이 실행되지 않습니다. 대신, uvicorn이 기본적으로 localhost의 8000번 포트에서 서버를 시작합니다. --reload 옵션은 코드 변경 시 자동으로 서버를 재시작하도록 합니다.


from contextlib import asynccontextmanager
from fastapi import FastAPI
import logging
import yaml # YAML 파일을 읽기 위한 라이브러리
import os

# __file__ : 현재 실행중인 파이썬 파일(main.py)의 경로(전체 경로)를 의미
# os.path.abspath(__file__) : main.py의 절대 경로를 반환
# os.path.dirname() : 그 경로에서 "디렉터리 (폴더) 경로"만 추툴합니다. 즉, main.py가 위치한 폴더의 경로를 얻을 수 있습니다.
# 결과적으로 base_dir에는 main.py가 위치한 폴더의 경로가 들어갑니다.
# os.path.join() : base_dirt과 "config.yml"을 합쳐서, main.py가 위치한 폴더의 config.yml 파일의 "정확한 전체 경로"를 만듭니다.

base_dir = os.path.dirname(os.path.abspath(__file__))
config_path = os.path.join(base_dir, "config.yml")

# config.yml 파일에서 서버 설정을 읽어오는 부분
with open(config_path, "r", encoding="utf-8") as f:
    config = yaml.safe_load(f)

host = config["server"]["host"]
port = config["server"]["port"]
log = logging.getLogger(__name__)

from core.agent_router import agent_router
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

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=host,
        port=port,
        reload=True
    )