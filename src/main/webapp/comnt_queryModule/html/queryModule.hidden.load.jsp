<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>

<c:if test="${not empty param.criteria}">
    <c:set var="criteria" scope="session" value="${param.criteria}"/>
</c:if>

<c:if test="${not empty param.subView}">
    <c:set var="subView" scope="session" value="${param.subView}"/>
</c:if>

<c:if test="${not empty param.category}">
    <c:set var="category" scope="session" value="${param.category}"/>
</c:if>

<jcr:nodeProperty node="${currentNode}" name='startNode' var="startNode"/>

<c:choose>
    <c:when test="${criteria eq 'new'}">
        <c:choose>
            <c:when test="${!empty category and category != 'none'}">
                <c:set var="statement" value="SELECT * FROM [comnt:module] AS module WHERE ISDESCENDANTNODE(module,'${not empty startNode and not empty startNode.node ? startNode.node.path : renderContext.site.path}') AND module.[deleted]=false AND module.[new]=true AND module.[j:defaultCategory]='${category}' ORDER BY module.[jcr:created] desc "/>
            </c:when>
            <c:otherwise>
                <c:set var="statement" value="SELECT * FROM [comnt:module] AS module WHERE ISDESCENDANTNODE(module,'${not empty startNode and not empty startNode.node ? startNode.node.path : renderContext.site.path}') AND module.[deleted]=false AND module.[new]=true ORDER BY module.[jcr:created] desc"/>
            </c:otherwise>
        </c:choose>
    </c:when>
    <%--<c:when test="${criteria eq 'top'}">
        @TODO
    </c:when>--%>
    <c:otherwise>
        <c:choose>
            <c:when test="${!empty category and category != 'none'}">
                 <c:set var="statement" value="SELECT * FROM [comnt:module] AS module WHERE ISDESCENDANTNODE(module,'${not empty startNode and not empty startNode.node ? startNode.node.path : renderContext.site.path}') AND module.[deleted]=false AND module.[j:defaultCategory]='${category}' ORDER BY module.[jcr:created] desc"/>
            </c:when>
            <c:otherwise>
                 <c:set var="statement" value="SELECT * FROM [comnt:module] AS module WHERE ISDESCENDANTNODE(module,'${not empty startNode and not empty startNode.node ? startNode.node.path : renderContext.site.path}') AND module.[deleted]=false ORDER BY module.[jcr:created] desc"/>
            </c:otherwise>
        </c:choose>
    </c:otherwise>
</c:choose>


<query:definition var="listQuery" statement="${statement}"  />

<c:set target="${moduleMap}" property="editable" value="false" />
<c:set target="${moduleMap}" property="emptyListMessage" value="No module found" />
<c:set target="${moduleMap}" property="listQuery" value="${listQuery}" />

<c:choose>
    <c:when test="${empty param.pagesize}">
        <c:set target="${moduleMap}" property="nbOfItemsPerPage" value="${currentNode.properties.nbOfModulePerPage.long - 1}" />
    </c:when>
    <c:otherwise>
        <c:set target="${moduleMap}" property="nbOfItemsPerPage" value="${param.pagesize - 1}" />
    </c:otherwise>
</c:choose>

<c:choose>
    <c:when test="${subView eq 'list'}">
           <c:set target="${moduleMap}" property="subNodesView" value="list" />
    </c:when>

    <c:otherwise>
            <c:set target="${moduleMap}" property="subNodesView" value="default" />
    </c:otherwise>
</c:choose>

