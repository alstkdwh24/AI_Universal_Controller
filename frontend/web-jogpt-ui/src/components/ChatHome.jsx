import DOMPurify from 'dompurify';
import { marked } from 'marked';
import { useEffect, useRef, useState } from 'react';
import CONFIG from '../config/config';
import { fetchWithRefresh } from '../config/tokenRefresh';

const CHUNK_SIZE = 4;
const TICK_MS = 18;

const MODEL_OPTIONS = [
    { label: 'Gemini 3 Flash Image', value: 'gemini-3.1-flash-image-preview' },
    { label: 'GPT-4.5', value: 'gpt-4.5' },

];

export default function ChatHome({ user, isActive, selectedChatKey, onChatLoaded }) {
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const [showChat, setShowChat] = useState(() => {
        const stored = localStorage.getItem('showChat');
        if (stored && !/^\d+$/.test(stored.trim())) {
            localStorage.removeItem('showChat');
            return null;
        }
        return stored;
    });

    const [selectedModel, setSelectedModel] = useState(MODEL_OPTIONS[0].value);
    const textareaRef = useRef(null);
    const chatContainerRef = useRef(null);
    const streamIntervalRef = useRef(null);
    const fileInputRef = useRef(null);
    const [isRecording, setIsRecording] = useState(false);
    const [attachedFiles, setAttachedFiles] = useState([]);
    const recognitionRef = useRef(null);

    useEffect(() => {
        const el = chatContainerRef.current;
        if (!el) return;
        el.scrollTop = el.scrollHeight;
    }, [messages, loading]);

    useEffect(() => {
        return () => clearInterval(streamIntervalRef.current);
    }, []);

    useEffect(() => {
        if (!selectedChatKey) return;
        setLoading(true);
        fetchWithRefresh(`${CONFIG.API_CONTENTS_URL}/contents/chatRoom/${selectedChatKey}/messages`, {
            credentials: 'include'
            // fetch() 요청시 브라우저가 쿠키를 자동으로 함께 전송하도록 하는 옵션입니다.
            // 기본값은 'same-origin' 이라 다른 도메인으로 요청할때 쿠키가 안실려가는데, 'include'로 설정하면 cross-origin 요청에도 쿠키를 포함시킵니다.


        })
            .then(res => res.json())
            .then(data => {
                setMessages(data.map(m => ({ role: m.role, content: m.content, images: [] })));
                localStorage.setItem('showChat', selectedChatKey);
                setShowChat(String(selectedChatKey));
                if (onChatLoaded) onChatLoaded();
            })
            .catch(e => console.error('대화 내역 로드 실패:', e))
            .finally(() => setLoading(false));
    }, [selectedChatKey]);



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
        if (!user) {

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
                await firstSend(myContent);
            } else {
                await continueSend(myContent, showChat);

            }
        } catch (e) {
            console.error(e);
            setMessages(prev => [...prev, { role: 'ai', content: '오류가 발생했습니다.' }]);
        } finally {
            setLoading(false);
        }
    };

    const firstSend = async (myContent) => {
        const res = await fetchWithRefresh(`${CONFIG.API_CONTENTS_URL}/contents/chatRoom`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ myChatContents: myContent })
        });
        if (!res.ok) throw new Error(`채팅방 생성 실패: ${res.status}`);
        const key = await res.text();
        localStorage.setItem('showChat', key);
        setShowChat(key);
        await fetchGptResponse(myContent, selectedModel, key);
    };

    const continueSend = async (myContent, chatKey) => {
        await fetchWithRefresh(`${CONFIG.API_CONTENTS_URL}/contents/myContents`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ myChatContents: myContent, showChatKey: chatKey })
        });
        await fetchGptResponse(myContent, selectedModel, chatKey);
    };

    const fetchGptResponse = async (myContent, model, chatKey) => {
        const customPrompt = localStorage.getItem('CUSTOM_PROMPT')?.trim();
        const res = await fetchWithRefresh(`${CONFIG.API_CONTENTS_URL}/contents/gptContents`, {
            method: 'POST',
            credentials: 'include',

            headers: {
                'Content-Type': 'application/json',
                'X-Model': model,
                ...(customPrompt && { 'X-Custom-Prompt': encodeURIComponent(customPrompt) }),
            },
            body: JSON.stringify({ myChatContents: myContent, showChatKey: chatKey, files: attachedFiles })
        });
        setAttachedFiles([]); // 전송 후 첨부 파일 초기화
        if (!res.ok) {
            setMessages(prev => [...prev, { role: 'ai', content: '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.' }]);
            return;
        }
        const sendBrowserNotification = (content) => {

            if (document.hidden && 'Notification' in window && Notification.permission === 'granted') {

                new Notification('JO-GPT 답변 도착', {

                    body: content.replace(/[#*`>\-]/g, '').slice(0, 80),

                    icon: '/image/blueChatTing.png',

                });

            }

        };
        // 다형성 처리 - 서버가 단순 텍스트만 줄수도 있고, 여러 형태의 것을 줄 수도 있다.
        const gptText = await res.text();
        if (gptText) {
            let fullContent = gptText;

            let images = [];
            try {
                const parsed = JSON.parse(gptText);
                if (parsed.text !== undefined && parsed.images !== undefined) {
                    fullContent = parsed.text;
                    images = parsed.images;
                }
            } catch (_) { }

            // 빈 메시지로 먼저 추가 후 타이핑 애니메이션 시작
            setMessages(prev => [...prev, { role: 'ai', content: '', images, streaming: true }]);

            let i = 0;
            clearInterval(streamIntervalRef.current);
            streamIntervalRef.current = setInterval(() => {
                i = Math.min(i + CHUNK_SIZE, fullContent.length);
                const chunk = fullContent.slice(0, i);
                const done = i >= fullContent.length;
                setMessages(prev => {
                    const next = [...prev];
                    next[next.length - 1] = { ...next[next.length - 1], content: chunk, streaming: !done };
                    return next;
                });
                if (done) {
                    clearInterval(streamIntervalRef.current);
                    sendBrowserNotification(fullContent);
                }
            }, TICK_MS);

            fetchWithRefresh(`${CONFIG.API_CONTENTS_URL}/contents/notifications`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: '"gpt 답변이 등록되었습니다."' })
            });
        }
    };
    // 이 코드의 장점 
    // 사용자 참여 유도 : 앱이 백그라운드에 있거나 다른 탭을 보고 있을 때도 중요한 정보를 실시간으로 전달가능
    // 표준 API를 사용하여 추가 라이브러리 없이 구현 가능
    // React 컴포넌트가 마운트되거나 업데이트될 떼 사이드 이펙트(Side Effect)를 수행하기위해 사용됩니다.
    useEffect(() => {
        // 브라우저가 데스크톱 알림을 지원하는지 확인하는 인터페이스
        if (!('Notification' in window)) {
            // requestPermission() 사용자에게 알림 표시 권한을 명시작으로 요청하는 메서드. 결과 값으로 granted(허용), denied(거부), default(무응답) 중 하나를 반환합니다.
            console.log("이 브라우저는 알림을 지원하지 않습니다.");
            return;
        }

        // 2. 현재 권한 상태 확인 후 요청
        // 권한이 허용되지 않았을 때만 요청
        if (Notification.permission !== 'granted' && Notification.permission !== 'denied') {
            // requestPermission() 메서드를 사용하여 사용자에게 알림 권한을 요청합니다. 사용자가 허용하면 granted, 거부하면 denied, 아무런 응답이 없으면 default가 반환됩니다.
            Notification.requestPermission().then((permission) => {
                if (permission === 'granted') {
                    console.log("알림 권한이 허용되었습니다.");

                    new Notification("알림이 활성화되었습니다!", {
                        body: "이제 새로운 메시지가 도착하면 알림을 받을 수 있습니다.",
                        icon: '/favicon.ico'
                    });
                } else if (permission === 'denied') {
                    console.log("알림 권한이 거부되었습니다. 알림을 받을 수 없습니다.");
                }
            })
        }
    }, []); // 빈 배열을 넣어서 컴포넌트 마운트 시 1회만 실행
    // 파일 이미지 처리:
    const handleFileSelect = async (e) => {

        // 파일 목록 배열 변환
        const files = Array.from(e.target.files);

        // 비동기 병렬 처리 - 모든 파일을 base64로 변환
        const processed = await Promise.all(files.map(async (file) => {
            const base64 = await toBase64(file);
            return {
                name: file.name,
                mimeType: file.type,
                data: base64.split(',')[1], // "data:image/png;base64,..."에서 실제 base64 데이터 부분만 추출
                type: file.type.startsWith('image/') ? 'image' : 'file'
            };
        }));

        // setAttachedFiles는 React에서 useState로 만든 상태를 바꾸는 Setter 함수. 첨부 파일 목록을 이걸로 바꿔줘 라고 요청하는 것입니다.
        // prev => ... 는 Previous의 약자로 수정전 파일 목록을 의미 React는 상태를 업데이트할 때 가장 최신의 상태값을 안전하게 가져오기 위해 이런 함수형 방식을 권장
        // [...prev, ...processed]는 기존 파일 목록(prev)과 새로 처리된 파일 목록(processed)을 합쳐서 새로운 배열을 만드는 코드입니다. 즉, 기존에 첨부된 파일들은 유지하면서 새로 선택한 파일들을 추가하는 형태입니다.
        setAttachedFiles(prev => [...prev, ...processed]);
    };

    const toBase64 = (file) =>
        // 약속이라고 보면 된다.
        new Promise((resolve, reject) => {
            // FileReader는 웹 브라우저에서 제공하는 API로, 파일을 읽어서 다양한 형식으로 변환할 수 있게 해주는 객체입니다. 여기서는 파일을 base64 문자열로 변환하기 위해 사용됩니다.
            const reader = new FileReader();
            // 성공 시
            reader.onload = () => resolve(reader.result);
            // 실패 시
            reader.onerror = (error) => reject(error);
            // 파일 읽기 시작
            reader.readAsDataURL(file);
        });

    const handlePaste = (e) => {
        console.log('paste 이벤트 발생', e.clipboardData?.items);
        const items = Array.from(e.clipboardData?.items || []);
        console.log('items:', items.map(i => `kind=${i.kind} type=${i.type}`));
        const fileItems = items.filter(item => item.kind === 'file');
        if (fileItems.length === 0) return;

        e.preventDefault();

        const files = fileItems.map(item => item.getAsFile()).filter(Boolean);
        if (files.length === 0) return;

        files.forEach(file => {
            toBase64(file).then(base64 => {
                setAttachedFiles(prev => [...prev, {
                    name: file.name || `pasted-image-${Date.now()}.png`,
                    mimeType: file.type || 'image/png',
                    data: base64.split(',')[1],
                    type: file.type.startsWith('image/') ? 'image' : 'file'
                }]);
            });
        });
    };

    // 음성인식 처리:
    const handleVoice = () => {
        const SpeechRecofnition = window.SpeechRecognition || window.webkitSpeechRecognition;

        if (!SpeechRecofnition) {
            alert("이 브라우저는 음성 인식을 지원하지 않습니다.");
            return;
        }

        if (isRecording) {
            recognitionRef.current?.stop();
            setIsRecording(false);
            return;
        }

        const recognition = new SpeechRecognition();
        recognition.lang = 'ko-KR';
        recognition.continuous = false;
        recognition.interimResults = false;
        recognition.onresult = (e) => setInput(prev => prev + e.results[0][0].transcript);
        recognition.onerror = (e) => console.error("음성 인식 오류:", e);
        recognition.start();
        recognitionRef.current = recognition;
        setIsRecording(true);

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
                                {msg.content && (
                                    <div
                                        id="realGeminiContent"
                                        className={msg.streaming ? 'streaming-cursor' : ''}

                                        dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(marked.parse(msg.content)) }}
                                    />
                                )}
                                {msg.images && msg.images.length > 0 && (
                                    <div className="gpt-image-wrap">
                                        {msg.images.map((img, idx) => (
                                            <img
                                                key={idx}
                                                src={`data:${img.mimeType};base64,${img.data}`}
                                                alt="AI 생성 이미지"
                                                className="gpt-generated-image"
                                            />
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>
                    )
                ))}
                {loading && (
                    <div className="loading" id="start-loading">
                        <div id="geminiContent-geminiContent">
                            <div className="gemini-loader">
                                <div className="gemini-ring"></div>
                                <span className="gemini-star">⬥</span>
                            </div>
                        </div>
                    </div>
                )}
            </div>
            <div className="realBox">
                {messages.length === 0 && !loading && (
                    <div className="realBoxFont">
                        안녕하세요 <span id="userName">{user ? user.nickname : '사용자'}</span>님
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
                            onPaste={handlePaste}
                            autoFocus
                        />
                    </label>
                    <div className="search-div">
                        <div className="left-tools">
                            <button className="search-btn" onClick={() => fileInputRef.current.click()}>
                                <i className="fa fa-paperclip"></i>
                            </button>
                            <input
                                ref={fileInputRef}
                                type="file"
                                multiple
                                accept="image/*, audio/*, .pdf"
                                style={{ display: 'none' }}
                                onChange={handleFileSelect}
                            />
                            <button className='search-btn' onClick={handleVoice}>
                                <i className={`fa ${isRecording ? 'fa-stop' : 'fa-microphone'}`}
                                    style={{ color: isRecording ? '#e74c3c' : '#3A6BF5' }} />
                            </button>
                            {attachedFiles.length > 0 && (
                                <div className='attached-preview'>
                                    {attachedFiles.map((file, idx) => (
                                        <span key={idx} className='attached-chip'>
                                            {file.type === 'image'
                                                ? <img src={`data:${file.mimeType};base64,${file.data}`} alt={file.name} className="thumb" />
                                                : <i className="fa fa-file" />}
                                            <button onClick={() => setAttachedFiles(prev => prev.filter((_, j) => j !== idx))}>×</button>
                                        </span>
                                    ))}
                                </div>)}
                        </div>

                        <div className="middle-tools"></div>
                        <div className="search-result">
                            <div className="select-model-wrapper">
                                <select
                                    className="select-model"
                                    value={selectedModel}
                                    onChange={e => setSelectedModel(e.target.value)}
                                >
                                    {MODEL_OPTIONS.map(opt => (
                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                    ))}
                                </select>
                            </div>
                            <button
                                className={messages.length > 0 ? 'search-real-hide' : 'search-btn search-real'}
                                onClick={handleSend}
                            >
                                <i className="fa fa-arrow-up"></i>
                            </button>
                        </div>
                    </div>
                </div>
                <p style={{ fontFamily: 'normal', fontSize: '12px', color: '#666' }}>Gemini는 AI이며 인물 등에 관한 정보 제공 시 실수를 할 수 있습니다. 이 모델은 구글의 모델을 기반으로 합니다.</p>

            </div>

        </div >
    );
}