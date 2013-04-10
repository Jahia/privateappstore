<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="uiComponents" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="currentUser" type="org.jahia.services.usermanager.JahiaUser"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="css" resources="review.css,ui.stars.css"/>
<template:addResources type="javascript" resources="jquery.min.js,jquery.validate.js,ui.stars.js"/>

<c:set var="id" value="${currentNode.identifier}"/>
<c:set var="commentMandatory" value="${currentNode.properties['commentMandatory'].boolean}"/>
<c:set var="commentMinLength" value="${currentNode.properties['commentMinLength'].long}"/>
<c:set var="boundComponent"
       value="${uiComponents:getBindedComponent(currentNode, renderContext, 'j:bindedComponent')}"/>

<c:if test="${not empty boundComponent}">

    <script type="text/javascript">

        $(document).ready(function(){

            $("#writeReviewForm").validate({
                rules: {
                    'j:lastVote': {
                        required: true,
                        min: 1,
                        max: 5
                    },
                    'jcr:title': {
                        required: "#reviewComment-${boundComponent.identifier}:filled"
                    },
                    <c:if test="${commentMandatory}">
                    'content': {
                        required: true
                        <c:if test="${not empty commentMinLength}">
                        ,minlength: ${commentMinLength}
                        </c:if>
                    },
                    </c:if>
                    <c:if test="${not renderContext.loggedIn}">
                    pseudo: "required",
                    captcha: "required"
                    </c:if>
                },
                submitHandler: function(form) {
                    $('#reviewRating-${boundComponent.identifier}').find('input[name="j:lastVote"]').removeAttr("disabled");
                    form.submit();
                }
            });

            $("#reviewRating-${boundComponent.identifier}").stars({
                inputType: "select",
                cancelShow: false,
                disableValue: false
            });

            $("#writeReviewToggle").click(function(){
                $("#writeReviewForm").slideToggle();
                $(this).toggleClass("toggleButtonEnabled");
            });

        });

    </script>

    <section id="writeReview">

        <span id="writeReviewToggle"><fmt:message key="jnt_addReview.label.writeReview"/></span>

        <template:tokenizedForm>
            <form action="<c:url value='${url.base}${boundComponent.path}.chain.do'/>" method="post" id="writeReviewForm">
                <input type="hidden" name="jcrNodeType" value="jnt:review"/>
                <input type="hidden" name="chainOfAction" value="addReview,rate"/>

                <fieldset>
                    <c:if test="${not renderContext.loggedIn}">
                        <p class="field">
                            <label for="reviewPseudo-${boundComponent.identifier}"><fmt:message key="jnt_review.label.pseudo"/></label>
                            <input value="${sessionScope.formDatas['pseudo'][0]}"
                                   type="text" size="35" name="pseudo" id="reviewPseudo-${boundComponent.identifier}"
                                   tabindex="1"/>
                        </p>
                    </c:if>
                    <p class="field" id="reviewRating-${boundComponent.identifier}">
                        <span><fmt:message key="jnt_review.label.rating"/></span>
                        <select name="j:lastVote">
                            <option value="1"><fmt:message key="jnt_review.label.rating.poor"/></option>
                            <option value="2"><fmt:message key="jnt_review.label.rating.fair"/></option>
                            <option value="3"><fmt:message key="jnt_review.label.rating.average"/></option>
                            <option value="4"><fmt:message key="jnt_review.label.rating.good"/></option>
                            <option value="5"><fmt:message key="jnt_review.label.rating.excellent"/></option>
                        </select>
                    </p>
                    <p class="field">
                        <label class="left" for="reviewTitle-${boundComponent.identifier}"><fmt:message key="jnt_review.label.title"/></label>
                        <input class="" value="${sessionScope.formDatas['jcr:title'][0]}"
                               type="text" size="35" id="reviewTitle-${boundComponent.identifier}" name="jcr:title"
                               tabindex="1"/>
                    </p>

                    <p class="field">
                        <label class="left" for="reviewComment-${boundComponent.identifier}">
                            <fmt:message key="jnt_review.label.body"/>
                            <c:if test="${commentMandatory}">
                                <fmt:message key="jnt_review.label.body.optional"/>
                            </c:if>
                        </label>
                        <textarea rows="7" cols="35" id="reviewComment-${boundComponent.identifier}"
                                  name="content"
                                  tabindex="2"><c:if test="${not empty sessionScope.formDatas['content']}">
                                  ${fn:escapeXml(sessionScope.formDatas['content'][0])}
                        </c:if></textarea>
                    </p>

                    <c:if test="${not renderContext.loggedIn}">
                        <p class="field">
                            <label class="left" for="captcha"><fmt:message key="jnt_review.label.captcha"/></label>
                            <template:captcha/>
                            <c:if test="${not empty sessionScope.formError}">
                                <label class="error">${fn:escapeXml(sessionScope.formError)}</label>
                            </c:if>
                        </p>
                        <p class="field">
                            <label class="left" for="captcha"><fmt:message key="jnt_review.label.captcha.enter"/></label>
                            <input type="text" id="captcha" name="jcrCaptcha"/>
                        </p>
                    </c:if>

                    <p>
                        <input type="submit" value="<fmt:message key='jnt_review.label.submit'/>" class="button"
                               tabindex="4"  ${disabled} onclick=""/>
                    </p>
                </fieldset>
            </form>

        </template:tokenizedForm>

    </section>

</c:if>

