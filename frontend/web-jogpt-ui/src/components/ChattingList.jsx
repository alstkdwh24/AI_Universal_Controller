import { useState, useEffect } from 'react';
import CONFIG from '../config/config';

export default function ChattingList({ onSelectChat }) {
    const [chatList, setChatList] = useState([]);
    const [search, setSearch] = useState('');

    useEffect(() => {
        loadChattingList();
    }, []);

    const loadChattingList = async () => {
        const token = localStorage.getItem('ACCESS_TOKEN');
        if (token) {
            try {
                const res = await fetch(`${CONFIG.API_CONTENTS_URL}/contents/chattingList`, {
                    method: 'GET',
                    headers: { 'Authorization': 'Bearer ' + token }
                });
                if (res.ok) {
                    const data = await res.json();
                    setChatList(data);
                }
            } catch (e) {
                console.error('채팅 목록 로드 실패:', e);
            }
        } else {
            setChatList([
                { showMyChatContents: '내 물음', showChatRegistration: new Date().toISOString(), mock: true },
                { showMyChatContents: '내 물음', showChatRegistration: new Date().toISOString(), mock: true },
                { showMyChatContents: '내 물음', showChatRegistration: new Date().toISOString(), mock: true },
            ]);
        }
    };

    const handleDelete = async (showChatKey) => {
        const token = localStorage.getItem('ACCESS_TOKEN');
        if (!token || !showChatKey) return;
        try {
            const res = await fetch(`${CONFIG.API_CONTENTS_URL}/contents/chatRoom/${showChatKey}`, {
                method: 'DELETE',
                headers: { 'Authorization': 'Bearer ' + token }
            });
            if (res.ok) {
                setChatList(prev => prev.filter(item => item.showChatKey !== showChatKey));
            }
        } catch (e) {
            console.error('채팅 삭제 실패:', e);
        }
    };

    const formatTime = (dateStr) => {
        const d = new Date(dateStr);
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        return `${month}/${day} ${hours}:${minutes}`;
    };

    const handleSearchInput = (e) => {
        setSearch(e.target.value);
        e.target.style.height = 'auto';
        e.target.style.height = e.target.scrollHeight + 'px';
    };

    const filtered = chatList.filter(item =>
        !search.trim() || (item.showMyChatContents || '').includes(search.trim())
    );

    return (
        <div className="chatting-container">
            <div id="title-chatting">
                <div id="chatting-icon-container">
                    <img src="/image/blueChatTing.png" alt="Chat Icon" id="chatting-icon" />
                    <h1>채팅</h1>
                </div>
            </div>
            <div id="search-chatting">
                <label id="search-input">
                    <i className="fa fa-search" id="search-icon"></i>
                    <textarea
                        placeholder="대화 내용 검색"
                        id="search-chatting-input"
                        rows={1}
                        value={search}
                        onChange={handleSearchInput}
                    />
                </label>
            </div>
            <div id="chatting-list">
                {filtered.map((item, i) => (
                    <div className="chatting-list-container" key={item.showChatKey ?? i} onClick={() => !item.mock && onSelectChat && onSelectChat(item.showChatKey)} style={{ cursor: item.mock ? 'default' : 'pointer' }}>
                        <div className="chatting-list-icon-wrap">
                            <img
                                className="chatting-icon"
                                src={item.mock ? '/image/Gemini_chat.png' : '/image/blueChatTing.png'}
                                alt="채팅 아이콘"
                            />
                        </div>
                        <div className="chatting-list-word">
                            <span className="chatting-list-title">
                                {item.showMyChatContents || '(내용 없음)'}
                            </span>
                            <div className="chatting-list-time">
                                <i className="fa fa-clock-o chatting-list-time-icon"></i>
                                <span className="chatting-list-time-text">
                                    {item.mock ? '오전 10:00' : formatTime(item.showChatRegistration)}
                                </span>
                            </div>
                        </div>
                        {!item.mock && (
                            <button
                                className="chatting-delete-btn"
                                onClick={(e) => { e.stopPropagation(); handleDelete(item.showChatKey); }}
                                title="삭제"
                            >
                                <i className="fa fa-trash"></i>
                            </button>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
}
