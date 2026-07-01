<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="json" uri="http://www.atg.com/taglibs/json" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<json:array>
    <json:object>
<%-- Emit a stable opaque id (UUID) but NOT the absolute JCR path: the path is
     internal-structure reconnaissance for anonymous callers and no feed consumer
     needs it (SECURITY-571 #55). --%>
        <json:property name="id" value="${currentNode.identifier}"/>
        <json:property name="name" value="${currentNode.name}"/>
        <json:property name="title" value="${currentNode.displayableName}"/>
        <json:array name="modules">
            <c:forEach items="${jcr:getDescendantNodes(currentNode, 'jnt:content')}" var="child">
                <c:if test="${(child.properties['jcr:primaryType'].string eq 'jnt:forgeModule') or (child.properties['jcr:primaryType'].string eq 'jnt:forgePackage')}">
                    <c:choose>
                        <c:when test="${child.properties['jcr:primaryType'].string eq 'jnt:forgeModule'}">
                            <c:set var="versions" value="${jcr:getChildrenOfType(child, 'jnt:forgeModuleVersion')}"/>
                            <c:set var="groupID" value="${child.properties['groupId'].string}"/>
                        </c:when>
                        <c:otherwise>
                            <c:set var="versions" value="${jcr:getChildrenOfType(child, 'jnt:forgePackageVersion')}"/>
                            <c:set var="groupID" value="${'package'}"/>
                            <c:url var="downloadUrl" value="${url.server}${url.context}${url.files}${file.path}" context="/"/>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${child.properties.published.boolean}">
                        <c:remove var="iconFolder"/>
                        <c:remove var="icon"/>

                        <jcr:node var="iconFolder" path="${child.path}/icon"/>
                        <c:forEach var="iconItem" items="${iconFolder.nodes}">
                            <c:set var="icon" value="${iconItem}"/>
                        </c:forEach>
                        <json:object>
                            <%-- Stable opaque id only; the absolute JCR path and jcr:primaryType are
                                 internal identifiers withheld from the anonymous feed (SECURITY-571 #55).
                                 Consumers distinguish modules from packages via groupId ("package"). --%>
                            <json:property name="id" value="${child.identifier}"/>
                            <c:url context="/" var="localUrl" value="${url.server}${child.url}">
                                <c:param name="dx" value="true"/>
                            </c:url>
                            <c:if test="${!fn:contains(localUrl, 'https') && !fn:contains(localUrl, 'localhost')}">
                                <c:set var="localUrl" value="${fn:replace(localUrl, 'http', 'https')}"/>
                            </c:if>
                            <json:property name="remoteUrl" value="${localUrl}"/>
                            <json:property name="groupId" value="${groupID}"/>
                            <json:property name="name" value="${child.name}"/>
                            <json:property name="title" value="${child.displayableName}"/>
                            <json:property name="status" value="${child.properties.status.string}"/>
                            <c:if test="${not empty icon}">
                                <json:property name="icon" value="${url.server}${icon.url}"/>
                            </c:if>
                            <json:array name="versions">
                                <c:forEach var="version" items="${versions}">
                                    <c:if test="${version.properties.published.boolean and child.properties.published.boolean}">
                                        <json:object>
                                            <json:property name="version"
                                                           value="${version.properties.versionNumber.string}"/>
                                            <json:property name="requiredVersion"
                                                           value="${version.properties.requiredVersion.node.name}"/>
                                            <c:set var="files" value="${jcr:getChildrenOfType(version, 'jnt:file')}"/>
                                            <%-- Reset per version so an unsafe-coordinate skip below can't leak the
                                                 previous version's URL into this entry. --%>
                                            <c:set var="downloadUrl" value=""/>
                                            <c:if test="${empty files}">
                                                <%-- No attached artifact: the module was deployed to the site's Maven
                                                     repository, so the download is served by the MavenProxy servlet at
                                                     /modules/mavenproxy. Generate the URL from the request server + the
                                                     module coordinates instead of storing an absolute URL on the node,
                                                     so it adapts to the request's scheme / host / port / site.
                                                     CANONICAL MAVENPROXY GRAMMAR (keep these three in sync; MavenProxy.java
                                                     parses it back): {server}{context}/modules/mavenproxy/{siteKey}/
                                                     {groupId-with-dots-as-slashes}/{name}/{version}/{name}-{version}.jar
                                                     Also implemented in rss/contentFolder.moduleList.jsp and, root-relative
                                                     for the browser, in store-template src/components/forge/versions.ts. --%>
                                                <c:set var="vNum" value="${version.properties.versionNumber.string}"/>
                                                <%-- Skip the URL when a coordinate could deepen/traverse the proxy path
                                                     (".." or "/"), mirroring the SAFE_MAVEN_SEGMENT guard in versions.ts so
                                                     this feed never emits a path the proxy/storefront would reject. --%>
                                                <c:if test="${not (fn:contains(groupID, '..') or fn:contains(groupID, '/') or fn:contains(child.name, '..') or fn:contains(child.name, '/') or fn:contains(vNum, '..') or fn:contains(vNum, '/'))}">
                                                    <c:set var="downloadUrl" value="${url.server}${url.context}/modules/mavenproxy/${currentNode.resolveSite.siteKey}/${fn:replace(groupID, '.', '/')}/${child.name}/${vNum}/${child.name}-${vNum}.jar"/>
                                                </c:if>
                                            </c:if>
                                            <c:if test="${not empty files}">
                                                <c:forEach var="file" items="${files}">
                                                    <c:url var="downloadUrl" value="${url.server}${url.context}${url.files}${file.path}" context="/"/>
                                                </c:forEach>
                                            </c:if>
                                            <json:property name="downloadUrl" value="${downloadUrl}"/>
                                        </json:object>
                                    </c:if>
                                </c:forEach>
                            </json:array>
                        </json:object>
                    </c:if>
                </c:if>
            </c:forEach>
        </json:array>
    </json:object>
</json:array>