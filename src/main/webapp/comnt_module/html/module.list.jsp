<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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

<div class="module moduleviewlist">

    <c:url value="${url.base}${currentNode.path}.html" var="moduleUrl" />
    <a href="${moduleUrl}">
        <c:url value="${currentNode.properties.icon.node.url}" var="iconUrl" />
        <img alt="icon" src="${iconUrl}" class="moduleicon">
    </a>

     <c:if test="${jcr:isNodeType(currentNode,'jmix:rating')}">
              <c:url value="${url.currentModule}/img/rating_${fn:substringBefore(currentNode.properties['j:sumOfVotes'].long / currentNode.properties['j:nbOfVotes'].long,'.')}.png" var="ratingUrl" />
            <div class="ratingbox floatright"><img alt="rating" src="${ratingUrl}"><small> ${currentNode.properties['j:nbOfVotes'].string} ratings</small></div>
    </c:if>

    <h2><a href="${moduleUrl}">${currentNode.properties['jcr:title'].string}</a></h2>

    <p class="moduleinfo"><fmt:message key="forge.by"/>&nbsp;<a href="${currentNode.properties.authorURL.string}">${currentNode.properties.authorName.string}</a> - <fmt:formatDate value="${currentNode.properties.date.time}" type="date" dateStyle="long"/></p>

    <p>${currentNode.properties.quickDescription.string}</p>

    <c:url value="${currentNode.properties.moduleFile.node.url}" var="downloadUrl" />
    <a class="modulebutton" href="${downloadUrl}"><strong><fmt:message key="forge.download"/></strong> <em><fmt:message key="forge.download"/></em></a>
</div>