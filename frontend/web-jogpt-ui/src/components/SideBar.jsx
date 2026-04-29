export default function SideBar({ isActive, onMenuClick, onHomeClick, onChatClick, onBellClick }) {
    return (
        <div className={isActive ? 'active' : 'sideBar'}>
            <div className={isActive ? 'sideBarTopActive' : 'sideBar-top'}>
                <button className={isActive ? 'sideBarImgActive' : 'sideBarImg'} onClick={onMenuClick}>
                    <div className="sideButton">
                        <i className="fa fa-bars same-font icon-controller"></i>
                        <div className="sideBarText">메뉴</div>
                    </div>
                </button>
                <button className={isActive ? 'sideBarImgActive' : 'sideBarImg'} onClick={onHomeClick}>
                    <div className="sideButton">
                        <i className="fa fa-plus same-font icon-controller"></i>
                        <div className="sideBarText">홈</div>
                    </div>
                </button>
                <button className={isActive ? 'sideBarImgActive' : 'sideBarImg'} onClick={onChatClick}>
                    <div className="sideButton">
                        <i className="fa fa-comment same-font icon-controller"></i>
                        <div className="sideBarText">채팅</div>
                    </div>
                </button>
                <button className={isActive ? 'sideBarImgActive' : 'sideBarImg'}>
                    <div className="sideButton">
                        <i className="bi bi-grid same-font icon-controller"></i>
                        <div className="sideBarText">모델</div>
                    </div>
                </button>
                <button className={isActive ? 'sideBarImgActive' : 'sideBarImg'}>
                    <div className="sideButton">
                        <i className="fa fa-ellipsis-h same-font icon-controller"></i>
                        <div className="sideBarText">더보기</div>
                    </div>
                </button>
            </div>
            <div className={isActive ? 'sideBar-bottomActive' : 'sideBar-bottom'}>
                <button className={isActive ? 'sideBarImgActive sideBell' : 'sideBarImg sideBell'} onClick={onBellClick}>
                    <div className="icon-controller">
                        <i className="fa fa-bell" style={{ fontSize: '24px', color: '#555' }}></i>
                    </div>
                    <div className="sideBarText">알림</div>
                </button>
                <button className={isActive ? 'sideBarImgActive' : 'sideBarImg'}>
                    <div className="icon-controller">
                        <i className="fa fa-cog" style={{ fontSize: '24px', color: '#555' }}></i>
                    </div>
                    <div className="sideBarText">설정</div>
                </button>
            </div>
        </div>
    );
}