<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:include view="hidden.header"/>
<c:set var="isEmpty" value="true"/>

<template:include view="pagination.hidden" />

<table class="emboss">
    <tbody>

        <c:forEach items="${moduleMap.currentList}" var="subchild" begin="${moduleMap.begin}" end="${moduleMap.end}" varStatus="status">
            <c:choose>
                 <%--If 1st on the line--%>
                <c:when test="${(status.index+1)%3 eq 1}">
                     <tr <c:if test="${(status.index+1)%6 eq 1}">class="odd-row"</c:if>>
                         <td class="first">
                                <template:module node="${subchild}" view="${moduleMap.subNodesView}" editable="${moduleMap.editable}"/>
                         </td>
                </c:when>
                 <%--If last on the line--%>
                <c:when test="${(status.index+1)%3 eq 0}">
                         <td class="last">
                                <template:module node="${subchild}" view="${moduleMap.subNodesView}" editable="${moduleMap.editable}"/>
                         </td>
                    </tr>
                </c:when>
                <c:otherwise>
                        <td>
                                 <template:module node="${subchild}" view="${moduleMap.subNodesView}" editable="${moduleMap.editable}"/>
                        </td>
                </c:otherwise>
            </c:choose>


            <c:set var="isEmpty" value="false"/>
        </c:forEach>

    </tbody>
</table>

<template:include view="pagination.display.hidden" />


<c:if test="${not omitFormatting}"><div class="clear"></div></c:if>
<c:if test="${moduleMap.editable and renderContext.editMode}">
    <template:module path="*"/>
</c:if>
<c:if test="${not empty moduleMap.emptyListMessage and renderContext.editMode and isEmpty}">
    ${moduleMap.emptyListMessage}
</c:if>
<template:include view="hidden.footer"/>


