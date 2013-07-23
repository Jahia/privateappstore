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
            <c:forEach items="${jcr:getChildrenOfType(currentNode, 'jnt:forgeModule')}" var="child">
                <json:object>
                    <json:property name="id" value="${child.identifier}"/>
                    <json:property name="path" value="${child.path}"/>
                    <json:property name="name" value="${child.name}"/>
                    <json:property name="title" value="${child.displayableName}"/>
                    <c:forEach var="version" items="${jcr:getChildrenOfType(child, 'jnt:forgeModuleVersion')}">
                        <c:if test="${version.properties.activeVersion.boolean}">
                            <json:property name="version" value="${version.properties.versionNumber.string}"/>
                            <json:property name="downloadUrl" value="${version.properties.url.string}"/>
                        </c:if>
                    </c:forEach>
                </json:object>
            </c:forEach>
        </json:array>
    </json:object>
</json:array>