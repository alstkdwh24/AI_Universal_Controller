import logging
import os

from flask import Flask, render_template, request, jsonify, session
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)

_secret_key = os.getenv("FLASK_SECRET_KEY")
if not _secret_key:
    logging.warning(
        "FLASK_SECRET_KEY is not set. Using an insecure default key. "
        "Set this variable before running in production."
    )
    _secret_key = "dev-secret-key-change-in-production"
app.secret_key = _secret_key

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

SYSTEM_PROMPT = (
    "You are a helpful AI assistant. "
    "Answer questions clearly and concisely. "
    "You can communicate in both Korean and English depending on what the user writes."
)


@app.route("/")
def index():
    session.setdefault("history", [])
    return render_template("index.html")


@app.route("/chat", methods=["POST"])
def chat():
    data = request.get_json()
    user_message = (data.get("message") or "").strip()
    if not user_message:
        return jsonify({"error": "메시지를 입력해 주세요."}), 400

    history = session.get("history", [])

    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    messages.extend(history)
    messages.append({"role": "user", "content": user_message})

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=messages,
            temperature=0.7,
            max_tokens=1024,
        )
        assistant_message = response.choices[0].message.content
    except Exception as e:
        logging.exception("OpenAI API error")
        return jsonify({"error": "AI 응답을 가져오는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."}), 500

    history.append({"role": "user", "content": user_message})
    history.append({"role": "assistant", "content": assistant_message})

    # Keep the last 20 exchanges (40 messages) to avoid exceeding token limits
    if len(history) > 40:
        history = history[-40:]

    session["history"] = history

    return jsonify({"reply": assistant_message})


@app.route("/reset", methods=["POST"])
def reset():
    session["history"] = []
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    debug = os.getenv("FLASK_DEBUG", "false").lower() == "true"
    app.run(debug=debug)
