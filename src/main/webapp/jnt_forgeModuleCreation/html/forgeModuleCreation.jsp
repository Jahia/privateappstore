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

<c:set var="id" value="${currentNode.identifier}"/>
<jcr:nodeProperty node="${currentNode}" name='modulesRepository' var="modulesRepository"/>
<c:set var="modulesRepositoryPath" value="${url.base}${modulesRepository.node.path}"/>

<uiComponents:ckeditor selector="jahia-forge-module-description-${id}"/>

<template:addResources type="inlinejavascript">
    <script type="text/javascript">

        $(document).ready(function() {

            // Set JQuery to traditional
            $.ajaxSetup({traditional: true, cache:false});

            $("#forgeModuleCreationForm-${id}").validate({

                rules: {
                    'jcr:title': {
                        required: true,
                        minlength: 2
                    },
                    'description': {
                        required: true,
                        minlength: 100
                    }
                },
                messages: {
                    'jcr:title': {
                        required: "<fmt:message key='forge.label.askTitle'/>",
                        minlength: "<fmt:message key='forge.label.titleSizeWarning'/>"
                    },
                    'description': {
                        required: "<fmt:message key='forge.label.askDescription'/>",
                        minlength: "<fmt:message key='forge.label.descriptionSizeWarning'/>"
                    }
                }
            });

        });

    </script>
</template:addResources>

<section class="forgeModuleCreation">

    <header>
        <h2><fmt:message key="jnt_forgeModule.label.forgeModuleCreationHeader"/></h2>
        <p><fmt:message key="jnt_forgeModule.label.forgeModuleCreationSubheader"/></p>
    </header>

    <template:tokenizedForm>
        <form id="forgeModuleCreationForm-${id}" action="<c:url value='${modulesRepositoryPath}.createModule.do'/>" method="post">
            <fieldset>

                <input type="hidden" name="jcrNormalizeNodeName" value="true"/>

                <div class="control-group">
                    <label class="control-label" for="jcr:title"><fmt:message key="jnt_forgeModule.label.title"/></label>
                    <div class="controls">
                        <input placeholder="<fmt:message key="jnt_forgeModule.label.title" />" type="text"
                               name="jcr:title" id="jcr:title"/>
                    </div>
                </div>

                <div class="control-group">
                    <label class="control-label" for="jahia-forge-module-description-${id}"><fmt:message key="jnt_forgeModule.label.description"/></label>
                    <div class="controls">
                        <textarea rows="7" cols="35"
                                  placeholder="<fmt:message key="jnt_forgeModule.description" />" class="jahia-ckeditor"
                                  name="description" id="jahia-forge-module-description-${id}">
                        </textarea>
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls">
                        <input type="submit" class="btn btn-primary" onclick="CKEDITOR.instances['jahia-forge-module-description-${id}'].updateElement();" value="<fmt:message key="forge.label.submit" />"/>
                    </div>
                </div>

            </fieldset>
        </form>
    </template:tokenizedForm>

</section>
