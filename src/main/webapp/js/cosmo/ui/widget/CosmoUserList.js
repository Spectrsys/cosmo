/*
 * Copyright 2006-2007 Open Source Applications Foundation
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
 * @fileoverview CosmoList - a list of cosmo users that speaks CMP
 * @author Travis Vachon travis@osafoundation.org
 * @license Apache License 2.0
 */

dojo.provide("cosmo.ui.widget.CosmoUserList");

dojo.require("dojo.widget.*");
dojo.require("dojo.event.*");
dojo.require("dojo.dom");
dojo.require("dojo.date.serialize");
dojo.require("dojo.uri.Uri");

dojo.require("cosmo.env");
dojo.require("cosmo.cmp");

dojo.require("dojo.widget.FilteringTable");


dojo.widget.defineWidget("cosmo.ui.widget.CosmoUserList", dojo.widget.FilteringTable,
    {
        resourceDirectory : cosmo.env.getTemplateBase() + "CosmoUserList/",

        sortOrder : null,
        sortType : null,

        pageNumber : 1,
        pageSize : 25,

        cmpFirstLink : null,
        cmpPreviousLink : null,
        cmpNextLink : null,
        cmpLast : null,
        userCountIndicator: null,

        orderIndicator : null,
        
        ASCENDING : "ascending",
        DESCENDING : "descending",
        DEFAULT_SORT_TYPE : "username",
        DEFAULT_SORT_ORDER : "ascending",

        createOrderIndicator : function(){
            var node = document.createElement("img")
            node.setAttribute("id", "orderIndicator")

            resourceDirectory = this.resourceDirectory

            node.setAscending = function(){
                node.setAttribute("src", resourceDirectory + "ascending.png");
                node.setAttribute("alt", " v");
            }
            node.setDescending = function(){
                node.setAttribute("src", resourceDirectory + "descending.png");
                node.setAttribute("alt", " ^");
            }

            node.setAscending();

            return node;
        },
        
        setSortOrder : function(order){
            if (order == this.DESCENDING){
                this.sortOrder = order;
                this.orderIndicator.setDescending();
            } else if (order == this.ASCENDING){
                this.sortOrder = order;
                this.orderIndicator.setAscending();
            }
        },

        setSortType : function(type){
            this.sortType = type;

            var header;

            var tableHeaders = this.domNode.getElementsByTagName("th");
            for (i = 0; i < tableHeaders.length; i++){
                if (tableHeaders[i].getAttribute("field") == type){
                    header = tableHeaders[i];
                }
            }

            if (!header){
                alert("Could not find " + type + "column");
            } else {
                header.appendChild(this.orderIndicator);
            }
        },

        deleteSelectedUsers: function(){
            var users = this.getSelectedData();
            var usernames = []

            for (i=0; i<users.length; i++){

                usernames.push(users[i].username)
            }
            self = this;

            cosmo.cmp.deleteUsers(usernames,
                {load: function(type, data, evt){self.updateUserList()},
                 error: function(type, error){alert("Could not delete user:" + error)}
                }
                );
        },

        loadCMPPage:function(cmpUrl){
            var documentAddress = new dojo.uri.Uri(cmpUrl);
            
            var query = documentAddress.query.substring(1);
            var vars = query.split("&");

            for (i=0; i < vars.length; i++){
                pair = vars[i].split("=")

                switch(pair[0]){
                    case('ps'):
                        this.pageSize = pair[1];
                        break;
                    case('pn'):
                        this.pageNumber = pair[1];
                        break;
                    case('so'):
                        this.setSortOrder(pair[1]);
                        break;
                    case('st'):
                        this.setSortType(pair[1]);
                        break;
                    }
            }
            this.updateUserList()
        },

        createPagingLink:function(label, id, jsText){
            var a = document.createElement("a");
            a.setAttribute("class", "userListPagingLink");
            a.setAttribute("id", id);
            a.setAttribute("href", "javascript:void(0)");
            a.style.visibility = "hidden";

            dojo.event.connect(a, "onclick", this, jsText);

            var t =	document.createTextNode(label);
            a.appendChild(t);
            return a;
        },

        createPageSizeChooser:function(){
            var s = document.createElement("span");
            s.setAttribute("id", "pageSizeChooser");

            s.appendChild(document.createTextNode("Users per page: "));

            var i = document.createElement("input");
            i.setAttribute("type", "text");
            i.setAttribute("size", "4");
            i.setAttribute("maxlength", "4");
            i.setAttribute("value", this.pageSize);
            i.setAttribute("align", "middle");

            dojo.event.kwConnect({
                srcObj : i,
                srcFunc : "onchange",
                targetObj : this,
                targetFunc : "onPageSizeChooserChange",
                once : true});

            s.appendChild(i);
            return s;
        },

        createPageNumberChooser:function(){
            var s = document.createElement("span");
            s.setAttribute("id", "pageNumberChooser");
            s.style.visibility = "hidden";

            s.appendChild(this.createPagingLink(" << ", "firstPageLink","loadFirstPage"));
            s.appendChild(this.createPagingLink(" < ", "previousPageLink", "loadPreviousPage"));

            s.appendChild(document.createTextNode("Go to page: "));

            var i = document.createElement("input");
            i.setAttribute("type", "text");
            i.setAttribute("size", "2");
            i.setAttribute("maxlength", "10");
            i.setAttribute("value", this.pageNumber);
            i.setAttribute("align", "middle");

            dojo.event.kwConnect({
                srcObj : i,
                srcFunc : "onchange",
                targetObj : this,
                targetFunc : "onPageNumberChooserChange",
                once : true});

            s.appendChild(i);

            s.appendChild(this.createPagingLink(" > ", "nextPageLink", "loadNextPage"));
            s.appendChild(this.createPagingLink(" >> ", "lastPageLink", "loadLastPage"));

            return s;
        },

      
        createUserCountIndicator: function(){
        	var s = document.createElement("span");
        	s.setAttribute("id", "userCountIndicatorSpan");
        	
			var count = document.createElement("span");
			
			this.userCountIndicator = count;
			
			s.appendChild(document.createTextNode("Total Users: "));
			s.appendChild(count);
    
			this.updateTotalUserCount();
        	return s;
        },
        
        updateTotalUserCount: function(){
        	var self = this;
	       	var setCountCallback = function (type, data, evt){
				self.userCountIndicator.innerHTML = data;
    	   	}
        	
        	cosmo.cmp.getUserCount({
        		load: setCountCallback,
        		error: function(type, error){
        			alert('Could not get user count: ' + error.message);
        		}
        	});
        	
        },
        
        onPageNumberChooserChange : function (evt){
            this.pageNumber = evt.target.value;
            this.updateUserList();
        },

        onPageSizeChooserChange : function (evt){
            if (evt.target.value <= 0 || 
                evt.target.value % 1 != 1){
                alert("Page size cannot be " + evt.target.value + ".");
                evt.target.value = this.pageSize;
                return;
            }
            this.pageSize = evt.target.value;
            this.pageNumber = 1;
            this.updateUserList();
        },

        loadFirstPage:function(){
            if (this.cmpFirstLink){
                this.loadCMPPage(this.cmpFirstLink);
            }
        },

        loadPreviousPage:function(){
            if (this.cmpPreviousLink){
                this.loadCMPPage(this.cmpPreviousLink);
            }
        },

        loadNextPage:function(){
            if (this.cmpNextLink){
                this.loadCMPPage(this.cmpNextLink);
            }
        },

        loadLastPage:function(){
            if (this.cmpLastLink){
                this.loadCMPPage(this.cmpLastLink);
            }
        },

        updatePageNumber:function(page){
            this.pageNumber = page;
            this.updateUserList();
        },

        updatePageSize:function(size){
            this.pageSize = size;
            this.updateUserList();
        },

        updateControlsView: function(){
            document.getElementById("pageSizeChooser").
                getElementsByTagName("input")[0].value = this.pageSize;

            document.getElementById("pageNumberChooser").
                getElementsByTagName("input")[0].value = this.pageNumber;
                
           	this.updateTotalUserCount();

        },

        updateUserListCallback:function(data, evt){

            var cmpXml = evt.responseXML;

            this.updateControlsView();

            var jsonObject = [];

            var users = data;

            for (i = 0; i < users.length; i++){

                var user = users[i];

                var row = {};

                row.email = user.email;
                row.name = user.firstName + " " + user.lastName;
                row.username = user.username;


                row.created = dojo.date.fromRfc3339(user.dateCreated);

                row.modified = dojo.date.fromRfc3339(user.dateModified);
                
                if (user.unactivated) {
                	row.activated = "No";
                } else {
                	row.activated = "Yes";
                }

                if (user.administrator) {
                    row.admin = "Yes";
                } else {
                    row.admin = "No";
                }
                
                row.userObject = user;

                jsonObject.push(row);
            }

            var pagingLinks = cmpXml.getElementsByTagName("link");

            this.cmpFirstLink = null;
            this.cmpPreviousLink = null;
            this.cmpNextLink = null;
            this.cmpLastLink = null;

            for (i=0; i< pagingLinks.length; i++){

                link = pagingLinks[i]

                switch(link.getAttribute("rel")){
                    case 'first':
                        this.cmpFirstLink = link.getAttribute("href");
                        break;
                    case 'previous':
                        this.cmpPreviousLink = link.getAttribute("href");
                        break;
                    case 'next':
                        this.cmpNextLink = link.getAttribute("href");
                        break;
                    case 'last':
                        this.cmpLastLink = link.getAttribute("href");
                        break;
                }
            }

            var multiPage = (this.cmpPreviousLink || this.cmpNextLink)

            document.getElementById("firstPageLink").style.visibility =
                (this.cmpFirstLink && multiPage) ? 'visible' : 'hidden';

            document.getElementById("previousPageLink").style.visibility =
                (this.cmpPreviousLink) ? 'visible' : 'hidden';

            document.getElementById("nextPageLink").style.visibility =
                (this.cmpNextLink) ? 'visible' : 'hidden';

            document.getElementById("lastPageLink").style.visibility =
                (this.cmpLastLink && multiPage) ? 'visible' : 'hidden';

            document.getElementById("pageNumberChooser").style.visibility =
                (multiPage) ? 'visible' : 'hidden';

            this.store.setData(jsonObject);

        },

        updateUserList:function(){


            var self = this;


            cosmo.cmp.getUsers({
                load: function(type, data, evt){self.updateUserListCallback(data, evt)},
                 error: function(type, error){alert("Could not update user list:" + error.message)}
                 },
                 this.pageNumber,
                 this.pageSize,
                 this.sortOrder,
                 this.sortType);
        },

        // These two functions will disable client side sorting.
        createSorter : function(x){return null},

        onSort:function(/* DomEvent */ e){
            this.pageNumber = 1;

            var sortType = e.currentTarget.getAttribute("field");

            if (this.sortType == sortType){
                if (this.sortOrder == this.ASCENDING){
                    this.setSortOrder(this.DESCENDING);
                } else {
                    this.setSortOrder(this.ASCENDING);
                }
            } else if (sortType) {
                this.setSortType(sortType);
                e.currentTarget.appendChild(this.orderIndicator);
            }

            this.updateUserList();
        }

    },
    "html",
    function(){
        var self = this;

        //dojo.widget.html.SortableTable.call(this);
        this.widgetType="CosmoUserList";

        this.orderIndicator = this.createOrderIndicator();

        //this.render = function(b){this.updateUserList()};

        this.userListPostCreate = function(){


            //this.sortableTablePostCreate();

            this.setSortType(this.DEFAULT_SORT_TYPE);
            this.setSortOrder(this.DEFAULT_SORT_ORDER);

            var table = this.domNode;

            var controls = document.createElement("div");
            controls.setAttribute("id", "userListControls");

            controls.appendChild(this.createPageSizeChooser());
            controls.appendChild(this.createPageNumberChooser());
            controls.appendChild(this.createUserCountIndicator());

            table.parentNode.insertBefore(controls, table);
            
            dojo.event.topic.registerPublisher("/userListSelectionChanged", this, "renderSelections");

            this.updateUserList()

        }
        dojo.event.connect("after", this, "postCreate", this, "userListPostCreate");

		this.aroundCreateRow = function (invocation){
			var user = invocation.args[0].src;
			var row = invocation.proceed();
			row.id = user.username + "Row";
			return row;

		};
        dojo.event.connect("around", this, "createRow", this, "aroundCreateRow");
        	
    }
);
