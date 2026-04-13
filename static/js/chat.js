const chatBox   = document.getElementById('chatBox');
const chatForm  = document.getElementById('chatForm');
const userInput = document.getElementById('userInput');
const sendBtn   = document.getElementById('sendBtn');
const resetBtn  = document.getElementById('resetBtn');

/* ── Auto-resize textarea ─────────────────────────────────── */
userInput.addEventListener('input', () => {
  userInput.style.height = 'auto';
  userInput.style.height = `${userInput.scrollHeight}px`;
});

/* ── Submit on Enter (Shift+Enter = new line) ─────────────── */
userInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
});

chatForm.addEventListener('submit', (e) => {
  e.preventDefault();
  sendMessage();
});

/* ── Reset conversation ───────────────────────────────────── */
resetBtn.addEventListener('click', async () => {
  try {
    const res = await fetch('/reset', { method: 'POST' });
    if (!res.ok) throw new Error('Reset failed');
    chatBox.innerHTML = '';
    appendMessage('assistant', '대화가 초기화되었습니다. 다시 시작할게요 😊');
  } catch (err) {
    console.error('Reset error:', err);
    appendMessage('error', '대화 초기화 중 오류가 발생했습니다.');
  }
});

/* ── Core send logic ──────────────────────────────────────── */
async function sendMessage() {
  const text = userInput.value.trim();
  if (!text) return;

  userInput.value = '';
  userInput.style.height = 'auto';
  setLoading(true);

  appendMessage('user', text);

  const typingEl = appendTyping();

  try {
    const res = await fetch('/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text }),
    });

    const data = await res.json();
    typingEl.remove();

    if (res.ok) {
      appendMessage('assistant', data.reply);
    } else {
      appendMessage('error', data.error || '오류가 발생했습니다.');
    }
  } catch (err) {
    console.error('Chat error:', err);
    typingEl.remove();
    appendMessage('error', '서버와 통신 중 오류가 발생했습니다.');
  } finally {
    setLoading(false);
  }
}

/* ── DOM helpers ──────────────────────────────────────────── */
function appendMessage(role, text) {
  const msgEl = document.createElement('div');
  msgEl.className = `message ${role}`;

  const bubble = document.createElement('div');
  bubble.className = 'bubble';
  bubble.textContent = text;

  msgEl.appendChild(bubble);
  chatBox.appendChild(msgEl);
  scrollToBottom();
  return msgEl;
}

function appendTyping() {
  const msgEl = document.createElement('div');
  msgEl.className = 'message assistant typing-indicator';

  const bubble = document.createElement('div');
  bubble.className = 'bubble';
  bubble.innerHTML = '<span class="dot"></span><span class="dot"></span><span class="dot"></span>';

  msgEl.appendChild(bubble);
  chatBox.appendChild(msgEl);
  scrollToBottom();
  return msgEl;
}

function scrollToBottom() {
  chatBox.scrollTop = chatBox.scrollHeight;
}

function setLoading(isLoading) {
  sendBtn.disabled = isLoading;
  userInput.disabled = isLoading;
  if (!isLoading) userInput.focus();
}
