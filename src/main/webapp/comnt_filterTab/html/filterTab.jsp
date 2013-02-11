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

<template:addResources type="javascript" resources="jquery.min.js"/>

<c:set var="bindedComponent"
       value="${uiComponents:getBindedComponent(currentNode, renderContext, 'j:bindedComponent')}"/>

 <script type="text/javascript">

     <c:choose>
        <c:when test="${subView eq 'list'}">
            <c:url value="${url.base}${bindedComponent.path}.forgeDetail.html.ajax" var="ajaxUrl" />
            var baseUrl = "${ajaxUrl}";
        </c:when>
        <c:otherwise>
            <c:url value="${url.base}${bindedComponent.path}.forge.html.ajax" var="ajaxUrl" />
             var baseUrl = "${ajaxUrl}";
         </c:otherwise>
     </c:choose>
             var criteria="${criteria}";
             var subView="${subView}";

     $(document).ready(function() {

                $("#all").click(function(){
                       criteria = "all";
                       var urlTocall = baseUrl + "?criteria="+criteria+"&subView="+subView;
                       $('#forgeContent').load(urlTocall);
                       return false;
                });

                 $("#new").click(function(){
                           criteria = "new";
                           var urlTocall = baseUrl + "?criteria="+criteria+"&subView="+subView;
                           $('#forgeContent').load(urlTocall);
                           return false;
                 });

                 /*$("#top").click(function(){
                       criteria = "all";
                       var urlTocall = baseUrl + "?criteria="+criteria+"&subView="+subView;
                       $('#forgeContent').load(urlTocall);
                       return false;
                });*/

                 $("#box").click(function(){
                       <c:url value="${url.base}${bindedComponent.path}.forge.html.ajax" var="ajaxUrl" />
                       baseUrl = "${ajaxUrl}";
                       subView = "box";
                       var urlTocall = baseUrl + "?criteria="+criteria+"&subView="+subView+"&"+paginationParameters;
                       $('#forgeContent').load(urlTocall);
                       return false;
                });

                 $("#list").click(function(){
                       <c:url value="${url.base}${bindedComponent.path}.forgeDetail.html.ajax" var="ajaxUrl" />
                       baseUrl = "${ajaxUrl}";
                       subView = "list";
                       var urlTocall = baseUrl + "?criteria="+criteria+"&subView="+subView+"&"+paginationParameters;
                       $('#forgeContent').load(urlTocall);
                       return false;
                });
         });

 </script>

<div class="forge-skin-header" id="filters">
    <p class="forge-skin-title">Modules</p>
    <span>See:</span>
    <ul class="filter">
        <li><a href="" id="all">All</a></li>
        <li><a href="" id="new">New</a></li>
        <!--<li><a href="" id="top">Top</a></li>-->
    </ul>
    <span>View:</span>
    <ul>
        <li><a href="" id="box">Box</a></li>
        <li><a href="" id="list">List</a></li>
    </ul>
	<c:url value="${url.base}${bindedComponent.properties.startNode.node.path}.rss" var="rssUrl" />
    <a href="${rssUrl}" title="rss" class="rss">RSS</a>
</div>