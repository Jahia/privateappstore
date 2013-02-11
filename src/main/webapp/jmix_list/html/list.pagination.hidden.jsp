<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<%--
<c:if test="${not empty param.begin}">
    <c:set target="${moduleMap}" property="begin" value="${param.begin}" />
</c:if>

<c:if test="${not empty param.end}">
    <c:set target="${moduleMap}" property="end" value="${param.end}" />
</c:if>--%>


<script type="text/javascript">

$(document).ready(function() {

               $(".pagination a").click(function(){
                    var cutAt = (this.href.indexOf("?") + 1);
                    paginationParameters = this.href.substr(cutAt);
                    var urlTocall = baseUrl + "?" + paginationParameters;
                    $('#forgeContent').load(urlTocall);
                    return false;
               });

       });

</script>

<template:option node="${currentNode}" nodetype="${currentNode.primaryNodeTypeName},jmix:list" view="hidden.header"/>
<c:set var="pageSize" value="${moduleMap.nbOfItemsPerPage}"/>
<template:initPager totalSize="${moduleMap.listTotalSize}" pageSize="${pageSize}" id="${currentNode.identifier}"/>
<c:if test="${moduleMap.begin eq 0}">
     <c:set target="${moduleMap}" property="end" value="${moduleMap.nbOfItemsPerPage}"/>
</c:if>