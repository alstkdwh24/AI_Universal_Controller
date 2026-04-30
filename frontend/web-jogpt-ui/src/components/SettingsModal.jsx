export default function SettingsModal({ onClose, user }) {
    return (
        <div className="settings-overlay" onClick={onClose}>
            <div className="settings-modal" onClick={(e) => e.stopPropagation()}>
                <div className="settings-header">
                    <h2 className="settings-title">설정</h2>
                    <button className="settings-close-btn" onClick={onClose}>
                        <i className="fa fa-times"></i>
                    </button>
                </div>

                <div className="settings-body">
                    {user && (
                        <div className="settings-section">
                            <div className="settings-section-label">계정</div>
                            <div className="settings-item">
                                <span className="settings-item-name">사용자</span>
                                <span className="settings-item-value">{user.name}</span>
                            </div>
                            <div className="settings-item">
                                <span className="settings-item-name">이메일</span>
                                <span className="settings-item-value">{user.email ?? '-'}</span>
                            </div>
                        </div>
                    )}

                    <div className="settings-section">
                        <div className="settings-section-label">일반</div>
                        <div className="settings-item">
                            <span className="settings-item-name">테마</span>
                            <span className="settings-item-value">라이트</span>
                        </div>
                        <div className="settings-item">
                            <span className="settings-item-name">언어</span>
                            <span className="settings-item-value">한국어</span>
                        </div>
                    </div>

                    <div className="settings-section">
                        <div className="settings-section-label">정보</div>
                        <div className="settings-item">
                            <span className="settings-item-name">버전</span>
                            <span className="settings-item-value">1.0.0</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}