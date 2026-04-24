# dto 예시
from pydantic import BaseModel

class ChatRequest(BaseModel):
    user_id: int
    message: str

class ChatResponse(BaseModel):
    answer: str