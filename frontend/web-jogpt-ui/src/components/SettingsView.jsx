import { useState } from 'react';

export default function SettingsView({ onBack, user }) {
    const [prompt, setPrompt] = useState(localStorage.getItem('CUSTOM_PROMPT') || '');
    const [saved, setSaved] = useState(false);

    const handleSave = () => {
        localStorage.setItem('CUSTOM_PROMPT', prompt.trim());
        setSaved(true);
        setTimeout(() => setSaved(false), 1800);
    };

    const handleClear = () => {
        setPrompt('');
        localStorage.removeItem('CUSTOM_PROMPT');
        setSaved(false);
    };

    return (
        <div className="sv-wrap">
            <div className="sv-body">

                {/* 계정 섹션 */}
                {user && (
                    <div className="sv-section">
                        <div className="sv-section-title">계정</div>
                        <div className="sv-item">
                            <span className="sv-item-label">사용자</span>
                            <span className="sv-item-value">{user.name}</span>
                        </div>
                        {user.email && (
                            <div className="sv-item">
                                <span className="sv-item-label">이메일</span>
                                <span className="sv-item-value">{user.email}</span>
                            </div>
                        )}
                    </div>
                )}

                {/* 프롬프트 설정 섹션 */}
                <div className="sv-section">
                    <div className="sv-section-title">프롬프트 설정</div>
                    <p className="sv-section-desc">
                        AI에게 항상 적용할 역할이나 지침을 입력하세요. 모든 대화에 자동으로 포함됩니다.
                    </p>
                    <textarea
                        className="sv-prompt-textarea"
                        placeholder={"예시:\n너는 친절한 한국어 어시스턴트야.\n항상 마크다운 형식으로 답변해줘.\n코드는 반드시 설명과 함께 작성해줘."}
                        value={prompt}
                        onChange={(e) => { setPrompt(e.target.value); setSaved(false); }}
                        rows={6}
                    />
                    <div className="sv-prompt-actions">
                        <button className="sv-clear-btn" onClick={handleClear}>
                            초기화
                        </button>
                        <button
                            className={`sv-save-btn ${saved ? 'sv-save-btn--saved' : ''}`}
                            onClick={handleSave}
                        >
                            {saved ? '저장됨 ✓' : '저장'}
                        </button>
                    </div>
                    {prompt && (
                        <div className="sv-prompt-preview">
                            <span className="sv-prompt-preview-label">현재 적용 중</span>
                            <span className="sv-prompt-preview-text">{prompt}</span>
                        </div>
                    )}
                </div>

                {/* 일반 섹션 */}
                <div className="sv-section">
                    <div className="sv-section-title">정보</div>
                    <div className="sv-item">
                        <span className="sv-item-label">버전</span>
                        <span className="sv-item-value">1.0.0</span>
                    </div>
                </div>

            </div>
        </div>
    );
}
