<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<template:addResources type="javascript" resources="tree.jquery.js"/>

<template:addResources type="css" resources="jqtree.css"/>
<template:addResources type="inline">
    <script>
        <template:include view="categoryTree" templateType="html"/>

        function deleteCategory(id) {
            $('#actionField').val('delete');
            $('#categoryIdentifierField').val(id);
            $('#submitDeleteCategorySettingForm').trigger('click');
        }
        function editCategory(id) {
            $('#actionField').val('edit');
            $('#categoryIdentifierField').val(id);
            $('#submitEditCategorySettingForm').trigger('click');
        }

    </script>
</template:addResources>

<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="${msg.severity == 'ERROR' ? 'validationError' : ''} alert ${msg.severity == 'ERROR' ? 'alert-error' : 'alert-success'}"><button type="button" class="close" data-dismiss="alert">&times;</button>${fn:escapeXml(msg.text)}</div>
</c:forEach>
<div class="box-1">
    <form:form>
        <h3><fmt:message key="jahiaForge.categories.title"/></h3>
        <div class="container-fluid">
            <fieldset>
                <legend><fmt:message key="jahiaForge.settings.rootCategory"/></legend>
            <div class="row-fluid">
                <div class="span2"><fmt:message key="jahiaForge.settings.rootCategory.selected"/></div>
                <div class="span10">${renderContext.site.properties["rootCategory"].node.displayableName}</div>
            </div>
            <div class="row-fluid">
                <div class="span2">
                    <div class="input-group">
                        <div id="tree1"></div>
                        <input type="hidden" name="rootCategoryIdentifier" id="rootCategoryField"/>
                    </div>
                </div>
                <div class="span2">
                    <button class="btn btn-primary" id="submitCategorySettingForm" type="submit" name="_eventId_submit" onclick="$('#rootCategoryField').val($('#tree1').tree('getSelectedNode').id)"><i class="icon-ok icon-white"></i>&nbsp;<fmt:message key='label.save'/></button>
                    <button class="hide" id="submitEditCategorySettingForm" type="submit" name="_eventId_edit"><i class="icon-ok icon-white"></i>&nbsp;<fmt:message key='label.save'/></button>
                    <button class="hide" id="submitDeleteCategorySettingForm" type="submit" name="_eventId_delete"><i class="icon-ok icon-white"></i>&nbsp;<fmt:message key='label.save'/></button>
                </div>
            </div>
            </fieldset>
            <fieldset>
                <legend><fmt:message key="jahiaForge.settings.categories"/></legend>
            <div class="row-fluid">
                <div class="span12">
                    <input type="hidden" name="action" id="actionField"/>
                    <input type="hidden" name="categoryIdentifier" id="categoryIdentifierField"/>
                    <c:set var="moduleCategories" value="${renderContext.site.properties['rootCategory']}"/>
                    <c:if test="${!empty renderContext.site.properties['rootCategory']}">
                        <c:forEach items="${jcr:getNodes(moduleCategories.node, 'jnt:category')}" var="moduleCategory">
                            <div class="btn-group">
                                <button type="button" class="btn btn-primary" id="${moduleCategory.displayableName}">${moduleCategory.displayableName}</button>
                                <button type="button" class="btn btn-primary" onclick="editCategory('${moduleCategory.identifier}');"><i class="icon-pencil icon-white" title="<fmt:message key="label.edit"/>"></i></button>
                                    <button type="button"   class="btn btn-primary"  onclick="deleteCategory('${moduleCategory.identifier}');"><i class="icon-trash icon-white" title="<fmt:message key="label.delete"/>"></i></button>
                            </div>
                        </c:forEach>
                    </c:if>
                </div>
            </div>
            </fieldset>
            <fieldset>
                <legend><fmt:message key="jahiaForge.settings.addCategory"/></legend>
            <div class="row-fluid">
                <div class="span12">
                    <div class="input-group">
                        <input type="text" class="form-control" name="newCategoryName" placeholder="Add new category">
                        <button class="btn btn-primary" type="submit" name="_eventId_add" onclick="$('#rootCategoryField').val($('#tree1').tree('getSelectedNode').id)"><i class="icon-ok icon-white"></i>&nbsp;<fmt:message key='label.save'/></button>
                    </div>
                </div>
            </div>
            </fieldset>
        </div>
    </form:form>
</div>
<script>
    $('#tree1').tree({
        data: data
    });
</script>