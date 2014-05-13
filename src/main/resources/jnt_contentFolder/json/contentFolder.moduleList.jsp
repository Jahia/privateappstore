<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="json" uri="http://www.atg.com/taglibs/json" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<json:array>
    <json:object>
        <json:property name="id" value="${currentNode.identifier}"/>
        <json:property name="path" value="${currentNode.path}"/>
        <json:property name="name" value="${currentNode.name}"/>
        <json:property name="title" value="${currentNode.displayableName}"/>
        <json:array name="modules">
            <c:forEach items="${jcr:getDescendantNodes(currentNode, 'jnt:forgeModule')}" var="child">
                <c:if test="${child.properties.published.boolean}">
                    <c:remove var="iconFolder"/>
                    <c:remove var="icon"/>

                    <jcr:node var="iconFolder" path="${child.path}/icon"/>
                    <c:forEach var="iconItem" items="${iconFolder.nodes}">
                        <c:set var="icon" value="${iconItem}"/>
                    </c:forEach>
                    <json:object>
                        <json:property name="id" value="${child.identifier}"/>
                        <json:property name="path" value="${child.path}"/>
                        <c:url context="/" var="localUrl" value="${url.server}${child.url}"/>
                        <json:property name="remoteUrl" value="${localUrl}"/>
                        <json:property name="groupId" value="${child.properties['groupId'].string}"/>
                        <json:property name="name" value="${child.name}"/>
                        <json:property name="title" value="${child.displayableName}"/>
                        <c:if test="${not empty icon}">
                            <json:property name="icon" value="${url.server}${icon.url}"/>
                        </c:if>
                        <json:array name="versions">
                            <c:forEach var="version" items="${jcr:getChildrenOfType(child, 'jnt:forgeModuleVersion')}">
                                <c:if test="${version.properties.published.boolean and child.properties.published.boolean}">
                                    <json:object>
                                        <json:property name="version"
                                                       value="${version.properties.versionNumber.string}"/>
                                        <json:property name="requiredVersion"
                                                       value="${version.properties.requiredVersion.node.name}"/>
                                        <json:property name="downloadUrl" value="${version.properties.url.string}"/>
                                    </json:object>
                                </c:if>
                            </c:forEach>
                        </json:array>
                    </json:object>
                </c:if>
            </c:forEach>
        </json:array>
    </json:object>
</json:array>