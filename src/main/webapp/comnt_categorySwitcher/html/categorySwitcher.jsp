<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="jquery.min.js"/>

<c:set var="bindedComponent"
       value="${uiComponents:getBindedComponent(currentNode, renderContext, 'j:bindedComponent')}"/>

<script type="text/javascript">
    var category ="";

        function changeValue(selectList){
             var selectedValue = $(selectList).find(":selected").val();
             category = selectedValue;
             var urlTocall = baseUrl + "?category="+category;
             $('#forgeContent').load(urlTocall);

        }

</script>

<select name="selection" onchange="changeValue(this);" class="selectcategories" id="selectcategories">
     <option value="none"<c:if test="${category eq 'none'}"> selected </c:if>>None</option>
     <c:forEach var="categoryItem" items="${currentNode.properties.categoryList}">
            <c:if test="${not empty categoryItem.string}">
                 <option value="${categoryItem.string}"<c:if test="${category eq categoryItem.string}"> selected </c:if>>${categoryItem.node.properties['jcr:title'].string}</option>
            </c:if>
      </c:forEach>
</select>