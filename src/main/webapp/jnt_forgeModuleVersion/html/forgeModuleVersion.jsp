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

<div>
    <div></div>
    <div><fmt:message key="jnt_forgeModuleVersion.title"/>:${currentNode.properties["jcr:title"].string}</div>
    <div><fmt:message key="jnt_forgeModuleVersion.versionNumber"/>:${currentNode.properties.versionNumber.string}</div>
    <div><fmt:message key="jnt_forgeModuleVersion.date"/>:<fmt:formatDate value="${currentNode.properties.date.time}" type="date" dateStyle="long"/> </div>
    <div><fmt:message key="jnt_forgeModuleVersion.moduleBinary"/>:<a href="${currentNode.properties.moduleBinary.node.url}">Download</a></div>
    <div><fmt:message key="jnt_forgeModuleVersion.changeLog"/>:${currentNode.properties.changeLog.string}</div>
    <c:if test="${not empty currentNode.properties.relatedJahiaVersion.node.name}">
        <div><fmt:message key="jnt_forgeModuleVersion.relatedJahiaVersion"/>:${currentNode.properties.relatedJahiaVersion.node.name}</div>
    </c:if>
    <div><fmt:message key="jnt_forgeModuleVersion.releaseType"/>:${currentNode.properties.releaseType.string}</div>
    <c:if test="${not empty currentNode.properties.status.node.name}">
        <div><fmt:message key="jnt_forgeModuleVersion.status"/>:${currentNode.properties.status.node.name}</div>
    </c:if>
    <c:if test="${jcr:hasPermission(currentNode, 'editVersion')}">
        <div class="edit"><a href="<c:url value='${url.base}${currentNode.path}.forge-module-update-version.html'/>"><fmt:message key="jnt_forgeModuleVersion.edit"/></a></div>
    </c:if>
    <div class="back"><a href="<c:url value='${url.base}${currentNode.parent.path}.html'/>"><fmt:message key="jnt_forgeModuleVersion.backToParentModule"/></a></div>

</div>
