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

<div class="module moduleviewdetail">
  <c:url value="${renderContext.request.requestURL}" var="moduleUrl" />
  <c:url value="${currentNode.properties.moduleFile.node.url}" var="downloadUrl" />
  <a class="modulebutton" href="${downloadUrl}"><strong>
  <fmt:message key="forge.download"/>
  </strong> <em>
  <fmt:message key="forge.download"/>
  </em></a> <a class="modulebutton" href="mailto:?subject=[JAHIA Forge] ${currentNode.properties['jcr:title'].string}&body=${moduleUrl}"><strong>
  <fmt:message key="forge.sendToFriend"/>
  </strong> <em>
  <fmt:message key="forge.sendToFriend"/>
  </em></a>
  <c:url value="${currentNode.properties.icon.node.url}" var="iconUrl" />
  <img alt="icon" src="${iconUrl}" class="moduleicon"> <a class="clearitem floatright returnLink" href="javascript:history.back()" title='<fmt:message key="forge.backToList"/>'>
  <fmt:message key="forge.backToList"/>
  </a>
  <h1>${currentNode.properties['jcr:title'].string}</h1>
  <p class="moduleinfo">
    <fmt:message key="forge.by"/>
    &nbsp;<a href="${currentNode.properties.authorURL.string}">${currentNode.properties.authorName.string}</a> -
    <fmt:formatDate value="${currentNode.properties.date.time}" type="date" dateStyle="long"/>
  </p>
  <p class="moduleinfo">
    <c:if test="${not empty currentNode.properties.url.string}">
      <fmt:message key="forge.url"/>
      :&nbsp;<a href="${currentNode.properties.url.string}" target="_blank">${currentNode.properties.url.string}</a> </c:if>
    <c:if test="${not empty currentNode.properties.codeRepository.string}"> -
      <fmt:message key="forge.codeRepository"/>
      :&nbsp;<a href="${currentNode.properties.codeRepository.string}" target="_blank">${currentNode.properties.codeRepository.string}</a> </c:if>
    <c:if test="${not empty currentNode.properties.jahiAppLicense.node}"> -
      <fmt:message key="forge.license"/>
      :&nbsp;${currentNode.properties.jahiAppLicense.node.properties['jcr:title'].string} </c:if>
      
	      <c:if test="${not empty currentNode.properties.relatedJahiaVersion.node}"><br />
      <fmt:message key="forge.relatedJahiaVersion"/>
      :&nbsp;${currentNode.properties.relatedJahiaVersion.node.properties['jcr:title'].string} </c:if>
      
	      <c:if test="${not empty currentNode.properties.jahiAppStatus.node}"> -
      <fmt:message key="forge.jahiAppStatus"/>
      :&nbsp;${currentNode.properties.jahiAppStatus.node.properties['jcr:title'].string} </c:if>
  </p>
  <c:if test="${jcr:isNodeType(currentNode,'jmix:rating')}">
    <c:url value="${url.currentModule}/img/rating_${fn:substringBefore(currentNode.properties['j:sumOfVotes'].long / currentNode.properties['j:nbOfVotes'].long,'.')}.png" var="ratingUrl" />
    <div class="ratingbox"><img alt="rating" src="${ratingUrl}"><small> ${currentNode.properties['j:nbOfVotes'].string} ratings</small></div>
  </c:if>
  <p>${currentNode.properties.bigDescription.string}</p>
    <div class="addVersion"><span><a href="<c:url value='${url.base}${currentNode.path}.forge-addversion.html'/>"><fmt:message key="forge.addVersion"/></a></span></div>
    <c:forEach items="${currentNode.nodes}" var="version">
        <c:if test="${jcr:isNodeType(version,'comnt:moduleVersion')}">
            <template:module node="${version}" view="default"/>
        </c:if>
    </c:forEach>
</div>
