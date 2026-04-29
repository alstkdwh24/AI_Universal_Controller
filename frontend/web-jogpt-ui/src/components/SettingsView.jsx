import { useEffect, useState } from 'react';
import CONFIG from '../config/config';

export default function SettingsView({ onBack, user }) {
    const [prompts, setPrompts] = useState([]);
    const [newName, setNewName] = useState('');
    const [newContent, setNewContent] = useState('');
    const [saved, setSaved] = useState(false);

    const getToken = () => localStorage.getItem('ACCESS_TOKEN');

    useEffect(() => {
        loadPrompts();
    }, []);

    const loadPrompts = async () => {
        const token = getToken();
        if (!token) return;
        try {
            const res = await fetch(`${CONFIG.API_CONTENTS_URL}/contents/myPrompts`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (res.ok) {
                const data = await res.json();
                setPrompts(data);
            }
        } catch (e) {
            console.error('프롬프트 목록 로드 실패:', e);
        }
    };

    const handleSave = async () => {
        if (!newName.trim() || !newContent.trim()) return;
        const token = getToken();
        try {
            const res = await fetch(`${CONFIG.API_CONTENTS_URL}/contents/myPrompts`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
                body: JSON.stringify({ promptName: newName.trim(), promptContent: newContent.trim() })
            });
            if (!res.ok) {
                console.error('프롬프트 저장 실패:', res.status);
                return;
            }
            setNewName('');
            setNewContent('');
            setSaved(true);
            setTimeout(() => setSaved(false), 1800);
            await loadPrompts();
        } catch (e) {
            console.error('프롬프트 저장 실패:', e);
        }
    };

    const handleDelete = async (promptKey) => {
        const token = getToken();
        try {
            await fetch(`${CONFIG.API_CONTENTS_URL}/contents/myPrompts/${promptKey}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            });
            await loadPrompts();
        } catch (e) {
            console.error('프롬프트 삭제 실패:', e);
        }
    };

    const handleActivate = async (promptKey) => {
        const token = getToken();
        try {
            await fetch(`${CONFIG.API_CONTENTS_URL}/contents/myPrompts/${promptKey}/activate`, {
                method: 'PATCH',
                headers: { 'Authorization': `Bearer ${token}` }
            });
            await loadPrompts();
        } catch (e) {
            console.error('프롬프트 활성화 실패:', e);
        }
    };

    return (
        <div className="sv-wrap">
            <div className="sv-header">
                <button className="sv-back-btn" onClick={onBack}>
                    <i className="fa fa-arrow-left"></i>
                </button>
                <h2 className="sv-header-title">설정</h2>
            </div>

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
                        AI에게 적용할 프롬프트를 추가하고, 원하는 것을 선택하세요. 선택된 프롬프트가 모든 대화에 자동으로 적용됩니다.
                    </p>

                    {/* 새 프롬프트 추가 */}
                    <input
                        className="sv-prompt-name-input"
                        placeholder="프롬프트 이름 (예: 코드 리뷰어)"
                        value={newName}
                        onChange={(e) => { setNewName(e.target.value); setSaved(false); }}
                    />
                    <textarea
                        className="sv-prompt-textarea"
                        placeholder={"예시:\n너는 친절한 한국어 어시스턴트야.\n항상 마크다운 형식으로 답변해줘.\n코드는 반드시 설명과 함께 작성해줘."}
                        value={newContent}
                        onChange={(e) => { setNewContent(e.target.value); setSaved(false); }}
                        rows={5}
                    />
                    <div className="sv-prompt-actions">
                        <button
                            className={`sv-save-btn ${saved ? 'sv-save-btn--saved' : ''}`}
                            onClick={handleSave}
                        >
                            {saved ? '저장됨 ✓' : '추가'}
                        </button>
                    </div>

                    {/* 저장된 프롬프트 목록 */}
                    {prompts.length > 0 && (
                        <div className="sv-prompt-list">
                            {prompts.map(p => (
                                <div
                                    key={p.promptKey}
                                    className={`sv-prompt-item ${p.isActive ? 'sv-prompt-item--active' : ''}`}
                                >
                                    <div className="sv-prompt-item-info">
                                        <div className="sv-prompt-item-header">
                                            <span className="sv-prompt-item-name">{p.promptName}</span>
                                            {p.isActive && (
                                                <span className="sv-prompt-item-badge">적용 중</span>
                                            )}
                                        </div>
                                        <span className="sv-prompt-item-content">{p.promptContent}</span>
                                    </div>
                                    <div className="sv-prompt-item-actions">
                                        {!p.isActive && (
                                            <button
                                                className="sv-activate-btn"
                                                onClick={() => handleActivate(p.promptKey)}
                                            >
                                                선택
                                            </button>
                                        )}
                                        <button
                                            className="sv-delete-btn"
                                            onClick={() => handleDelete(p.promptKey)}
                                        >
                                            삭제
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* 정보 섹션 */}
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
