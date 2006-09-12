<%--
/*
 * Copyright 2005-2006 Open Source Applications Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
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

<tiles:importAttribute name="item"/>
<tiles:importAttribute name="path"/>
<tiles:importAttribute name="isCollection" ignore="true"/>

<c:if test="${empty isCollection}">
  <c:set var="isCollection" value="false"/>
</c:if>

<c:set var="davPath" value="/home${path}"/>
<c:set var="feedPath" value="/feed/atom/1.0${path}"/>
<c:if test="${isCollection}">
  <c:set var="davPath" value="${davPath}/"/>
</c:if>

<div class="hd" style="margin-top: 12px;">
  Tickets
</div>

<div style="margin-top:12px;">
<html:link page="/console/home/ticket${path}/new">
  [new ticket]
</html:link>
</div>

<div style="margin-top:12px;">
  <table cellpadding="4" cellspacing="1" border="0" width="100%">
    <tr>
      <td class="smTableColHead" style="width:1%;">
        &nbsp;
      </td>
      <td class="smTableColHead" style="text-align:left;">
        Id
      </td>
      <td class="smTableColHead">
        Sharing Links
      </td>
      <td class="smTableColHead">
        Owner
      </td>
      <td class="smTableColHead">
        Timeout
      </td>
      <td class="smTableColHead">
        Privileges
      </td>
      <td class="smTableColHead">
        Created
      </td>
    </tr>
    <c:forEach var="ticket" items="${item.tickets}">
    <tr>
      <td class="smTableData" style="text-align:center; white-space:nowrap;">
        <html:link page="/console/home/ticket${path}/revoke/${ticket.key}">
          [revoke]
        </html:link>
      </td>
      <td class="smTableData">
        ${ticket.key}
      </td>
      <td class="smTableData" style="text-align:center;">
        <html:link page="${davPath}?ticket=${ticket.key}">[dav]</html:link>
        <c:if test="${isCollection}">
          <html:link page="${feedPath}?ticket=${ticket.key}">[feed]</html:link>
        </c:if>
      </td>
      <td class="smTableData" style="text-align:center;">
        ${ticket.owner.username}
      </td>
      <td class="smTableData" style="text-align:center;">
        ${ticket.timeout}
      </td>
      <td class="smTableData" style="text-align:center;">
        <c:forEach var="privilege" items="${ticket.privileges}">
          ${privilege}
        </c:forEach>
      </td>
      <td class="smTableData" style="text-align:center;">
        <fmt:formatDate value="${ticket.created}" type="both"/>
      </td>
    </tr>
    </c:forEach>
  </table>
</div>
