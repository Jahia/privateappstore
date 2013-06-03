function countDownload(modulePath) {
    $.post(modulePath+".downloadCount.do");
}

function moduleDoAddReview(modulePath, form, bootstrapTab) {

    $.post(modulePath+".chain.do", form.serialize(), function(result) {

        if (result['moduleUrl'] != "")
            window.location = result['moduleUrl'] + "?bootstrapTab=" + bootstrapTab;

    }, "json");
}

function reviewDoReply(reviewPath, form, bootstrapTab) {

    $.post(reviewPath+".replyReview.do", form.serialize(), function(result) {

        if (result['moduleUrl'] != "")
            window.location = result['moduleUrl'] + "?bootstrapTab=" + bootstrapTab;

    }, "json");
}

function reviewDo(action,reviewPath, modalIdentifier, bootstrapTab) {

    $.post(reviewPath+"." + action + ".do", function(result) {

        if (result['moduleUrl'] != "")
            window.location = result['moduleUrl'] + "?bootstrapTab=" + bootstrapTab;
        else
            $(modalIdentifier).modal('hide');

    }, "json");

}