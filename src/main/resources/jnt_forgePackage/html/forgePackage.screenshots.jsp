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

<template:addResources type="javascript" resources="html5shiv.js"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="isDeveloper" value="${jcr:hasPermission(currentNode, 'jcr:write')}"/>
<c:if test="${isDeveloper}">
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}"/>
</c:if>
<jcr:node var="screenshots" path="${currentNode.path}/screenshots"/>
<c:set var="isEmptyTab" value="false"/>

<c:if test="${(not isDeveloper || viewAsUser) && empty jcr:getChildrenOfType(screenshots, 'jnt:file')}">
    <c:set var="isEmptyTab" value="true"/>
    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            $(document).ready(function() {
                var wrapper = $('#moduleScreenshots');
                var tabID = wrapper.parents('.tab-pane').attr("id");
                wrapper.parents(".jnt_bootstrapTabularList").find("a[href='#" + tabID + "']").parent().remove();
            });
        </script>
    </template:addResources>
</c:if>

<div id="fileList${renderContext.mainResource.node.identifier}">

    <section id="moduleScreenshots">

        <h2><fmt:message key="jnt_forgeModule.label.screenshots"/></h2>

        <template:addCacheDependency path="${currentNode.path}/screenshots"/>
        <template:module node="${screenshots}"/>

    </section>
</div>