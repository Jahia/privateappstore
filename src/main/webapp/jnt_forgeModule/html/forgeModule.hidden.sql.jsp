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

<c:if test="${getActiveVersion}">

    <jcr:sql
        var="query"
        sql="SELECT * FROM [jnt:forgeModuleVersion] AS moduleVersion
            INNER JOIN [jnt:file] AS moduleVersionBinary ON ischildnode(moduleVersionBinary,moduleVersion)
            WHERE isdescendantnode(moduleVersion,['${currentNode.path}'])
            ORDER BY moduleVersion.['jcr:created'] DESC"
        limit= '1' />

    <c:forEach items="${query.rows}" var="row">
        <c:set target="${moduleMap}" property="activeVersion" value="${row.nodes['moduleVersion']}" />
        <c:set target="${moduleMap}" property="activeVersionBinary" value="${row.nodes['moduleVersionBinary']}"/>
    </c:forEach>

</c:if>

<c:if test="${getPreviousVersions}">

    <jcr:sql
            var="previousVersions"
            sql="SELECT * FROM [jnt:forgeModuleVersion] WHERE isdescendantnode(['${currentNode.path}'])
              ORDER BY [jcr:created] DESC" />

    <c:set target="${moduleMap}" property="previousVersions" value="${previousVersions}"/>

</c:if>