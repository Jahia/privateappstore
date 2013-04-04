function countDownload(modulePath) {
    $.post(modulePath+".downloadCount.do");
}