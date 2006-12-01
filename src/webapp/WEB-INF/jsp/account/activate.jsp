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

<cosmo:standardLayout prefix="Account.Activate." showNav="false">


<script type="text/javascript">
var ACCOUNT_ACTIVATION_URL = "/account/activate"

dojo.require("cosmo.ui.widget.AccountActivator");

var activationId = location.pathname.substring(
    location.pathname.indexOf(ACCOUNT_ACTIVATION_URL) +
    ACCOUNT_ACTIVATION_URL.length + 1
);
</script>

<div dojoType="cosmo:AccountActivator" widgetId="accountActivator"></div>



<script type="text/javascript">
/*
Initialization function for this page that will initialize the account
activator widget.

*/
dojo.addOnLoad( function(){
    var accountActivator = dojo.widget.byId("accountActivator");

    accountActivator.setActivationId(activationId);
    
    dojo.event.connect("after", accountActivator, "activateSuccess", 
    	function(){location = cosmo.env.getLoginRedirect()}
    	);



});
</script>


</cosmo:standardLayout>