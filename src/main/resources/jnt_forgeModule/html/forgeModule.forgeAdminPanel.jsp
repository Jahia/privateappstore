<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
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

<c:set var="isForgeAdmin" value="${jcr:hasPermission(renderContext.site, 'jahiaForgeModerateModule')}"/>

<c:if test="${isForgeAdmin}">

    <c:set var="id" value="${currentNode.identifier}"/>
    <c:set var="reviewedByJahia" value="${currentNode.properties['reviewedByJahia'].boolean}"/>
    <c:set var="supportedByJahia" value="${currentNode.properties['supportedByJahia'].boolean}"/>

    <template:addResources type="inlinejavascript">

        <script type="text/javascript">

            $(document).ready(function() {

                $('#moduleForgeAdminPanel').find('.forgeAdminBtn').click(function() {

                    var btn = $(this);
                    var dataName = btn.attr('data-name');
                    var dataValue = !(btn.attr('data-value') === 'true');
                    var data = {};

                    data[dataName] = dataValue;
                    data['jcrMethodToCall'] = 'put';

                    $.post('<c:url value='${url.base}${currentNode.path}'/>', data, function(result) {
                        btn.toggleClass('btn-success btn-danger');
                        btn.attr('data-value', result[dataName]);
                    },"json")

                });

            });

        </script>

    </template:addResources>

    <section id="moduleForgeAdminPanel" ${viewAsUser ? 'class="viewAs"' : ''}>

        <h4><fmt:message key="jnt_forgeEntry.label.admin.title"/></h4>

        <div class="btn-group">
            <button class="forgeAdminBtn btn btn-small ${reviewedByJahia ? 'btn-success' : 'btn-danger'}"
                    data-value="${reviewedByJahia}" data-name="reviewedByJahia">
                <fmt:message key="jnt_forgeEntry.label.admin.reviewedByJahia"/>
            </button>
            <button class="forgeAdminBtn btn btn-small ${supportedByJahia ? 'btn-success' : 'btn-danger'}"
                    data-value="${supportedByJahia}" data-name="supportedByJahia">
                <fmt:message key="jnt_forgeEntry.label.admin.supportedByJahia"/>
            </button>
        </div>

    </section>

</c:if>