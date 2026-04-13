const { app, BrowserWindow } = require('electron');
const path = require('path');

function createWindow() {
    const win = new BrowserWindow({
        width: 1000,
        height: 800,
        webPreferences: {
            nodeIntegration: true, // Node.js API 사용 허용
            contextIsolation: false
        }
    });

    // 개발 모드에서는 localhost:3000을, 배포 후에는 빌드된 파일을 로드
    win.loadURL('http://localhost:3000');

    // 개발자 도구 자동으로 열기 (선택 사항)
    // win.webContents.openDevTools();
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
});