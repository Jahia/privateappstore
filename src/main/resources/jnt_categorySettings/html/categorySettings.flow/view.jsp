<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="javascript" resources="tree.jquery.js"/>

<template:addResources type="css" resources="jqtree.css"/>
<template:addResources type="inline">
    <script>
        <template:include view="categoryTree" templateType="html"/>
    </script>
</template:addResources>

<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="${msg.severity == 'ERROR' ? 'validationError' : ''} alert ${msg.severity == 'ERROR' ? 'alert-error' : 'alert-success'}"><button type="button" class="close" data-dismiss="alert">&times;</button>${fn:escapeXml(msg.text)}</div>
</c:forEach>
<div class="box-1">
    <form:form modelAttribute="categorySettings" cssClass="form" autocomplete="off">
        <h3><fmt:message key="jahiaForge.settings.title"/></h3>
        <div class="container-fluid">
            <div class="row-fluid">
                <div class="span2"><fmt:message key="jahiaForge.settings.rootCategory.selected"/></div>
                <div class="span10">${renderContext.site.properties["rootCategory"].node.displayableName}</div>
            </div>
            <div class="row-fluid">
                <div class="span2">
                    <div class="input-group">
                        <div id="tree1"></div>
                        <form:hidden path="rootCategory" id="rootCategoryField"/>
                    </div>
                </div>
                <div class="span2">
                    <button class="btn btn-primary" id="submit" type="submit" name="_eventId_submit" onclick="$('#rootCategoryField').val($('#tree1').tree('getSelectedNode').id)"><i class="icon-ok icon-white"></i>&nbsp;<fmt:message key='label.save'/></button>
                </div>
            </div>
            <div class="row-fluid">

            </div>
        </div>
    </form:form>
</div>
<script>
    $('#tree1').tree({
        data: data
    });
</script>