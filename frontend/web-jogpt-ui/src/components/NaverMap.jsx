import {useEffect, useRef} from 'react';
// 네이버 지도 로드를 위한 url
const NAVER_SCRIPT_SRC = `https://maps.apigw.ntruss.com/openapi/v3/maps.js?ncpClientId=${import.meta.env.VITE_NAVER_MAP_CLIENT_ID}`;

// 네이버 지도를 로드하기 위한 메서드
function loadNaverMapScript() {
    return new Promise((resolve) => {
        // 이미 로드 완료된 경우
        if (window.naver && window.naver.maps) {
            resolve();
            return;
        }
        // 이미 script 태그가 있는 경우 (로딩 중)
        const existing = document.querySelector('script[src*="maps.apigw.ntruss.com"]');
        if (existing) {
            existing.addEventListener('load', resolve);
            return;
        }
        // 새로 삽입
        const script = document.createElement('script');
        script.src = NAVER_SCRIPT_SRC;
        script.onload = resolve;
        document.head.appendChild(script);
    });
}

// 네이버 맵에 관한 코드
export default function NaverMap({coords, mapId}) {
    const mapRef = useRef(null);

    useEffect(() => {
        if (!coords) return;
        // 위도 경도 표출
        const x = parseFloat(coords.x);
        const y = parseFloat(coords.y);
        if (isNaN(x) || isNaN(y)) return;


        // React 환경에서 네이버 지도 api를 안전하게 불러와 특정 좌표에 지도를 렌터링 하고 그 중심에 마커(핀)를 꽃는 핵심 로직
        // 비동기 스크립트 로딩: 네이버 지도 기능을 사용하려면 먼저 외부 서버에서 관련 스크립트 파일을 다운로드 받아야 합니다.
        loadNaverMapScript().then(() => {
            // DOM ( 웹 브라우저가 HTML 문서를 인식하고 조작할 수 있도록 만든 트리 모양의 구조도) 요소 검증
           // mapRef.current 요소 안에 실제 네이버 지도를 생성합니다.
            if (!mapRef.current) return;
            const map = new window.naver.maps.Map(mapRef.current, {
                // 지도의 초기 중심점을 설정합니다. y는 위도, x는 경도를 의미합니다.
                center: new window.naver.maps.LatLng(y, x),
                zoom: 16,
            });
            new window.naver.maps.Marker({
                position: new window.naver.maps.LatLng(y, x),
                map: map,
            });
        });
    }, [coords]);

    if (!coords) return null;

    return (
        <div
            ref={mapRef}
            id={mapId}
            style={{
                width: '100%',
                height: '300px',
                borderRadius: '12px',
                marginTop: '10px',
                overflow: 'hidden',
            }}
        />
    );
}
