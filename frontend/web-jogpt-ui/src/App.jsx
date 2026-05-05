import { useEffect, useState } from 'react';
import AlertModal from './components/AlertModal';
import ChatHome from './components/ChatHome';
import ChattingList from './components/ChattingList';
import LoginModal from './components/LoginModal';
import LoginView from './components/LoginView';
import NicknameSetupModal from './components/NicknameSetupModal';
import SettingsView from './components/SettingsView';
import SideBar from './components/SideBar';
import TopBar from './components/TopBar';
import CONFIG from './config/config';

import './styles/alertModal.css';
import './styles/chatTing.css';
import './styles/chattingHome.css';
import './styles/GPT-Home.css';
import './styles/loginView.css';
import './styles/menuBar.css';
import './styles/modelJoin.css';
import './styles/settingsView.css';
import './styles/toastMessage.css';


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
    const [pendingNicknameSetup, setPendingNicknameSetup] = useState(false);

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
        // needsNickname은 UrlParameter로 받아도 무관 (민감정보 아님)
        if (urlParams.get('needsNickname') === 'true') {
            setSocialNickname(urlParams.get('socialNickname') || '');
            setPendingNicknameSetup(true);

        }
        if (urlParams.has('error')) {
            setShowLogin(true);
        }
        window.history.replaceState({}, '', '/'); // URL에서 쿼리 제거

        fetchMyInfo();
    }, []);

    // fetchMyInfo로 user가 세팅된 후 닉네임 모달 표시
    useEffect(() => {
        if (user && pendingNicknameSetup) {
            setShowNicknameSetup(true);
            setPendingNicknameSetup(false);
        }
    }, [user, pendingNicknameSetup]);

    const fetchMyInfo = async () => {

        try {
            const res = await fetch(`${CONFIG.API_BASE_URL}/login/myInfo`, {
                credentials: 'include',

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
                        <ChattingList onChatSelect={(key) => { setSelectedChatKey(key); setCurrentView('home'); }} user={user} />

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