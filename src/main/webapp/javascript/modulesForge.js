function countDownload(modulePath) {
    $.post(modulePath+".downloadCount.do");
}

function reviewDo(action,reviewPath, modalIdentifier, bootstrapTab) {

    $.post(reviewPath+"." + action + ".do", function(result) {

        window.location = result['moduleUrl'] + "?bootstrapTab=" + bootstrapTab;
        $(modalIdentifier).modal('hide');

    }, "json");

}