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
    <div><fmt:message key="comnt_moduleVersion.title"/>:${currentNode.properties["jcr:title"].string}</div>
    <div><fmt:message key="comnt_moduleVersion.version"/>:${currentNode.properties.version.string}</div>
    <div><fmt:message key="comnt_moduleVersion.date"/>:<fmt:formatDate value="${currentNode.properties.date.time}" type="date" dateStyle="long"/> </div>
    <div><fmt:message key="comnt_moduleVersion.moduleBinary"/>:<a href="${currentNode.properties.moduleBinary.node.url}">Download</a></div>
    <div><fmt:message key="comnt_moduleVersion.desc"/>:${currentNode.properties.desc.string}</div>
    <div><fmt:message key="comnt_moduleVersion.relatedJahiaVersion"/>:${currentNode.properties.relatedJahiaVersion.node.name}</div>
    <div><fmt:message key="comnt_moduleVersion.releaseType"/>:${currentNode.properties.releaseType.string}</div>
    <div><fmt:message key="comnt_moduleVersion.status"/>:${currentNode.properties.status.node.name}</div>

    <div class="edit"><a href="<c:url value='${url.base}${currentNode.path}.forge-updateversion.html'/>"><fmt:message key="comnt_moduleVersion.edit"/></a></div>
    <div class="back"><a href="<c:url value='${url.base}${currentNode.parent.path}.html'/>"><fmt:message key="comnt_moduleVersion.backToParentModule"/></a></div>

</div>
