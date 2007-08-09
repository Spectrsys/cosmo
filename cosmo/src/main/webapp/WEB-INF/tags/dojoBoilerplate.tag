<%--
/*
 * Copyright 2005-2006 Open Source Applications Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
--%>

<%@ include file="/WEB-INF/jsp/taglibs.jsp"  %>
<%@ include file="/WEB-INF/jsp/tagfiles.jsp" %>
<cosmo:staticbaseurl var="staticBaseUrl"/>
<cosmo:baseurl var="baseUrl"/>

<%@ attribute name="timezones"        %>
<%@ attribute name="parseWidgets"        %>
<%@ attribute name="searchIds"        %>

<u:bind var="PRODUCT_VERSION"
        type="org.osaf.cosmo.CosmoConstants"
        field="PRODUCT_VERSION"/>
        
<fmt:setBundle basename="PimMessageResources"/>
<fmt:message key="App.TemplateName" var="templateName"/>
        

<c:if test="${empty timezones}">
  <c:set var="timezones" value="false"/>
</c:if>

<c:if test="${empty parseWidgets}">
  <c:set var="parseWidgets" value="true"/>
</c:if>

<script type="text/javascript">

    // Set isDebug to true to get nice dojo debugging messages.

    var searchIds = [<c:forEach var="searchId" items="${searchIds}" varStatus="status"><c:if test='${status.count != 1}'>,</c:if>
                     "${searchId}"</c:forEach>];

    var djConfig = {isDebug: false, 
                    staticBaseUrl: "${staticBaseUrl}",
                    i18nLocation: "${baseUrl}/i18n.js",
                    confLocation: "${baseUrl}/webui.conf",
                    templateName: "${templateName}",
                    parseWidgets: ${parseWidgets},
                    searchIds: searchIds}

</script>

<script type="text/javascript" src="${baseUrl}/js-${PRODUCT_VERSION}/lib/dojo/dojo.js"></script>
<script type="text/javascript">

function bootstrap(){
    dojo.require("dojo.widget.*");
    dojo.require("dojo.debug.console");

    dojo.registerNamespaceManifest("cosmo", "../../cosmo", "cosmo", "cosmo.ui.widget",null);
    dojo.widget.manager.registerWidgetPackage("cosmo.ui.widget");

    dojo.require("cosmo.env");
    dojo.require("cosmo.ui.conf");

    cosmo.env.setBaseUrl("${baseUrl}");
    cosmo.env.setStaticBaseUrlTemplate("${cosmoui:getStaticHostUrlTemplate()}");
    cosmo.env.setStaticBaseUrlRange("${cosmoui:getStaticHostUrlRange()}");
    cosmo.env.setVersion("${PRODUCT_VERSION}");
    <%-- 
      Note: It is possible to set this value to negative numbers --
      Setting the canonical client-side value with a function ensures
      we end up with reasonable numbers in getTimeoutSeconds and getTimeoutMinutes
    --%>
    cosmo.env.setTimeoutSeconds(
        cosmo.ui.conf.uiTimeout ||
        <%=session.getMaxInactiveInterval()%>);

    dojo.require("cosmo.ui.widget.Debug");

    if (${timezones}){
        dojo.require("cosmo.datetime.timezone.LazyCachingTimezoneRegistry");
        var registry = new cosmo.datetime.timezone.LazyCachingTimezoneRegistry("${staticBaseUrl}/js-${PRODUCT_VERSION}/lib/olson-tzdata/");
        cosmo.datetime.timezone.setTimezoneRegistry(registry);
    }
    dojo.require('cosmo.ui.conf');
}
bootstrap();

</script>

