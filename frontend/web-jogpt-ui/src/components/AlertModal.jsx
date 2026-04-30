export default function AlertModal({ onClose }) {
    return (
        <div className="tpl-toast-alert show" onClick={onClose}>
            <div className="toast-alert" onClick={(e) => e.stopPropagation()}>
                <div className="toast-alert-header">
                    <h1>알림</h1>
                </div>
                <div className="toast-alert-body">
                    <div className="toast-alert-text">알림이 도착했습니다!</div>
                </div>
            </div>
        </div>
    );
}