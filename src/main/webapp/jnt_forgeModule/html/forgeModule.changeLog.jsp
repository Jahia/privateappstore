<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="currentUser" type="org.jahia.services.usermanager.JahiaUser"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="html5shiv.js, forge.js"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="isDeveloper" value="${renderContext.loggedIn && jcr:hasPermission(currentNode, 'jcr:all_live')
    && not jcr:hasPermission(currentNode.parent, 'jcr:all_live')}"/>
<c:if test="${isDeveloper}">
    <c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />
</c:if>

<template:include view="hidden.sql">
    <template:param name="getActiveVersion" value="true"/>
    <template:param name="getPreviousVersions" value="true"/>
</template:include>
<c:set value="${moduleMap.activeVersion}" var="activeVersion"/>
<c:set value="${moduleMap.previousVersions}" var="previousVersions"/>

<c:if test="${isDeveloper && not viewAsUser}">

    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            $(document).ready(function() {

                $('.makeActiveVersion').click( function() {

                    var boostrapTab = $(this).parents('.tab-pane').attr('id');

                    var data = {};
                    data['activeVersion'] = true;
                    data['jcrMethodToCall'] = 'put';

                    $.post($(this).attr("data-target"), data, function() {
                        window.location = '${currentNode.url}' + "?bootstrapTab=" + boostrapTab;
                    }, "json");
                });

            });
        </script>
    </template:addResources>
</c:if>

<article id="moduleChangeLog">

    <c:choose>

        <c:when test="${not empty activeVersion}">

            <section class="whatsNew">

                <c:set target="${moduleMap}" property="moduleBinary" value="${moduleMap.activeVersionBinary}"/>
                <template:module node="${activeVersion}" view="changeLog">
                    <template:param name="isActiveVersion" value="true"/>
                    <template:param name="isDeveloper" value="${isDeveloper}"/>
                    <template:param name="viewAsUser" value="${viewAsUser}"/>
                </template:module>

            </section>

        </c:when>
        <c:otherwise>

            <c:if test="${isDeveloper && not viewAsUser}">
                <div class="alert alert-info">
                    <fmt:message key="jnt_forgeModule.label.developer.emptyChangeLog"/>
                </div>
            </c:if>

        </c:otherwise>

    </c:choose>

    <c:if test="${functions:length(previousVersions.nodes) > 0}">

        <section class="previousVersions">

            <h2><fmt:message key="jnt_forgeModule.label.previousVersions"/></h2>

            <c:forEach items="${previousVersions.nodes}" var="previousVersion">

                <article class="previousVersion">

                    <jcr:sql
                            var="moduleBinaries"
                            sql="SELECT * FROM [jnt:file] WHERE ischildnode(['${previousVersion.path}'])
                          ORDER BY ['jcr:lastModified'] DESC" limit ='1'/>

                    <c:forEach items="${moduleBinaries.nodes}" var="moduleBinaryNode">
                        <c:set target="${moduleMap}" property="moduleBinary" value="${moduleBinaryNode}"/>
                    </c:forEach>

                    <template:module node="${previousVersion}" view="changeLog">
                        <template:param name="isActiveVersion" value="false"/>
                        <template:param name="isDeveloper" value="${isDeveloper}"/>
                        <template:param name="viewAsUser" value="${viewAsUser}"/>
                    </template:module>

                </article>

            </c:forEach>

        </section>

    </c:if>

</article>