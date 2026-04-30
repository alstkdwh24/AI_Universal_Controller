import DOMPurify from 'dompurify';
import { marked } from 'marked';
import { useEffect, useRef, useState } from 'react';
import CONFIG from '../config/config';

export default function ChatHome({ user, isActive }) {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [showChat, setShowChat] = useState(localStorage.getItem('showChat'));
    const textareaRef = useRef(null);
    const chatContainerRef = useRef(null);

    useEffect(() => {
        const el = chatContainerRef.current;
        if (!el) return;
        el.scrollTop = el.scrollHeight;
    }, [messages, loading]);

    const getValidToken = () => {
        const raw = localStorage.getItem('ACCESS_TOKEN');
        const token = (raw || '').trim();
        if (!token || token.split('.').length !== 3) {
            localStorage.removeItem('ACCESS_TOKEN');
            localStorage.removeItem('showChat');
            return null;
        }
        return token;
    };

    const handleInput = (e) => {
        setInput(e.target.value);
        e.target.style.height = 'auto';
        e.target.style.height = e.target.scrollHeight + 'px';
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    const handleSend = async () => {
        const query = input.trim();
        if (!query || loading) return;
        const token = getValidToken();
        if (!token) {
            alert('로그인 후 이용해주세요');
            return;
        }

        const myContent = query;
        setMessages(prev => [...prev, { role: 'user', content: myContent }]);
        setInput('');
        setLoading(true);
        if (textareaRef.current) textareaRef.current.style.height = 'auto';

        try {
            if (!showChat) {
                await firstSend(token, myContent);
            } else {
                await continueSend(token, myContent, showChat);
            }
        } catch (e) {
            console.error(e);
            setMessages(prev => [...prev, { role: 'ai', content: '오류가 발생했습니다.' }]);
        } finally {
            setLoading(false);
        }
    };

    const firstSend = async (token, myContent) => {
        const res = await fetch(`${CONFIG.API_CONTENTS_URL}/contents/chatRoom`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
            body: JSON.stringify({ myChatContents: myContent })
        });
        const key = await res.text();
        localStorage.setItem('showChat', key);
        setShowChat(key);
        await fetchGptResponse(token, myContent);
    };

    const continueSend = async (token, myContent, chatKey) => {
        await fetch(`${CONFIG.API_CONTENTS_URL}/contents/myContents`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
            body: JSON.stringify({ myChatContents: myContent, showChatKey: chatKey })
        });
        await fetchGptResponse(token, myContent);
    };

    const fetchGptResponse = async (token, myContent) => {
        const customPrompt = localStorage.getItem('CUSTOM_PROMPT')?.trim();


        const res = await fetch(`${CONFIG.API_CONTENTS_URL}/contents/gptContents`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json', ...(customPrompt && { 'X-Custom-Prompt': encodeURIComponent(customPrompt) }) },
            body: JSON.stringify({ myChatContents: myContent })
        });
        const gptText = await res.text();  // 텍스트로 직접 받기
        if (gptText) {
            setMessages(prev => [...prev, { role: 'ai', content: gptText }]);
            await fetch(`${CONFIG.API_CONTENTS_URL}/contents/notifications`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: '"gpt 답변이 등록되었습니다."' })
            });
        }
    };

    return (
        <div className={`${isActive ? 'realContentActive' : 'realContent'}${messages.length > 0 ? ' chat-active' : ''}`}>
            <div ref={chatContainerRef} className={`my-gemini-talk${messages.length > 0 ? ' my-gemini-talk-active' : ''}`}>
                {messages.map((msg, i) => (
                    msg.role === 'user' ? (
                        <div key={i} className="myContents">
                            <div id="myContent-myContent">
                                <div id="realMyContent">{msg.content}</div>
                            </div>
                        </div>
                    ) : (
                        <div key={i} className="gptContents">
                            <div id="geminiContent-geminiContent">
                                <div
                                    id="realGeminiContent"
                                    dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(marked.parse(msg.content)) }}
                                />
                            </div>
                        </div>
                    )
                ))}
                {loading && (
                    <div className="loading" id="start-loading">
                        <div id="geminiContent-geminiContent">
                            <div className="loading-dots">
                                <span></span><span></span><span></span>
                            </div>
                        </div>
                    </div>
                )}
            </div>
            <div className="realBox">
                {messages.length === 0 && (
                    <div className="realBoxFont">
                        안녕하세요 <span id="userName">{user ? user.name : '사용자'}</span>님
                    </div>
                )}
                <div className="input-wrapper">
                    <label>
                        <textarea
                            ref={textareaRef}
                            className="fake-input"
                            placeholder="어떤것이든 물어보세요"
                            value={input}
                            onChange={handleInput}
                            onKeyDown={handleKeyDown}
                            autoFocus
                        />
                    </label>
                    <div className="search-div">
                        <div className="left-tools">
                            <button className="search-btn">
                                <i className="fa fa-paperclip"></i>
                            </button>
                        </div>
                        <div className="middle-tools"></div>
                        <div className="search-result">
                            <div className="select-model-wrapper">
                                <select className="select-model">
                                    <option>Gemini 3 Flash</option>
                                    <option>GPT-4.5</option>
                                </select>
                            </div>
                            <button
                                className={`search-btn ${messages.length > 0 ? 'search-real-hide' : 'search-real'}`}
                                onClick={handleSend}
                            >
                                <i className="fa fa-arrow-up"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}