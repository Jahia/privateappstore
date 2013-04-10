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

<c:set var="worstRating" value="1"/>
<c:set var="bestRating" value="5"/>
<c:set var="ratingNbr" value="${fn:length(jcr:getNodes(currentNode, 'jnt:review'))}"/>
<c:set var="barWidth" value="200"/>
<c:set var="bindedComponent" value="${currentNode.parent}"/>

<c:forEach var="i" begin="${worstRating}" end="${bestRating}" step="1">

    <c:set var="j" value="${bestRating - i + worstRating}"/>

    <jcr:sql var="queryResult" sql="SELECT count AS [rep:count(skipChecks=1)] FROM [jnt:review] WHERE isdescendantnode(['${currentNode.path}'])
              AND rating = ${j}" />

    <c:forEach var="ratings" items="${queryResult.rows}">
        <c:set var="ratingCount" value="${ratings.values[0].long}"/>
    </c:forEach>

    <div class="ratingsSplitItem">

        <span class="ratingValue">${j}&nbsp;
            <c:choose>
                <c:when test="${j eq 0}">
                    <fmt:message key="jnt_reviewsList.label.ratingsSplit.star"/>
                </c:when>
                <c:otherwise>
                    <fmt:message key="jnt_reviewsList.label.ratingsSplit.stars"/>
                </c:otherwise>
            </c:choose>
        </span>
        <span class="ratingPercentage ratingPercentage-${j}" style="width: ${barWidth * ratingCount/ratingNbr}px;">&nbsp;</span>
        <span class="ratingCount">${ratingCount}</span><br/>

    </div>

</c:forEach>

<c:if test="${jcr:isNodeType(bindedComponent, 'jmix:rating')}">

    <c:set var="averageRating" value="${bindedComponent.properties['j:sumOfVotes'].long/bindedComponent.properties['j:nbOfVotes'].long}"/>

    <div class="averageRating">

        <h4><fmt:message key="jnt_reviewsList.label.ratingsSplit.average"/></h4>
        <span class="averageRatingValue"><fmt:formatNumber value="${averageRating}" maxFractionDigits="1"/></span>
        <%-- TODO

                <template:module node="${bindedComponent}" view="hidden.average.readonly" />

        --%>

        <template:module path="${bindedComponent.path}" view="hidden.average.readonly" />

    </div>

    <div class="clear"></div>

</c:if>