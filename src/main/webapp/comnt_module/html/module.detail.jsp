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
  <a class="modulebutton" href="mailto:?subject=[JAHIA Forge] ${currentNode.properties['jcr:title'].string}&body=${moduleUrl}"><strong>
  <fmt:message key="forge.sendToFriend"/>
  </strong> <em>
  <fmt:message key="forge.sendToFriend"/>
  </em></a>
  <c:url value="${currentNode.properties.icon.node.url}" var="iconUrl" />
  <img alt="icon" src="${iconUrl}" class="moduleicon"> <a class="clearitem floatright returnLink" href="javascript:history.back()" title='<fmt:message key="forge.backToList"/>'>
  <fmt:message key="forge.backToList"/>
  </a>
  <h1>${currentNode.properties['jcr:title'].string}</h1>
  <p class="authorinfo">
    <fmt:message key="forge.by"/>
    &nbsp;<a href="${currentNode.properties.authorURL.string}">${currentNode.properties.authorName.string}</a> -
    <fmt:formatDate value="${currentNode.properties.date.time}" type="date" dateStyle="long"/>
    ${currentNode.properties.authorURL.string}
  </p>
  <p class="moduleinfo">
    <c:if test="${not empty currentNode.properties.url.string}">
      <fmt:message key="forge.url"/>
      :&nbsp;<a href="${currentNode.properties.url.string}" target="_blank">${currentNode.properties.url.string}</a></c:if>
    <c:if test="${not empty currentNode.properties.codeRepository.string}"> -
      <fmt:message key="forge.codeRepository"/>
      :&nbsp;<a href="${currentNode.properties.codeRepository.string}" target="_blank">${currentNode.properties.codeRepository.string}</a></c:if>
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
    <div class="modulescreenshots">
        <c:if test="${not empty currentNode.properties.screenshot1.node}">
            <fmt:message key="comnt_module.screenshot1"/>:
            <img alt="icon" src="${currentNode.properties.screenshot1.node.url}">
        </c:if>
        <c:if test="${not empty currentNode.properties.screenshot2.node}">
            <fmt:message key="comnt_module.screenshot2"/>:
            <img alt="icon" src="${currentNode.properties.screenshot2.node.url}">
        </c:if>
        <c:if test="${not empty currentNode.properties.screenshot3.node}">
            <fmt:message key="comnt_module.screenshot3"/>:
            <img alt="icon" src="${currentNode.properties.screenshot3.node.url}">
        </c:if>
        <c:if test="${not empty currentNode.properties.screenshot4.node}">
            <fmt:message key="comnt_module.screenshot4"/>:
            <img alt="icon" src="${currentNode.properties.screenshot4.node.url}">
        </c:if>
    </div>

    <c:if test="${currentNode.properties.supportedByJahia.boolean}">
        <div class="supportedByJahia"><fmt:message key="comnt_module.supportedByJahia"/></div>
    </c:if>

    <c:if test="${currentNode.properties.reviewedByJahia.boolean}">
        <div class="reviewedByJahia"><fmt:message key="comnt_module.reviewedByJahia"/></div>
    </c:if>

  <p>${currentNode.properties.bigDescription.string}</p>
    <div class="addVersion">
        <span><a href="<c:url value='${url.base}${currentNode.path}.forge-addversion.html'/>"><fmt:message key="forge.addVersion"/></a></span>
    </div>
    <div class="module_versions">
        <c:forEach items="${currentNode.nodes}" var="version">
            <c:if test="${jcr:isNodeType(version,'comnt:moduleVersion')}">
                <a href="<c:url value='${url.base}${version.path}.html'/>">${version.name}</a>
            </c:if>
        </c:forEach>
    </div>

    <div class="edit"><a href="<c:url value='${url.base}${currentNode.path}.edit-module.html'/>">edit</a></div>

</div>
