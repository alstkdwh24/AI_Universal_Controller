# AI 챗봇 서비스

OpenAI GPT를 활용한 웹 기반 챗봇 서비스입니다.

## 기능

- 🤖 GPT-4o-mini 모델 기반 AI 대화
- 💬 대화 기록 유지 (최대 20회 교환)
- 🔄 대화 초기화 기능
- 📱 모바일 반응형 UI
- ⌨️ Enter 전송 / Shift+Enter 줄바꿈

## 설치 및 실행

### 사전 요구 사항

- Python 3.9 이상
- OpenAI API 키

### 설치

```bash
# 의존성 설치
pip install -r requirements.txt

# 환경 변수 설정
cp .env.example .env
# .env 파일을 열어 OPENAI_API_KEY를 입력하세요
```

### 실행

```bash
python app.py
```

브라우저에서 `http://localhost:5000` 으로 접속합니다.

## 환경 변수

| 변수명 | 설명 | 필수 |
|--------|------|------|
| `OPENAI_API_KEY` | OpenAI API 키 | ✅ |
| `FLASK_SECRET_KEY` | Flask 세션 암호화 키 | 권장 |
| `FLASK_DEBUG` | 디버그 모드 활성화 (`true`/`false`, 기본값: `false`) | 선택 |

## 프로젝트 구조

```
AI_Universal_Controller/
├── app.py               # Flask 애플리케이션 (백엔드)
├── requirements.txt     # Python 의존성
├── .env.example         # 환경 변수 예시
├── templates/
│   └── index.html       # 챗봇 UI
└── static/
    ├── css/
    │   └── style.css    # 스타일
    └── js/
        └── chat.js      # 프론트엔드 로직
```
