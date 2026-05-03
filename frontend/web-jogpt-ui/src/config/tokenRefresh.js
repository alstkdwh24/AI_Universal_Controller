import CONFIG from "./config";

export const fetchWithRefresh = async (url, options = {}) => {
    let res = await fetch(url, options);
    if (res.status === 401) {
        // Handle token refresh logic here
        const refreshResponse = await fetch(`${CONFIG.AI_MEMBERSECURITY}/refreshToken/login/refresh`, {
            method: "POST",
            credentials: "include"
        });
        if (regfreshResponse.ok) {
            const newToken = await refreshResponse.text();
            localStorage.setItem('ACCESS_TOKEN', newToken);
            options.headers = { ...options.headers, 'Authorization': 'Bearer ' + newToken };
            res = await fetch(url, options);  // 재시도
        } else {
            localStorage.removeItem('ACCESS_TOKEN');
            window.location.href = '/';
        }
    }
    return res;
}