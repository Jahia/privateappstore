<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="currentUser" type="org.jahia.services.usermanager.JahiaUser"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="jquery.js, html5shiv.js, forge.js"/>
<template:addResources type="css" resources="forge.css"/>

<c:set var="isDeveloper" value="${jcr:hasPermission(currentNode, 'jcr:write')}"/>

<c:if test="${isDeveloper}">
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />
</c:if>
<c:set var="isForgeAdmin" value="${jcr:hasPermission(renderContext.site, 'jahiaForgeModerateModule')}"/>

<c:if test="${isDeveloper || isForgeAdmin}">
    <template:addResources type="javascript" resources="jquery.js,bootstrap-transition.js,bootstrap-alert.js,bootstrap-button.js
        ,bootstrap-carousel.js,bootstrap-collapse.js,bootstrap-dropdown.js,bootstrap-modal.js,bootstrap-tooltip.js,bootstrap-popover.js
        ,bootstrap-scrollspy.js,bootstrap-tab.js,bootstrap-typehead.js,bootstrap-affix.js"/>
    <template:addResources type="javascript" resources="select2.js, bootstrap-editable.js, wysihtml5-0.3.0.js, bootstrap-wysihtml5.js, wysihtml5.js"/>
    <template:addResources type="css" resources="select2.css, select2-bootstrap.css, bootstrap-editable.css, wysiwyg-color.css, forge.edition.css"/>
    <template:addResources type="javascript" resources="jquery.fileupload-with-ui.min.js"/>
</c:if>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="title" value="${currentNode.properties['jcr:title'].string}"/>

<jcr:node var="iconFolder" path="${renderContext.mainResource.node.path}/icon" />
<c:forEach var="iconItem" items="${iconFolder.nodes}">
    <c:set var="icon" value="${iconItem}"/>
</c:forEach>


<%@include file="../../commons/authorName.jspf"%>

<c:set var="nbOfVotes"
       value="${not empty currentNode.properties['j:nbOfVotes'] ? currentNode.properties['j:nbOfVotes'].long : null}"/>

<template:include view="hidden.sql">
    <template:param name="getLatestVersion" value="true"/>
</template:include>
<template:addCacheDependency flushOnPathMatchingRegexp="${currentNode.path}/.*"/>

<c:if test="${isDeveloper && not viewAsUser}">

    <c:url var="postURL" value="${url.base}${currentNode.path}"/>
    <fmt:message var="labelEmptyOrganisation" key="jnt_forgeModule.label.developer.emptyOrganisation"/>
    <fmt:message var="labelEmptyFullName" key="jnt_forgeModule.label.developer.emptyFullName"/>

    <template:addResources type="inlinejavascript">

        <script type="text/javascript">

            $(document).ready(function() {

                <c:if test="${empty authorOrganisation}">
                    $('#authorName-header-${id}').on('shown', function(e, editable) {
                        $(this).next('.editable-container').find('.editable-input select option[value="organisation"]').attr("disabled","true");
                    });
                </c:if>

                <c:if test="${empty authorFullName || authorFullName eq authorUsername}">
                $('#authorName-header-${id}').on('shown', function(e, editable) {
                    $(this).next('.editable-container').find('.editable-input select option[value="fullName"]').attr("disabled","true");
                });
                </c:if>

                $('#authorName-header-${id}').editable({
                    source: [{value:'username', text:'${authorUsername}'},
                        {value:'fullName', text:'${not empty authorFullName &&  authorFullName ne authorUsername ? authorFullName : labelEmptyFullName}'},
                        {value:'organisation', text: '${not empty authorOrganisation ? authorOrganisation : labelEmptyOrganisation}'}],
                    value: '${authorNameDisplayedAs}',
                    <jsp:include page="../../commons/bootstrap-editable-options.jsp">
                        <jsp:param name="postURL" value="${postURL}"/>
                        <jsp:param name="customSuccess" value="
                            if (${authorNameDisplayedAs eq 'organisation'} || newValue == 'organisation')
                                document.location = '${currentNode.url}';
                            else {
                                var newAuthorName = $(this).next('.editable-container').find('option[value='+newValue+']').html();
                                $('#authorName-information-${id}').html(newAuthorName).editable('setValue', newValue);
                            }"/>
                    </jsp:include>
                });

            });

        </script>

    </template:addResources>
</c:if>

<section id="moduleHeader" class="box box-rounded">

    <header>
        <h1>${title}</h1>
        <p class="authorName">${authorName}</p>

    </header>
    <c:url var="iconUrl" value="${url.currentModule}/img/icon.png"/>
    <img class="moduleIcon" id="moduleIcon-${currentNode.identifier}" src="${not empty icon.url ? icon.url : iconUrl}"
         alt="<fmt:message key="jnt_forgeModule.label.moduleIcon"><fmt:param value="${title}"/></fmt:message>"/>

    <c:if test="${jcr:isNodeType(renderContext.mainResource.node, 'jnt:forgeModule')}">
        <c:set var="isDeveloper" value="${jcr:hasPermission(renderContext.mainResource.node, 'jcr:write')}"/>
        <c:if test="${isDeveloper}">
            <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}"/>
        </c:if>
    </c:if>

    <c:if test="${isDeveloper && not viewAsUser}">

        <form class="file_upload" id="file_upload_${currentNode.identifier}"
              action="<c:url value='${url.base}${renderContext.mainResource.node.path}.updateModuleIcon.do'/>" method="POST" enctype="multipart/form-data"
              accept="application/json">
            <div id="file_upload_container-${currentNode.identifier}" class="btn btn-block">
                <input type="file" name="file" multiple>
                <button><fmt:message key="forge.editModule.uploadIcon.label"/></button>
                <div id="drop-box-file-upload-${currentNode.identifier}"><fmt:message key="forge.editModule.uploadIcon.label"/></div>
            </div>
        </form>
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
                        // refresh the icon
                        var response = JSON.parse(xhr.response);
                        if (response.iconUpdate) {
                            d = new Date();
                            $("#moduleIcon-${currentNode.identifier}").attr("src", response.iconUrl+"?"+d.getTime());
                        } else {
                            alert(response.errorMessage);
                        }

                    },
                    acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i,
                    uploadTable: $('#files${currentNode.identifier}'),
                    dropZone: $('#file_upload_container-${currentNode.identifier}'),
                    beforeSend: function (event, files, index, xhr, handler, callBack) {
                        handler.formData = {
                            'jcrNodeType': "jnt:file",
                            'jcrReturnContentType': "json",
                            'jcrReturnContentTypeOverride': 'application/json; charset=UTF-8'                    };
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


    <c:if test="${nbOfVotes gt 0}">
        <div class="moduleRating">

            <c:if test="${jcr:isNodeType(currentNode, 'jmix:rating')}">
                <template:include view="hidden.average.readonly" />
                <span>(${nbOfVotes})</span>
            </c:if>

        </div>
    </c:if>
    <c:choose>

        <c:when test="${not empty moduleMap.latestVersion}">
            <jcr:nodeProperty node="${moduleMap.latestVersion}" name="versionNumber" var="versionNumber"/>
            <a class="btn btn-block" href="<c:url value="${moduleMap.latestVersion.properties.url.string}" context="/"/>"
               <c:if test="${not isDeveloper}">onclick="countDownload('<c:url value="${url.base}${currentNode.path}"/>')"</c:if>>
                <fmt:message key="jnt_forgeModule.label.downloadCurrentVersion">
                    <fmt:param value="${versionNumber.string}"/>
                </fmt:message>
            </a>
        </c:when>

        <c:otherwise>
            <a class="btn btn-block disabled" href="#">
                <fmt:message key="jnt_forgeModule.label.downloadCurrentVersion">
                    <fmt:param value="X.X.X.X"/>
                </fmt:message>
            </a>
        </c:otherwise>

    </c:choose>

</section>