<%@ page language="java" contentType="text/html; charset=UTF-8" %>

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
<fmt:setBundle basename="MessageResources"/>
<cosmo:standardLayout prefix="Account.PasswordRecover." showNav="false">
<cosmo:staticbaseurl var="staticBaseUrl"/>
<script type="text/javascript">
dojo.require("cosmo.ui.widget.PasswordRecoverer");
</script>

<style type="text/css">
#passwordRecoverer{
	margin-left: auto;
	margin-right: auto;
	text-align: center;
}
</style>

<div class="mainInfoBox">

<div dojoType="cosmo:PasswordRecoverer" widgetId="passwordRecoverer" displayDefaultInfo="true">

</div>

</div>


</cosmo:standardLayout>