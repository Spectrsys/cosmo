/* * Copyright 2006-2007 Open Source Applications Foundation *
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
 * summary:
 */

dojo.provide("cosmo.service.transport.Rest");

dojo.require("dojo.io.*");
dojo.require("dojo.string");
dojo.require("dojo.Deferred")
dojo.require("cosmo.env");
dojo.require("cosmo.util.auth");

dojo.declare("cosmo.service.transport.Rest", null,
    {
        translator: null,

        initializer: function (translator){

        },
        
        methodIsSupported: {
            'get': true,
            'post': true
        },
        
        /**
         * summary: Return request populated with attributes common to all CMP calls.
         */
        getDefaultRequest: function (/*dojo.Deferred*/deferred,
                                     /*Object*/ r,
                                     /*Object*/ kwArgs){
            kwArgs = kwArgs || {};
            if (r.url){
                if (!!r.url.match(/.*ticket=.*/)){
                    kwArgs.noAuth = true;
                }
            }
            // Add error fo transport layer problems
            deferred.addErrback(function(e) { dojo.debug("Transport Error: "); 
                                              dojo.debug(e);
                                              return e;});
            var request = cosmo.util.auth.getAuthorizedRequest(r, kwArgs);

            request.load = request.load || this.resultCallback(deferred);
            request.error = request.error || this.errorCallback(deferred);
            request.transport = request.transport || "XMLHTTPTransport";
            request.contentType = request.contentType || 'text/xml';
            request.sync = kwArgs.sync || r.sync || false;
            request.headers = request.headers || {};
            request.headers["Cache-Control"] = "no-cache";
            request.headers["Pragma"] = "no-cache";
            // Fight the dark powers of IE's evil caching mechanism
            //if (document.all) {
                request.preventCache = request.preventCache || true;
            //}
            if (request.method){
                if (!this.methodIsSupported[request.method.toLowerCase()]){
                    request.headers['X-Http-Method-Override'] = request.method;
                    request.method = 'POST';
                }
            }

            return request
        },

        errorCallback: function(/* dojo.Deferred */ deferredRequestHandler){
    		// summary
    		// create callback that calls the Deferreds errback method
    		return function(type, e, xhr){
                // Workaround to not choke on 204s
    		    if ((dojo.render.safari &&
                    !xhr.status) || (dojo.render.ie &&
                         evt.status == 1223)){

    		        xhr = {};
                    xhr.status = 204;
                    xhr.statusText = "No Content";
                    xhr.responseText = "";

                    deferredRequestHandler.callback("", xhr);

                } else {
        			deferredRequestHandler.errback(new Error(e.message), xhr);
                }
    		}
    	},
    	
    	resultCallback: function(/* dojo.Deferred */ deferredRequestHandler){
    		// summary
    		// create callback that calls the Deferred's callback method
    		var tf = dojo.lang.hitch(this,
    			function(type, obj, xhr){
    				if (obj["error"]!=null) {
    					var err = new Error(obj.error);
    					err.id = obj.id;
    					deferredRequestHandler.errback(err, xhr);
    				} else {
    				    obj = xhr.responseXML || obj;
    				    if (dojo.render.html.ie) {
    				        var response = xhr.responseText;
    				        response = response.replace(/xmlns:xml.*=".*"/, "");
    				        obj = new ActiveXObject("Microsoft.XMLDOM");
                            if (!obj.loadXML(response)){
    		                   dojo.debug(obj.parseError.reason)
                            }
    				    }
    					deferredRequestHandler.callback(obj, xhr);
    				}
    			}
    		);
    		return tf;
    	},
    	
    	putText: function (text, url, kwArgs){
    	    var deferred = new dojo.Deferred();
            var r = this.getDefaultRequest(deferred, kwArgs);

            r.contentType = "application/atom+xml";
            r.url = url;

            r.postContent = text;
            r.method = "POST";
            r.headers['X-Http-Method-Override'] = "PUT";
    
            dojo.io.bind(r);
            return deferred;
    	    
    	},
    	
    	postText: function (text, url, kwArgs){
    	    var deferred = new dojo.Deferred();
            var r = this.getDefaultRequest(deferred, kwArgs);

            r.contentType = "application/atom+xml";
            r.url = url;

            r.postContent = text;
            r.method = "POST";
    
            dojo.io.bind(r);
            return deferred;
    	    
    	},
    	
    	queryHashToString: function(/*Object*/ queryHash){
    	    var queryList = [];
    	    for (var key in queryHash){
                queryList.push(key + "=" + queryHash[key]);
    	    }
    	    if (queryList.length > 0){
    	        return "?" + queryList.join("&");
    	    }
    	    else return "";
    	},
    	
    	bind: function (r, kwArgs) {
            kwArgs = kwArgs || {};
            var deferred = new dojo.Deferred();
            var request = this.getDefaultRequest(deferred, r, kwArgs);
            dojo.lang.mixin(request, r);
            dojo.io.bind(request);
            return deferred;
        }
    }
);
