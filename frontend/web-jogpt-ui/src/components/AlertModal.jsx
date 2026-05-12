export default function AlertModal({ onClose, notifications = [], onChatSelect }) {

    const formatTime = (date) => {
        const d = new Date(date);
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes}`;
    };

    const handleNotificationClick = (noti) => {
        if (noti.chatKey) {
            onChatSelect?.(noti.chatKey);
        }
    };

    return (
        <div className="tpl-toast-alert show" onClick={onClose}>
            <div className="toast-alert" onClick={(e) => e.stopPropagation()}>
                <div className="toast-alert-header">
                    <h1>알림</h1>
                    <button className="toast-alert-close" onClick={onClose}>✕</button>
                </div>
                <div className="toast-alert-body">
                    {notifications.length === 0 ? (
                        <div className="toast-alert-empty">알림이 없습니다.</div>
                    ) : (
                        notifications.map((noti) => (
                            <div
                                key={noti.id}
                                className={`toast-alert-item ${noti.chatKey ? 'clickable' : ''}`}
                                onClick={() => handleNotificationClick(noti)}
                            >
                                <div className="toast-alert-item-icon">🔔</div>
                                <div className="toast-alert-item-content">
                                    <div className="toast-alert-item-message">{noti.message}</div>
                                    <div className="toast-alert-item-time">{formatTime(noti.time)}</div>
                                </div>
                                {noti.chatKey && (
                                    <div className="toast-alert-item-arrow">›</div>
                                )}
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
}