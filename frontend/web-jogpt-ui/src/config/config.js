const CONFIG = {
    // localhost 사용 <-> 실제 서버 사용 사이를 토글하려면:
    API_CONTENTS_URL: 'http://localhost:8082', /*/  'http://agentcloudllm.me:8082' /*/

    API_BASE_URL: 'http://localhost:8086', /* / 'http://agentcloudllm.me:8086' /*/
    AI_API_URL: 'http://localhost:8000', /* / 'http://실제서버:8000' /*/
    AI_JO_GTP: 'http://agentcloudllm.me:8082', /* / 'http://localhost:8082' /*/
    AI_MEMBERSECURITY: 'http://agentcloudllm.me:8086' /* / 'http://localhost:8086' /*/

};

export default CONFIG;