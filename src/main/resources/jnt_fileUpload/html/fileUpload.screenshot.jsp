<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="propertyDefinition" type="org.jahia.services.content.nodetypes.ExtendedPropertyDefinition"--%>
<%--@elvariable id="type" type="org.jahia.services.content.nodetypes.ExtendedNodeType"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="css" resources="forge.css"/>
<template:addResources type="javascript" resources="jquery.min.js,jquery-ui.min.js,jquery.fileupload-with-ui.min.js"/>
<fmt:message key="label.dropHere.ie" var="i18nDropHereIE"/>
<%--<template:addResources type="inlinejavascript">
    <script type="text/javascript">
        $(document).ready(function() {
            if ($.browser.msie) {
                $("#drop-box-file-upload-${currentNode.identifier}").empty().append("${functions:escapeJavaScript(i18nDropHereIE)}");
            }
        });
    </script>
</template:addResources>--%>

<c:set var="linked" value="${ui:getBindedComponent(currentNode, renderContext, 'j:bindedComponent')}"/>
<c:set var="targetNode" value="${renderContext.mainResource.node}"/>
<c:if test="${jcr:isNodeType(renderContext.mainResource.node, 'jnt:forgeModule') or jcr:isNodeType(renderContext.mainResource.node, 'jnt:forgePackage')}">
    <jcr:node var="targetNode" path="${renderContext.mainResource.node.path}/screenshots"/>
    <c:set var="isDeveloper" value="${jcr:hasPermission(renderContext.mainResource.node, 'jcr:write')}"/>
    <c:if test="${isDeveloper}">
        <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}"/>
    </c:if>
</c:if>

<c:if test="${isDeveloper && not viewAsUser}">
    <c:if test="${!empty currentNode.properties.target}">
        <c:set var="targetNode" value="${currentNode.properties.target.node}"/>
    </c:if>
    <template:tokenizedForm allowsMultipleSubmits="true">
        <form class="file_upload" id="file_upload_${currentNode.identifier}"
              action="<c:url value='${url.base}${targetNode.path}'/>" method="POST" enctype="multipart/form-data"
              accept="application/json">
            <div id="file_upload_container" class="btn btn-block">
                <input type="file" name="file" accept="image/*" multiple>
                <button><fmt:message key="forge.uploadScreenshots.label"/></button>
                <div id="drop-box-file-upload-${currentNode.identifier}"><fmt:message key="forge.uploadScreenshots.label"/></div>
            </div>
            <c:url var="targetNodePath" value="${url.base}${renderContext.mainResource.node.path}.screenshots.html.ajax">
                <c:param name="targetNodePath" value="${targetNode.path}"/>
            </c:url>

        </form>
    </template:tokenizedForm>
    <table id="files${currentNode.identifier}" class="table"></table>
    <script>
        /*global $ */
        $(function () {
            $('#file_upload_${currentNode.identifier}').fileUploadUI({
                namespace: 'file_upload_${currentNode.identifier}',
                onComplete: function (event, files, index, xhr, handler) {
                    <%--$('#fileList${renderContext.mainResource.node.identifier}').load('${targetNodePath}', function () {--%>
                        <%--$('#moduleScreenshotsList').triggerHandler('uploadCompleted');--%>
                    <%--});--%>
                    var boostrapTab = $('#file_upload_${currentNode.identifier}').parents('.tab-pane').attr('id');

                    window.location = "<c:url value="${url.base}${renderContext.mainResource.node.path}.html?bootstrapTab="/>" + boostrapTab;
                },
                acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
                uploadTable: $('#files${currentNode.identifier}'),
                dropZone: $('#file_upload_container'),
                beforeSend: function (event, files, index, xhr, handler, callBack) {
                    handler.formData = {
                        'jcrNodeType': "jnt:file",
                        'jcrReturnContentType': "json",
                        'jcrReturnContentTypeOverride': 'application/json; charset=UTF-8',
                        'jcrRedirectTo': "<c:url value='${url.base}${renderContext.mainResource.node.path}'/>",
                        'jcrNewNodeOutputFormat': "${renderContext.mainResource.template}.html",
                        'form-token': $('#file_upload_${currentNode.identifier} input[name=form-token]').val()
                    };
                    callBack();
                },
                buildUploadRow: function (files, index) {
                    return $('<tr><td>' + files[index].name + '<\/td>' +
                             '<td class="file_upload_progress"><div><\/div><\/td>' + '<td class="file_upload_cancel">' +
                             '<button class="ui-state-default ui-corner-all" title="Cancel">' +
                             '<span class="ui-icon ui-icon-cancel">Cancel<\/span>' + '<\/button><\/td><\/tr>');
                }
            });
        });
    </script>

</c:if>