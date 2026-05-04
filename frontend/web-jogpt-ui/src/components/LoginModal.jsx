import CONFIG from '../config/config';

export default function LoginModal({ onClose }) {
    const handleSocialLogin = (url) => {
        window.location.href = url;
    };

    return (
        <div className="modelLogin" style={{ display: 'flex' }} onClick={onClose}>
            <div className="modelJoin" onClick={(e) => e.stopPropagation()}>
                <div className="modelJoinTitle">
                    <h2 className="modelJoinTitleH2 fontFamily" id="fontFamily">로그인 또는 회원가입</h2>
                    <p className="modelJoinTitleP fontFamily">소셜 로그인을 통해 쉽게 로그인하세요</p>
                </div>
                <div className="modelJoinSocial">
                    <div className="socialJoin-kakao" onClick={() => handleSocialLogin(`${CONFIG.API_BASE_URL}/oauth2/authorization/kakao`)}>
                        <div className="socialJoins-round">
                            <img src="/image/kakaoLogo.png" className="login-logo" alt="카카오로고" />
                        </div>
                        <div className="socialJoins-name">카카오 로그인</div>
                    </div>
                    <div className="socialJoin-naver" onClick={() => handleSocialLogin(`${CONFIG.API_BASE_URL}/oauth2/authorization/naver`)}>
                        <div className="socialJoins-round">
                            <img src="/image/naver_login.png" alt="네이버 로그인" className="login-logo" />
                        </div>
                        <div className="socialJoins-name">네이버 로그인</div>
                    </div>
                    <div className="socialJoin-google" onClick={() => handleSocialLogin(`${CONFIG.API_BASE_URL}/oauth2/authorization/google`)}>
                        <div className="socialJoins-round">
                            <img src="/image/google_login.png" alt="구글 로그인" className="login-logo login-google" />
                        </div>
                        <div className="socialJoins-name">구글 로그인</div>
                    </div>
                    <div className="socialJoin-facebook" onClick={() => handleSocialLogin(`${CONFIG.API_BASE_URL}/oauth2/authorization/facebook`)}>
                        <div className="socialJoins-round">
                            <img src="/image/facebook_login.png" alt="페이스북 로그인" className="login-logo" />
                        </div>
                        <div className="socialJoins-name">페이스북 로그인</div>
                    </div>
                    <div className="socialJoin-github" onClick={() => handleSocialLogin(`${CONFIG.API_BASE_URL}/oauth2/authorization/github`)}>
                        <div className="socialJoins-round">
                            <img src="/image/GitHub_login.svg" alt="깃허브 로그인" className="login-logo" />
                        </div>
                        <div className="socialJoins-name">깃허브 로그인</div>
                    </div>
                    <div className="socialJoin-self">
                        <div className="socialJoins-round">
                            <img alt="JO-GPT_login" src="/image/JO-GPT_login.png" className="login-logo" />
                        </div>
                        <div className="socialJoins-name">JO-GPT 회원가입</div>
                    </div>
                </div>
            </div>
        </div>
    );
}