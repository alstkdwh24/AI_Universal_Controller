import { useState, useEffect } from 'react';
import SideBar from './components/SideBar';
import TopBar from './components/TopBar';
import ChatHome from './components/ChatHome';
import ChattingList from './components/ChattingList';
import SettingsView from './components/SettingsView';
import LoginModal from './components/LoginModal';
import LoginView from './components/LoginView';
import AlertModal from './components/AlertModal';
import NicknameSetupModal from './components/NicknameSetupModal';
import CONFIG from './config/config';

import './styles/GPT-Home.css';
import './styles/modelJoin.css';
import './styles/toastMessage.css';
import './styles/chatTing.css';
import './styles/chattingHome.css';
import './styles/alertModal.css';
import './styles/menuBar.css';
import './styles/settingsView.css';
import './styles/loginView.css';

export default function App() {
    const [user, setUser] = useState(null);
    const [showLogin, setShowLogin] = useState(false);
    const [toast, setToast] = useState('');
    const [sidebarActive, setSidebarActive] = useState(false);
    const [currentView, setCurrentView] = useState('home');
    const [showAlert, setShowAlert] = useState(false);
    const [showLoginView, setShowLoginView] = useState(false);
    const [selectedChatKey, setSelectedChatKey] = useState(null);
    const [showNicknameSetup, setShowNicknameSetup] = useState(false);
    const [socialNickname, setSocialNickname] = useState('');

    /* 모바일 → 화면 전환 / PC → 기존 모달 */
    const handleLoginClick = () => {
        if (window.innerWidth <= 768) {
            setShowLoginView(true);
        } else {
            setShowLogin(true);
        }
    };

    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search);
        const token = urlParams.get('token');
        if (token) {
            localStorage.setItem('ACCESS_TOKEN', token);
            window.history.replaceState({}, '', '/');
            if (urlParams.get('needsNickname') === 'true') {
                setSocialNickname(urlParams.get('socialNickname') || '');
                setShowNicknameSetup(true);
            } else {
                showToastMessage('성공적으로 로그인 되었습니다.');
            }
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

    const renderContent = () => {
        switch (currentView) {
            case 'chat':
                return (
                    <div style={{ display: 'flex', flex: 1, justifyContent: 'center', overflow: 'auto' }}>
                        <ChattingList onChatSelect={(key) => { setSelectedChatKey(key); setCurrentView('home'); }} />
                    </div>
                );
            case 'settings':
                return (
                    <SettingsView
                        user={user}
                        onBack={() => setCurrentView('home')}
                    />
                );
            default:
                return <ChatHome user={user} isActive={sidebarActive} selectedChatKey={selectedChatKey} onChatLoaded={() => setSelectedChatKey(null)} />;
        }
    };

    return (
        <>
            <SideBar
                isActive={sidebarActive}
                onMenuClick={() => setSidebarActive(prev => !prev)}
                onHomeClick={() => setCurrentView('home')}
                onChatClick={() => setCurrentView('chat')}
                onBellClick={() => setShowAlert(true)}
                onSettingsClick={() => setCurrentView('settings')}
            />
            <div className={sidebarActive ? 'contentActive' : 'content'}>
                <TopBar
                    user={user}
                    isActive={sidebarActive}
                    onLoginClick={handleLoginClick}
                    onLogout={handleLogout}
                    isSettings={currentView === 'settings'}
                    onSettingsBack={() => setCurrentView('home')}
                />
                {renderContent()}
            </div>

            {showLogin && <LoginModal onClose={() => setShowLogin(false)} />}
            {showLoginView && <LoginView onClose={() => setShowLoginView(false)} />}
            {showAlert && <AlertModal onClose={() => setShowAlert(false)} />}
            {showNicknameSetup && (
                <NicknameSetupModal
                    socialNickname={socialNickname}
                    onComplete={(nick) => {
                        setShowNicknameSetup(false);
                        setSocialNickname('');
                        setUser(prev => prev ? { ...prev, nickname: nick } : prev);
                        fetchMyInfo();
                        showToastMessage('닉네임이 설정되었습니다. 환영합니다!');
                    }}
                />
            )}
            {toast && <div id="toast" className="show">{toast}</div>}
        </>
    );
}