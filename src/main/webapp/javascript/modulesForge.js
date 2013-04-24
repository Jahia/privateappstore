function countDownload(modulePath) {
    $.post(modulePath+".downloadCount.do");
}

function reportReview(reviewPath, modalIdentifier) {

    $.post(reviewPath+".reportReview.do", null, function() {

        $(modalIdentifier).modal('hide');
    });
}