const menuBar = document.querySelector('.menuBar');
const sideBar = document.querySelector('.sideBar');
const activeBar = document.querySelector('.active');
const menuActive = document.querySelector('.menuActive');
const contents = document.querySelector('.content');
const realContent = document.querySelector('.realContent');
const topBar = document.querySelector('.topBar');
const sideBarTop = document.querySelector('.sideBar-top');
const sideBarImg = document.querySelector(".sideBarImg");

document.addEventListener('DOMContentLoaded', () => {
    menuBar.addEventListener('click', () => {
        menuBar.classList.toggle('menuActive');
        menuBar.classList.toggle('menuBar');
        sideBar.classList.toggle('active');
        sideBar.classList.toggle('sideBar');
        contents.classList.toggle('content');
        contents.classList.toggle('contentActive');
        realContent.classList.toggle('realContent');
        realContent.classList.toggle('realContentActive');
        topBar.classList.toggle('topBar');
        topBar.classList.toggle('topBarActive');
        sideBarTop.classList.toggle('sideBarTopActive');
        sideBarTop.classList.toggle('sideBar-top');
        sideBarImg.classList.toggle('sideBarImgActive');
        sideBarImg.classList.toggle('sideBarImg');
        sideBarTop.style.width = '60px';
        sideBarImg.style.width = '20rem';

        sideBar.style.display = 'flex';
        sideBar.style.alignItems = 'flex-start';

        console.log('Menu bar clicked');
    });

});