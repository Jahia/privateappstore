<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="acl" type="java.lang.String"--%>

<template:addResources type="javascript" resources="html5shiv.js,jquery.min.js,jquery.validate.js"/>
<template:addResources type="css" resources="forge.css"/>
<template:addResources type="javascript" resources="bootstrap-editable.js, wysihtml5-0.3.0.js, bootstrap-wysihtml5.js, wysihtml5.js"/>
<template:addResources type="css" resources="bootstrap-editable.css, forge.edition.css, wysiwyg-color.css"/>
<c:set var="id" value="${currentNode.identifier}"/>
<jcr:nodeProperty node="${currentNode}" name='modulesRepository' var="modulesRepository"/>
<c:set var="modulesRepositoryPath" value="${url.base}${renderContext.mainResource.node.resolveSite.path}/contents/forge-modules-repository"/>
<%--<uiComponents:ckeditor selector="jahia-forge-module-description-${id}"/>--%>

<template:addResources type="inlinejavascript">
    <script type="text/javascript">

        $(document).ready(function() {

            $('#description-${id}').wysihtml5({
                "font-styles": true, //Font styling, e.g. h1, h2, etc. Default true
                "emphasis": true, //Italics, bold, etc. Default true
                "lists": true, //(Un)ordered lists, e.g. Bullets, Numbers. Default true
                "html": false, //Button which allows you to edit the generated HTML. Default false
                "link": false, //Button to insert a link. Default true
                "image": false, //Button to insert an image. Default true,
                "color": false //Button to change color of font
            });

            <c:if test="${renderContext.liveMode}">

                // Set JQuery to traditional
                $.ajaxSetup({traditional: true, cache:false});

                jQuery.validator.addMethod("regexp", function(value, element, param) {
                    return this.optional(element) || param.test(value);
                });

                var validator = $("#forgeModuleCreationForm-${id}").validate({

                    rules: {
                        'jcr:title': {
                            required: true,
                            regexp: /^[^"]*$/i,
                            minlength: 2
                        },
                        'description': {
                            required: true,
                            minlength: 100
                        }
                    },
                    messages: {
                        'jcr:title': {
                            required: "<fmt:message key='jnt_forgeModuleCreation.label.askTitle'/>",
                            regexp: "<fmt:message key="jnt_forgeModuleCreation.label.error.doubleQuote"/>",
                            minlength: "<fmt:message key='jnt_forgeModuleCreation.label.titleSizeWarning'/>"
                        },
                        'description': {
                            required: "<fmt:message key='jnt_forgeModuleCreation.label.askDescription'/>",
                            minlength: "<fmt:message key='jnt_forgeModuleCreation.label.descriptionSizeWarning'/>"
                        }
                    },
                    submitHandler: function(form) {
                        $.post('<c:url value='${modulesRepositoryPath}.createModule.do'/>',$(form).serialize(), function(result) {
                            if(result['error'] == "titleAlreadyUsed") {
                                validator.showErrors({'jcr:title':"<fmt:message key="jnt_forgeModuleCreation.label.error.titleAlreadyUse"/>"});
                            }
                            else {
                                if (result['moduleUrl'] != "")
                                    window.location = result['moduleUrl'];
                            }
                        }, "json");
                    },
                    highlight: function(element, errorClass, validClass) {
                        $(element).addClass("error").removeClass(validClass).parents('.control-group').addClass("error");
                    },
                    unhighlight: function(element, errorClass, validClass) {
                        $(element).removeClass("error").addClass(validClass).parents('.control-group').removeClass("error");
                    }
                });

            </c:if>

        });

    </script>
</template:addResources>

<section class="forgeModuleCreation">

    <%--<p><fmt:message key="jnt_forgeModuleCreation.label.forgeModuleCreationSubheader"/></p>--%>

    <template:tokenizedForm>
        <form id="forgeModuleCreationForm-${id}" action="<c:url value='${modulesRepositoryPath}.createModule.do'/>" method="post">
            <fieldset>

                <input type="hidden" name="jcrNormalizeNodeName" value="true"/>
                <input type="hidden" name="allowsMultipleSubmits" value="true"/>

                <div class="control-group">
                    <label class="control-label" for="jcr:title"><fmt:message key="jnt_forgeModule.label.title"/></label>
                    <div class="controls">
                        <input placeholder="<fmt:message key="jnt_forgeModule.label.title" />" type="text"
                               name="jcr:title" id="jcr:title"/>
                    </div>
                </div>

                <div class="control-group">
                    <label class="control-label" for="description-${id}"><fmt:message key="jnt_forgeModule.label.description"/></label>
                    <div class="controls">
                        <textarea id="description-${id}" name="description" rows="7" cols="35" class="span16"></textarea>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls">
                        <input type="submit" class="btn btn-primary" value="<fmt:message key="jnt_forgeModuleCreation.label.submit" />"
                               <c:if test="${not renderContext.liveMode}">disabled</c:if>/>
                    </div>
                </div>

            </fieldset>
        </form>
    </template:tokenizedForm>

</section>
