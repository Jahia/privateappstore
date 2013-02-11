<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:include view="pagination.init.hidden" />

<div id="forgeContent" class="forge-skin-content">

    <template:include view="forge" />

</div>

<c:if test="${subView eq 'list'}">
    <script type="text/javascript">
        <c:choose>
            <c:when test="${subView eq 'box'}">
                <c:url value="${url.base}${currentNode.path}.forge.html.ajax" var="ajaxUrl" />
            </c:when>
            <c:otherwise>
                <c:url value="${url.base}${currentNode.path}.forgeDetail.html.ajax" var="ajaxUrl" />
             </c:otherwise>
         </c:choose>

        $('#forgeContent').load('${ajaxUrl}');
    </script>
</c:if>
