/*
 * Copyright 2006 Open Source Applications Foundation
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

/**
 * To use this file, you must first provide it with the base URL for your application
 * by calling "setBaseUrl"
 * 
 * @fileoverview provides information about the Cosmo environment.
 * @author Bobby Rullo br@osafoundation.org
 * @license Apache License 2.0
 */
 
dojo.provide("cosmo.env");

cosmo.env.OVERLORD_USERNAME = "root";

//private variable for storing environment information. Do not access directly, 
//use methods below.
cosmo.env._cosmoConfig = {};
cosmo.env._cosmoConfig["baseUrl"] = djConfig['staticBaseUrl'];
cosmo.env._NULL = {};
cosmo.env._FALSE_OR_ZERO = {};
cosmo.env._getCachePropGetterPopulator = function(propName, calculatorFunction ){
   var _calcy = calculatorFunction;
   
   return  function(){
   var prop = cosmo.env._cosmoConfig[propName];
   
   if (prop){
       dojo.debug("got a cache hit: " + prop);       
       //if we don't use these placeholders, then the preceding if statement will return 
       //false, and we'll have to recalculate.
       if (prop == cosmo.env._NULL) {
           return null; 
       }
       
       if (prop == cosmo.env._FALSE_OR_ZERO) {
           return false; 
       }
              
       return prop;
   }
   
   
   prop = _calcy();
   dojo.debug("calculated property: " + prop);       

   if (!prop){
       if (prop == false) {
           cosmo.env._cosmoConfig[propName] = cosmo.env._FALSE_OR_ZERO;
       } else if (prop == null) {
           cosmo.env._cosmoConfig[propName] = cosmo.env._NULL;
       }     
   } else {
       cosmo.env._cosmoConfig[propName] = prop;
   } 
   
   return prop;
   };
}

/**
 * Returns the path to the cosmo script base, relative to the document NOT dojo
 */
cosmo.env.getCosmoBase = cosmo.env._getCachePropGetterPopulator("cosmoBase", function(){
    // "../.." is ugly but it works. 
    var uri = dojo.hostenv.getBaseScriptUri() + "../../";
    cosmo.env._cosmoConfig["baseCosmoUri"] = uri;
    return uri;
});

/**
 * Returns the path to the widgets template directory , relative to the document NOT dojo.
 * In other words, not for use with dojo.uri.dojoUri(), which wants you to be relative to 
 * dojo scripts. This is useful for stuff like css background urls which can't deal with
 * dojo relative uri's
 *
 * TODO - add an option for getting dojo-relative URI's
 */
cosmo.env.getTemplateBase = cosmo.env._getCachePropGetterPopulator("templateBase", function(){
//FIXME maybe this should go in our base widget (once we make one ;-) )
    var uri = cosmo.env.getCosmoBase() + "cosmo/ui/widget/templates/";
    return uri;
});

/**
 * Returns the baseURI of the application.
 */
cosmo.env.getBaseUrl = function(){
    var result = cosmo.env._cosmoConfig["baseUrl"];
    if (typeof(result) == "undefined"){
        throw new Error("You must setBaseUrl before calling this function");
    }
	return result;
}

/**
 * Sets the base url of the application. Provided by the server somehow.
 * @param {String} baseUrl
 */
cosmo.env.setBaseUrl = function(baseUrl){
    cosmo.env._cosmoConfig["baseUrl"] = baseUrl;
}

cosmo.env.getImagesUrl = function(){
	return cosmo.env.getBaseUrl() + '/templates/default/images/';
}

cosmo.env.getRedirectUrl = function(){
	return cosmo.env.getBaseUrl() + '/redirect_login.jsp';}

cosmo.env.getLoginRedirect = function(){
	return cosmo.env.getBaseUrl() + "/login";
}

cosmo.env.getAuthProc = function(){
	return cosmo.env.getBaseUrl() + "/j_acegi_security_check";
}

cosmo.env.getVersion = function(){
	if (cosmo.env._version)	return cosmo.env._version;
	else {
		var s = dojo.hostenv.getText(
			cosmo.env.getBaseUrl() + "/version.jsp");
		s = dojo.string.trim(s);
		cosmo.env._version = s;
	}
}
