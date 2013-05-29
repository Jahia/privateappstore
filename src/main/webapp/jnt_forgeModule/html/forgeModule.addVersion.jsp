<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="acl" type="java.lang.String"--%>

<template:addResources type="javascript" resources="jquery.min.js,jquery.validate.js"/>
<template:addResources type="javascript" resources="select2.js"/>
<template:addResources type="css" resources="select2.css, select2-bootstrap.css"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="hasModuleVersions" value="${jcr:hasChildrenOfType(currentNode, 'jnt:forgeModuleVersion')}"/>

<uiComponents:ckeditor selector="jahia-moduleVersion-changeLog-${id}"/>

<template:addResources type="inlinejavascript">
    <script type="text/javascript">

        $(document).ready(function() {

            jQuery.validator.addMethod("regexp", function(value, element, param) {
                return this.optional(element) || param.test(value);
            });

            var validator = $("#moduleVersionForm-${id}").validate({
                rules: {

                    'versionNumber': {
                        required: true,
                        regexp: /^(\d+\.){3}(\d+)$/i
                    },

                    'moduleVersionBinary': {
                        required: true,
                        regexp: /(\S+?)\.(jar|war)$/i
                    },

                    'changeLog': {
                        required: true,
                        minlength: 50
                    },

                    'requiredVersion': {
                        required: true
                    }
                },
                messages: {

                    'versionNumber': {
                        required: "<fmt:message key='forge.label.askVersionNumber'/>",
                        regexp: "<fmt:message key='forge.label.askValidVersionNumber'/>"
                    },

                    'moduleVersionBinary': {
                         required: "<fmt:message key='forge.label.askmoduleVersionBinary'/>",
                         regexp:"<fmt:message key='forge.label.askValidmoduleVersionBinary'/>"
                     },

                    'changeLog': {
                        required: "<fmt:message key='forge.label.askChangeLog'/>",
                        minlength: "<fmt:message key='forge.label.changeLogSizeWarning'/>"
                    },

                    'requiredVersion': {
                        required: "<fmt:message key='forge.label.askRequiredVersion'/>"
                    }
                },
                <%--submitHandler: function(form) {
                    $.post('<c:url value='${url.base}${currentNode.path}.addModuleVersion.do'/>',$(form).serialize(), function(data) {
                        if(data['error'] == "versionNumber") {
                            validator.showErrors({versionNumber:"<fmt:message key="jnt_forgeModuleVersion.label.versionNumberAlreadyUsed"/>"});
                        }
                    }, "json");
                },  --%>
                highlight: function(element, errorClass, validClass) {
                    $(element).addClass("error").removeClass(validClass).parents('.control-group').addClass("error");
                },
                unhighlight: function(element, errorClass, validClass) {
                    $(element).removeClass("error").addClass(validClass).parents('.control-group').removeClass("error");
                }
            });

            $('#requiredVersion').select2();
        });

    </script>
</template:addResources>

<template:tokenizedForm>
    <form action="<c:url value='${url.base}${currentNode.path}.addModuleVersion.do'/>" method="post" id="moduleVersionForm-${id}" enctype="multipart/form-data">
        <fieldset>
            <input type="hidden" name="jcrNormalizeNodeName" value="true"/>

            <div class="control-group">
                <label class="control-label" for="versionNumber"><fmt:message key="jnt_forgeModuleVersion.versionNumber"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="jnt_forgeModuleVersion.versionNumber" />" class="span16" type="text"
                           name="versionNumber" id="versionNumber"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="moduleVersionBinary"><fmt:message key="jnt_forgeModuleVersion.moduleFile"/></label>
                <div class="controls">
                    <input placeholder="<fmt:message key="jnt_forgeModuleVersion.moduleFile" />" class="span16" type="file"
                           name="moduleVersionBinary" id="moduleVersionBinary"/>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="jahia-moduleVersion-changeLog-${id}"><fmt:message key="jnt_forgeModuleVersion.changeLog"/></label>
                <div class="controls">
                    <textarea rows="7" cols="35" id="jahia-moduleVersion-changeLog-${id}" class="jahia-ckeditor span16"
                        placeholder="<fmt:message key="jnt_forgeModuleVersion.changeLog" />" name="changeLog"></textarea>
                </div>
            </div>

            <jcr:node var="requiredVersions" path="${renderContext.site.path}/contents/forge-modules-required-versions"/>

            <div class="control-group">
                <label class="control-label" for="requiredVersion"><fmt:message key="jnt_forgeModuleVersion.requiredVersion"/></label>
                <div class="controls">
                    <select name="requiredVersion" id="requiredVersion" >
                        <c:forEach items="${jcr:getNodes(requiredVersions, 'jnt:contentFolder')}" var="requiredVersionsFolder">
                            <optgroup label="${requiredVersionsFolder.displayableName}">
                                <c:forEach items="${jcr:getNodes(requiredVersionsFolder, 'jnt:text')}" var="requiredVersion">
                                    <option value="${requiredVersion.identifier}">${requiredVersion.displayableName}</option>
                                </c:forEach>
                            </optgroup>
                        </c:forEach>
                    </select>
                </div>
            </div>

            <div class="control-group">
                <label class="control-label" for="releaseType"><fmt:message key="jnt_forgeModuleVersion.releaseType"/></label>
                <div class="controls">
                    <select name="releaseType" id="releaseType" >
                        <option value="hotfix"> hotfix </option>
                        <option value="service-pack"> service-pack </option>
                        <option value="upgrade"> upgrade </option>
                    </select>
                </div>
            </div>

            <jcr:node var="jahiaVersionCategory" path="/sites/systemsite/categories/forge-categories/status"/>
            <c:if test="${not empty jahiaVersionCategory.node}">
                <div class="control-group">
                    <label class="control-label" for="status"><fmt:message key="jnt_forgeModule.status"/></label>
                    <div class="controls">
                        <input type="text" id="status" name="jahiAppStatus" value=""/>
                        <input type="text" id="categoryFieldDisplay2" name="categoryFieldDisplay2" readonly="readonly" />
                        <uiComponents:treeItemSelector fieldId="jahiAppStatus" displayFieldId="categoryFieldDisplay2" nodeTypes="jnt:category"
                            selectableNodeTypes="jnt:category" root="/sites/systemsite/categories/forge-categories/status"
                            includeChildren="false" displayIncludeChildren="false" valueType="identifier" />
                    </div>
                </div>
            </c:if>

            <div class="control-group">
                <label class="control-label" for="activeVersion"><fmt:message key="jnt_forgeModuleVersion.activeVersion"/></label>
                <div class="controls">
                    <input type="checkbox" id="activeVersion" name="activeVersion" checked="true" ${hasModuleVersions ? '' : 'disabled'}/>
                </div>
            </div>


            <div class="control-group">
                <div class="controls">
                    <input type="submit" class="btn" value="<fmt:message key="forge.label.submit" />"/>
                </div>
            </div>

        </fieldset>

    </form>
</template:tokenizedForm>
