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
<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="${msg.severity == 'ERROR' ? 'validationError' : ''} alert ${msg.severity == 'ERROR' ? 'alert-error' : 'alert-success'}"><button type="button" class="close" data-dismiss="alert">&times;</button>${fn:escapeXml(msg.text)}</div>
</c:forEach>
<div class="box-1">
    <form:form modelAttribute="forgeSettings" cssClass="form" autocomplete="off">
        <h3>Forge settings</h3>
        <div class="container-fluid">
            <div class="row-fluid">
                <div class="span8">
                    <label for="url">maven repository URL</label>
                    <form:input class="span12" type="text" id="url" path="url"/>
                </div>
            </div>
            <div class="row-fluid">
                <div class="span4">
                    <label for="user">user</label>
                    <form:input class="span12" type="text" id="user" path="user"/>
                </div>
                <div class="span4">
                    <label for="password">password</label>
                    <form:password class="span12" type="password" id="password" path="password"/>
                </div>
                <div class="span4">
                    <label for="passwordConfirm">confirm password</label>
                    <form:password class="span12" type="password" id="passwordConfirm" path="passwordConfirm"/>
                </div>
            </div>
            <div class="row-fluid">
                <div class="span4">
                    <button class="btn btn-primary" id="submit" type="submit" name="_eventId_submit"><i class="icon-ok icon-white"></i>&nbsp;<fmt:message key='label.save'/></button>
                </div>
            </div>
        </div>
    </form:form>
</div>
