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
<c:set var="isForgeAdmin" value="${jcr:hasPermission(renderContext.site, 'jahiaForgeModerateModule')}"/>
<c:set var="publishedCondition" value=""/>
<c:if test="${!jcr:hasPermission(renderContext.site, 'jahiaForgeModerateModule')}">
    <c:set var="publishedCondition" value=" AND [published]=true"/>
</c:if>
<c:set var="statement"
       value="SELECT * FROM [jnt:content]
                WHERE ISDESCENDANTNODE('${renderContext.site.path}') ${publishedCondition}
                AND ([jcr:primaryType] = 'jnt:forgeModule' OR [jcr:primaryType] = 'jnt:forgePackage')
                ORDER BY [jcr:created] DESC"/>

<query:definition var="listQuery" statement="${statement}"/>
<c:set target="${moduleMap}" property="listQuery" value="${listQuery}" />
<template:addCacheDependency flushOnPathMatchingRegexp="${renderContext.site.path}/contents/modules-repository/.*"/>