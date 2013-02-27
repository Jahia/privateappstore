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
    <c:url value="${currentNode.properties.icon.node.thumbnailUrls['thumbnail']}" var="iconUrl" />
    <a class="clearitem floatright returnLink" href="javascript:history.back()" title='<fmt:message key="forge.backToList"/>'>
        <fmt:message key="forge.backToList"/>
    </a>
    <h1>${currentNode.properties['jcr:title'].string}</h1>
    <img alt="icon" src="${iconUrl}" class="moduleicon">
    <div class="authorinfo">
        <fmt:message key="forge.by"/>
        &nbsp;<a href="${currentNode.properties.authorURL.string}">${currentNode.properties.authorName.string}</a> -
        <fmt:formatDate value="${currentNode.properties.date.time}" type="date" dateStyle="long"/>
        ${currentNode.properties.authorURL.string}
    </div>
    <div class="moduleinfo">
        <c:if test="${not empty currentNode.properties.url.string}">
            <fmt:message key="forge.url"/>
            :&nbsp;<a href="${currentNode.properties.url.string}" target="_blank">${currentNode.properties.url.string}</a><br /></c:if>
        <c:if test="${not empty currentNode.properties.codeRepository.string}"> -
            <fmt:message key="forge.codeRepository"/>
            :&nbsp;<a href="${currentNode.properties.codeRepository.string}" target="_blank">${currentNode.properties.codeRepository.string}</a><br /></c:if>
        <c:if test="${not empty currentNode.properties.jahiAppLicense.node}"> -
            <fmt:message key="forge.license"/>
            :&nbsp;${currentNode.properties.jahiAppLicense.node.properties['jcr:title'].string} <br /></c:if>

        <c:if test="${not empty currentNode.properties.relatedJahiaVersion.node}">
            <fmt:message key="forge.relatedJahiaVersion"/>
            :&nbsp;${currentNode.properties.relatedJahiaVersion.node.properties['jcr:title'].string}<br />
        </c:if>

        <c:if test="${not empty currentNode.properties.jahiAppStatus.node}"> -
            <fmt:message key="forge.jahiAppStatus"/>
            :&nbsp;${currentNode.properties.jahiAppStatus.node.properties['jcr:title'].string}<br />
        </c:if>

        <c:if test="${not empty currentNode.properties.bigDescription.string}"> -
            <fmt:message key="comnt_module.bigDescription"/>
            :&nbsp;${currentNode.properties.bigDescription.string}<br />
        </c:if>

    </div>
    <div class="modulescreenshots">
        <c:if test="${not empty currentNode.properties.screenshot1.node}">
            <fmt:message key="comnt_module.screenshot1"/>:
            <div>
                <a href="${url.files}${currentNode.properties.screenshot1.node.path}">
                    <img alt="screenshot1" src="${currentNode.properties.screenshot1.node.thumbnailUrls['thumbnail']}">
                </a>
            </div>
        </c:if>
        <c:if test="${not empty currentNode.properties.screenshot2.node}">
            <fmt:message key="comnt_module.screenshot2"/>:
            <div>
                <a href="${url.files}${currentNode.properties.screenshot2.node.path}">
                    <img alt="screenshot2" src="${currentNode.properties.screenshot2.node.thumbnailUrls['thumbnail']}">
                </a>
            </div>
        </c:if>
        <c:if test="${not empty currentNode.properties.screenshot3.node}">
            <fmt:message key="comnt_module.screenshot3"/>:
            <div>
                <a href="${url.files}${currentNode.properties.screenshot3.node.path}">
                    <img alt="screenshot3" src="${currentNode.properties.screenshot3.node.thumbnailUrls['thumbnail']}">
                </a>
            </div>
        </c:if>
        <c:if test="${not empty currentNode.properties.screenshot4.node}">
            <fmt:message key="comnt_module.screenshot4"/>:
            <div>
                <a href="${url.files}${currentNode.properties.screenshot4.node.path}">
                    <img alt="screenshot4" src="${currentNode.properties.screenshot4.node.thumbnailUrls['thumbnail']}">
                </a>
            </div>
        </c:if>
    </div>
    <jcr:node var="videoNode" path="${currentNode.path}/video"/>
    <c:if test="${not empty videoNode}">
        <div class="modulevideo">
            <template:module node="${videoNode}" view="default" nodeTypes="jnt:videostreaming"/>
            <template:module node="${videoNode}"/>
            <template:module path="${currentNode.path}/video"/>
        </div>
    </c:if>
    <c:if test="${currentNode.properties.supportedByJahia.boolean}">
        <div class="supportedByJahia"><fmt:message key="comnt_module.supportedByJahia"/></div>
    </c:if>

    <c:if test="${currentNode.properties.reviewedByJahia.boolean}">
        <div class="reviewedByJahia"><fmt:message key="comnt_module.reviewedByJahia"/></div>
    </c:if>

    <c:if test="${jcr:hasPermission(currentNode, 'createVersion')}">
        <div class="addVersion">
            <span><a href="<c:url value='${url.base}${currentNode.path}.forge-addversion.html'/>"><fmt:message key="forge.addVersion"/></a></span>
        </div>
    </c:if>

    <div class="module_versions">
        <c:forEach items="${currentNode.nodes}" var="version">
            <c:if test="${jcr:isNodeType(version,'comnt:moduleVersion')}">
                <a href="<c:url value='${url.base}${version.path}.html'/>">${version.name}</a>
            </c:if>
        </c:forEach>
    </div>

    <c:if test="${jcr:hasPermission(currentNode, 'editModule')}">
        <div class="edit"><a href="<c:url value='${url.base}${currentNode.path}.edit-module.html'/>">edit</a></div>
    </c:if>

    <c:if test="${jcr:hasPermission(currentNode, 'deleteModule')}">
        <template:tokenizedForm>
            <form action="<c:url value='${url.base}${currentNode.path}.DeleteModule.do'/>" method="post" id="deleteModule" enctype="multipart/form-data"  accept="application/json">
            </form>
        </template:tokenizedForm>
        <div class="delete"><a onclick="$('#deleteModule').submit()">delete</a></div>
    </c:if>
    <div class="edit">
        <a class="modulebutton" href="mailto:?subject=[JAHIA Forge] ${currentNode.properties['jcr:title'].string}&body=${moduleUrl}">
            <fmt:message key="forge.sendToFriend"/>
        </a>
    </div>
</div>
