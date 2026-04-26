import logging
import os

from dotenv import load_dotenv
from google import genai
from google.genai import types
# 환경변수 설정과 로깅 설정
load_dotenv(override=True)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

client = genai.Client(api_key=os.getenv("GOOGLE_API_KEY"))

def perform_google_web_search(query: str, *args, **kwargs) -> list[str]:
    try:
        # Google 검색을 사용하여 최신 정보를 찾아줍니다.

        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=f"Google 검색을 반드시 사용해서 최신 정보를 찾아줘: {query}",
            config=types.GenerateContentConfig(
                tools=[types.Tool(google_search=types.GoogleSearch())]
            )
        )
        answer_text = response.text if response.text else ""
        logger.info(f"Gemini 응답 텍스트: {answer_text}")

   
        return answer_text
    except Exception as e:
        logger.error(f"Gemini 검색 오류: {e}")
        return []