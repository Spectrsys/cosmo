<%@ include file="/WEB-INF/jsp/taglibs.jsp"  %>
<%@ include file="/WEB-INF/jsp/tagfiles.jsp" %>

<tiles:importAttribute name="body"/>
<tiles:importAttribute name="prefix"/>
<tiles:importAttribute name="showNav" ignore="true"/>

<c:if test="${empty showNav}">
  <c:set var="showNav" value="true"/>
  <cosmo-core:user var="user"/>
</c:if>

<html:html xhtml="true">
  <head>
    <title>
      <fmt:message key="${prefix}HeadTitle">
        <c:forEach var="p" items="${TitleParam}">
          <fmt:param value="${p}"/>
        </c:forEach>
      </fmt:message>
    </title>
    <link rel="stylesheet" type="text/css"
          href="<html:rewrite page="/cosmo.css"/>"/>
    <script type="text/javascript"
            src="<html:rewrite page="/cosmo.js"/>"></script>
  </head>
  <body class="bodystyle">
    <table border="0" cellpadding="0" cellspacing="0" width="100%">
      <tr>
        <td align="left" valign="top">
          <div class="lg">
              <c:choose><c:when test="${fn:endsWith(body, '/welcome.jsp') || fn:endsWith(body, '/login.jsp')}"><b><fmt:message key="Layout.Title"/></b></c:when><c:otherwise><html:link page="/"><b><fmt:message key="Layout.Title"/></b></html:link></c:otherwise></c:choose>
          </div>
        </td>
        <c:if test="${showNav}">
          <td align="right" valign="top">
            <!-- main navbar -->
            <div class="md">
              <fmt:message key="Layout.Nav.Main.LoggedInAs">
                <fmt:param value="${user.username}"/>
              </fmt:message>
              |
              <authz:authorize ifAllGranted="ROLE_ROOT">
                <html:link page="/home/"><fmt:message key="Layout.Nav.Main.Shares"/></html:link>
              |
              </authz:authorize>
              <authz:authorize ifAllGranted="ROLE_USER">
                <html:link page="/home/${user.username}/"><fmt:message key="Layout.Nav.Main.Home"/></html:link>
              |
              </authz:authorize>
              <html:link page="/logout">
                <fmt:message key="Layout.Nav.Main.LogOut"/>
              </html:link>
              |
              <a href="mailto:${applicationScope.cosmoServerAdmin}">
                <fmt:message key="Layout.Nav.Main.Help"/>
              </a>
            </div>
            <!-- end main navbar -->
          </td>
        </c:if>
      </tr>
    </table>
    <hr noshade="noshade"/>
    <c:choose>
      <c:when test="${showNav}">
        <authz:authorize ifAllGranted="ROLE_ROOT">
          <!-- admin console navbar -->
          <div class="md">
            <fmt:message key="Layout.Nav.Console.Label"/>
            <c:choose><c:when test="${fn:endsWith(body, '/user/list.jsp')}"><b><fmt:message key="Layout.Nav.Console.Users"/></b></c:when><c:otherwise><html:link page="/users"><fmt:message key="Layout.Nav.Console.Users"/></html:link></c:otherwise></c:choose>
            <!-- end admin console navbar -->
          </div>
          <hr noshade="noshade"/>
        </authz:authorize>
      </c:when>
      <c:otherwise>
      </c:otherwise>
    </c:choose>
    <div class="md">
      <!-- page body -->
      <tiles:insert attribute="body" flush="false"/>
      <!-- end page body -->
    </div>
    <!-- footer -->
    <html:img page="/spacer.gif" width="1" height="60" alt=""
              border="0" styleId="footerSpacer"/>
    <hr noshade="noshade"/>
    <div class="footer">
      <a href="mailto:${applicationScope.cosmoServerAdmin}">
        <fmt:message key="Layout.Footer">
          <fmt:param value="${applicationScope.cosmoVersion}"/>
        </fmt:message>
      </a>
      <jsp:useBean id="now" class="java.util.Date"/>
      &nbsp;&nbsp;&nbsp;
      <fmt:formatDate value="${now}" type="both"/>
    </div>
    <script language="JavaScript" type="text/javascript">
      setFoot();
    </script>
    <!-- end footer -->
  </body>
</html:html>
