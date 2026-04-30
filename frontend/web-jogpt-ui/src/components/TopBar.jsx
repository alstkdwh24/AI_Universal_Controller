export default function TopBar({ user, isActive, onLoginClick, onLogout }) {
    return (
        <div className={isActive ? 'topBarActive' : 'topBar'}>
            <div className="topBar-left">
                <h2>JOGPT</h2>
            </div>
            <div className="topBar-middle"></div>
            <div className="topBar-right">
                {!user ? (
                    <div id="authButtons" className="topBar-rightRight">
                        <button className="buttonWhite" onClick={onLoginClick}>로그인</button>
                        <button className="buttonBlack" onClick={onLoginClick}>회원가입</button>
                    </div>
                ) : (
                    <div id="userProfile" className="topBar-rightRight">
                        <span id="userNameDisplay">{user.name}</span>님
                        <button className="buttonWhite" style={{ marginLeft: '10px' }} onClick={onLogout}>
                            로그아웃
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}