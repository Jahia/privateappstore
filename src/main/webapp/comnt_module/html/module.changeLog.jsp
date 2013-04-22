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

<template:addResources type="javascript" resources="html5shiv.js, modulesForge.js"/>

<c:set var="id" value="${currentNode.identifier}"/>

<template:include view="sql">
    <template:param name="getActiveVersion" value="true"/>
    <template:param name="getPreviousVersions" value="true"/>
</template:include>
<c:set value="${moduleMap.activeVersion}" var="activeVersion"/>
<c:set value="${moduleMap.previousVersions}" var="previousVersions"/>

<article id="moduleChangeLog">

    <section class="whatsNew">

        <jcr:nodeProperty node="${activeVersion}" name="changeLog" var="changeLog"/>
        <jcr:nodeProperty node="${activeVersion}" name="version" var="version"/>
        <jcr:nodeProperty node="${activeVersion}" name="relatedJahiaVersion" var="relatedJahiaVersion"/>
        <jcr:nodeProperty node="${activeVersion}" name="moduleBinary" var="moduleBinary"/>

        <h2>What's new in ${version.string}</h2>

        ${changeLog.string}

        <footer>
            <dl class="inline">
                <dt>Requires Jahia:</dt>
                <dd>${relatedJahiaVersion.node.displayableName}</dd>
                <dt>Updated:</dt>
                <dd><fmt:formatDate value="${moduleBinary.node.contentLastModifiedAsDate}" pattern="yyyy-MM-dd" /></dd>
            </dl>
        </footer>

    </section>

    <c:if test="${functions:length(previousVersions.nodes) > 0}">

        <section class="previousVersions">

            <h2>Previous versions</h2>

            <c:forEach items="${previousVersions.nodes}" var="previousVersion">

                <article class="previousVersion">

                    <jcr:nodeProperty node="${previousVersion}" name="changeLog" var="changeLog"/>
                    <jcr:nodeProperty node="${previousVersion}" name="version" var="version"/>
                    <jcr:nodeProperty node="${previousVersion}" name="relatedJahiaVersion" var="relatedJahiaVersion"/>

                    <jcr:sql
                        var="moduleBinaries"
                        sql="SELECT * FROM [jnt:file] WHERE ischildnode(['${previousVersion.path}'])
                          ORDER BY ['jcr:lastModified'] DESC" limit ='1'/>

                    <c:forEach items="${moduleBinaries.nodes}" var="moduleBinaryNode">
                        <c:set value="${moduleBinaryNode}" var="moduleBinary"/>
                    </c:forEach>

                    <header>
                        <h3>${version.string}</h3>
                        <a class="btn btn-small pull-right" href="${moduleBinary.url}" onclick="countDownload('<c:url value="${url.base}${currentNode.path}"/>')">Download version ${version.string}</a>
                    </header>

                    <div class="clearfix">

                        ${changeLog.string}

                    </div>

                    <footer>
                        <dl class="inline">
                            <dt>Requires Jahia:</dt>
                                <dd>${relatedJahiaVersion.node.displayableName}</dd>
                            <dt>Updated:</dt>
                                <dd><fmt:formatDate value="${moduleBinary.contentLastModifiedAsDate}" pattern="yyyy-MM-dd" /></dd>
                        </dl>
                    </footer>

                </article>

            </c:forEach>

        </section>

    </c:if>

</article>