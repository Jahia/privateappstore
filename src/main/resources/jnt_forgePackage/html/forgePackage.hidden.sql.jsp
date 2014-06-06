<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="forge" uri="http://www.jahia.org/modules/forge/tags" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="currentUser" type="org.jahia.services.usermanager.JahiaUser"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addCacheDependency flushOnPathMatchingRegexp="${currentNode.path}/.*"/>
<jcr:sql
        var="query"
        sql="SELECT * FROM [jnt:forgePackageVersion] AS packageVersion
            WHERE isdescendantnode(packageVersion,['${currentNode.path}'])"
        />
<jcr:sql
        var="filesQuery"
        sql="SELECT * FROM [jnt:forgePackageVersion] AS packageVersion
            WHERE isdescendantnode(packageVersion,['${currentNode.path}'])"
        />
<c:set var="sortedModules" value="${forge:sortByVersion(query.nodes)}"/>
<c:set target="${moduleMap}" property="latestVersion" value="${forge:latestVersion(sortedModules)}" />
<c:set target="${moduleMap}" property="previousVersions" value="${forge:previousVersions(sortedModules)}" />
<c:set target="${moduleMap}" property="nextVersions" value="${forge:nextVersions(sortedModules)}" />
<c:if test="${not empty moduleMap.latestVersion.path}">
    <jcr:sql
            var="modulesQuery"
            sql="SELECT * FROM [jnt:forgePackageModule] AS packageVersion
            WHERE isdescendantnode(packageVersion,['${moduleMap.latestVersion.path}'])
            ORDER BY [moduleName]" />
    <c:set target="${moduleMap}" property="submodules" value="${modulesQuery.nodes}" />
</c:if>



