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
<template:addResources type="javascript" resources="html5shiv.js"/>

<template:addResources type="css" resources="select2.css, select2-bootstrap.css"/>

<c:set var="isDeveloper" value="${jcr:hasPermission(currentNode, 'jcr:write')}"/>
<c:set var="viewAsUser" value="${not empty param['viewAs'] && param['viewAs'] eq 'user'}" />


<c:set var="id" value="${currentNode.identifier}"/>


<c:if test="${isDeveloper && not viewAsUser}">

    <template:addResources type="inlinejavascript">
        <script type="text/javascript">

            $(document).ready(function() {

                <c:url var="postURL" value="${url.base}${currentNode.path}"/>
                $('#changeLog-${currentNode.identifier}').editable({

                });

            });
        </script>
    </template:addResources>

</c:if>

<template:addResources type="inlinejavascript">
    <script type="text/javascript">

        $(document).ready(function() {

            jQuery.validator.addMethod("regexp", function(value, element, param) {
                return this.optional(element) || param.test(value);
            });

            var validator = $("#moduleVersionForm-${id}").validate({
                rules: {

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

                    'moduleVersionBinary': {
                         required: "<fmt:message key='jnt_forgeModuleVersion.label.askModuleVersionBinary'/>",
                         regexp:"<fmt:message key='jnt_forgeModuleVersion.label.askValidModuleVersionBinary'/>"
                     },

                    'changeLog': {
                        required: "<fmt:message key='jnt_forgeModuleVersion.label.askChangeLog'/>",
                        minlength: "<fmt:message key='jnt_forgeModuleVersion.label.changeLogSizeWarning'/>"
                    },

                    'requiredVersion': {
                        required: "<fmt:message key='jnt_forgeModuleVersion.label.askRequiredVersion'/>"
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
<c:if test="${isDeveloper && not viewAsUser}">

    <div data-original-title="<fmt:message key="jnt_forgeModuleVersion.label.changeLog"/>" data-toggle="manual" data-name="changeLog" data-type="wysihtml5"
    data-pk="1" id="changeLog-${currentNode.identifier}" class="editable">
</c:if>
<template:tokenizedForm>
    <form action="<c:url value='${url.base}${currentNode.path}.addModuleVersion.do'/>" method="post" id="moduleVersionForm-${id}" enctype="multipart/form-data">
        <fieldset>
            <input type="hidden" name="jcrNormalizeNodeName" value="true"/>

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
                                    <option value="${requiredVersion.identifier}">${requiredVersion.properties['text'].string}</option>
                                </c:forEach>
                            </optgroup>
                        </c:forEach>
                    </select>
                </div>
            </div>

            <div class="control-group">
                <div class="controls">
                    <input type="submit" class="btn" value="<fmt:message key="jnt_forgeModuleVersion.label.submit" />"/>
                    <a class="btn btnBack" href="${currentNode.url}" target="_self">
                        <fmt:message key="jnt_forgeModule.label.developer.cancel"/>
                    </a>
                </div>
            </div>

        </fieldset>

    </form>
</template:tokenizedForm>
