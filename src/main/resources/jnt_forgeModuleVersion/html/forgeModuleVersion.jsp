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

<c:set var="id" value="${currentNode.identifier}"/>
<jcr:nodeProperty node="${currentNode}" name="changeLog" var="changeLog"/>
<jcr:nodeProperty node="${currentNode}" name="versionNumber" var="versionNumber"/>
<jcr:nodeProperty node="${currentNode}" name="requiredVersion" var="requiredVersion"/>

<c:if test="${isDeveloper && not viewAsUser}">

    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            $(document).ready(function() {

                <c:url var="postURL" value="${url.base}${currentNode.path}"/>
                $('#changeLog-${currentNode.identifier}').editable({
                    <jsp:include page="../../commons/bootstrap-editable-options-wysihtml5.jsp">
                        <jsp:param name="postURL" value='${postURL}'/>
                        <jsp:param name="fullEditor" value='false'/>
                    </jsp:include>
                });

                $('#toggle-changeLog-${currentNode.identifier}').click(function(e) {
                    e.stopPropagation();
                    e.preventDefault();
                    $('#changeLog-${currentNode.identifier}').editable('toggle');
                });

                <jcr:node var="requiredVersions" path="${renderContext.site.path}/contents/forge-modules-required-versions"/>

                <c:set var="requiredVersionsList" value=""/>
                <c:forEach items="${jcr:getNodes(requiredVersions, 'jnt:contentFolder')}" var="requiredVersionsFolder" varStatus="statusFolder">
                    <c:set var="requiredVersionsList" value="${requiredVersionsList}{text: '${requiredVersionsFolder.displayableName}', children : ["/>
                    <c:forEach items="${jcr:getNodes(requiredVersionsFolder, 'jnt:text')}" var="requiredVersionNode" varStatus="status">
                        <c:set var="requiredVersionsList" value="${requiredVersionsList} {id: '${requiredVersionNode.identifier}', text:'${requiredVersionNode.displayableName}'} ${status.last ? ']': ','}"/>
                    </c:forEach>
                    <c:set var="requiredVersionsList" value="${requiredVersionsList}}${statusFolder.last ? '' : ','}"/>
                </c:forEach>

                $('#requiredVersion-${id}').editable({
                    inputclass: 'input-large',
                    source: [${requiredVersionsList}],
                    value: '${requiredVersion.node.identifier}',
                    <jsp:include page="../../commons/bootstrap-editable-options.jsp">
                        <jsp:param name="postURL" value="${postURL}"/>
                    </jsp:include>
                });

            });
        </script>
    </template:addResources>

</c:if>

<c:choose>

    <c:when test="${isActiveVersion}">
        <h2><fmt:message key="jnt_forgeModule.label.whatsNew"><fmt:param value="${versionNumber.string}"/></fmt:message></h2>
    </c:when>

    <c:otherwise>

        <header>
            <h3>${versionNumber.string}</h3>

            <div class="pull-right">

                <a class="btn btn-small" href="${currentNode.properties.url.string}"
                   onclick="countDownload('<c:url value="${url.base}${currentNode.path}"/>')">
                    <fmt:message key="jnt_forgeModule.label.downloadVersion">
                        <fmt:param value="${versionNumber.string}"/>
                    </fmt:message>
                </a>

            <c:if test="${isDeveloper && not viewAsUser}">
                <button class="btn btn-small btn-success makeActiveVersion" data-target="<c:url value="${url.base}${currentNode.path}"/>">
                    <fmt:message key="jnt_forgeModule.label.developer.makeActiveVersion"/>
                </button>

            </c:if>

            </div>
        </header>

    </c:otherwise>

</c:choose>

<c:if test="${isDeveloper && not viewAsUser}">
    <p class="editable-toggle">
        <a id="toggle-changeLog-${currentNode.identifier}" href="#"><i class="icon-pencil"></i>&nbsp;<fmt:message key="jnt_forgeModule.label.edit"/></a>
    </p>
    <div data-original-title="<fmt:message key="jnt_forgeModuleVersion.label.changeLog"/>" data-toggle="manual" data-name="changeLog" data-type="wysihtml5"
    data-pk="1" id="changeLog-${currentNode.identifier}" class="editable">
</c:if>

${changeLog.string}

<c:if test="${isDeveloper && not viewAsUser}">
    </div>
</c:if>

<footer>
    <dl class="inline">
        <dt><fmt:message key="jnt_forgeModule.label.relatedJahiaVersion"/></dt>
        <dd>
            <c:if test="${isDeveloper && not viewAsUser}">
                <a href="#" id="requiredVersion-${id}" class="editable editable-click" data-type="select2" data-pk="1"
                   data-name="requiredVersion" data-original-title="<fmt:message key="jnt_forgeModuleVersion.label.requiredVersion"/>">
            </c:if>
            ${requiredVersion.node.displayableName}
            <c:if test="${isDeveloper && not viewAsUser}">
                </a>
            </c:if>
        </dd>
        <dt><fmt:message key="jnt_forgeModule.label.updated"/></dt>
        <dd><fmt:formatDate value="${moduleVersionBinary.contentLastModifiedAsDate}" pattern="yyyy-MM-dd" /></dd>
    </dl>
</footer>

