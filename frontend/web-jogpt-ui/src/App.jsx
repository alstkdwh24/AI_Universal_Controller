import { useState, useEffect } from 'react';
import SideBar from './components/SideBar';
import TopBar from './components/TopBar';
import ChatHome from './components/ChatHome';
import ChattingList from './components/ChattingList';
import LoginModal from './components/LoginModal';
import AlertModal from './components/AlertModal';
import CONFIG from './config/config';

import './styles/GPT-Home.css';
import './styles/modelJoin.css';
import './styles/toastMessage.css';
import './styles/chatTing.css';
import './styles/chattingHome.css';
import './styles/alertModal.css';
import './styles/menuBar.css';

export default function App() {
    const [user, setUser] = useState(null);
    const [showLogin, setShowLogin] = useState(false);
    const [toast, setToast] = useState('');
    const [sidebarActive, setSidebarActive] = useState(false);
    const [currentView, setCurrentView] = useState('home');
    const [showAlert, setShowAlert] = useState(false);

    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        if (token) {
            localStorage.setItem('ACCESS_TOKEN', token);
            window.history.replaceState({}, '', '/');
            showToastMessage('성공적으로 로그인 되었습니다.');
        }
        if (urlParams.has('error')) {
            setShowLogin(true);
        }
        fetchMyInfo();
    }, []);

    const fetchMyInfo = async () => {
        const token = localStorage.getItem('ACCESS_TOKEN');
        if (!token) return;
        try {
            const res = await fetch(`${CONFIG.API_BASE_URL}/login/myInfo`, {
                headers: { 'Authorization': 'Bearer ' + token }
            });
            if (res.ok) {
                const data = await res.json();
                setUser(data);
            } else if (res.status === 401) {
                localStorage.removeItem('ACCESS_TOKEN');
                showToastMessage('로그인이 만료되었습니다. 다시 로그인해주세요.');
            }
        } catch (e) {
            console.error('사용자 정보 로드 실패:', e);
        }
    };

    const handleLogout = async () => {
        try {
            await fetch(`${CONFIG.API_BASE_URL}/login/logout`, {
                method: 'GET',
                credentials: 'include'
            });
        } catch (e) {
            console.error('로그아웃 요청 실패:', e);
        }
        localStorage.removeItem('ACCESS_TOKEN');
        localStorage.removeItem('showChat');
        setUser(null);
        showToastMessage('로그아웃 되었습니다.');
    };

    const showToastMessage = (msg) => {
        setToast(msg);
        setTimeout(() => setToast(''), 2500);
    };

    return (
        <>
            <SideBar
                isActive={sidebarActive}
                onMenuClick={() => setSidebarActive(prev => !prev)}
                onHomeClick={() => setCurrentView('home')}
                onChatClick={() => setCurrentView('chat')}
                onBellClick={() => setShowAlert(true)}
            />
            <div className={sidebarActive ? 'contentActive' : 'content'}>
                <TopBar
                    user={user}
                    isActive={sidebarActive}
                    onLoginClick={() => setShowLogin(true)}
                    onLogout={handleLogout}
                />
                {currentView === 'home'
                    ? <ChatHome user={user} isActive={sidebarActive} />
                    : <div style={{ display: 'flex', flex: 1, justifyContent: 'center', overflow: 'auto' }}>
                        <ChattingList />
                      </div>
                }
            </div>

            {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
            {showAlert && <AlertModal onClose={() => setShowAlert(false)} />}
            {toast && <div id="toast" className="show">{toast}</div>}
        </>
    );
}