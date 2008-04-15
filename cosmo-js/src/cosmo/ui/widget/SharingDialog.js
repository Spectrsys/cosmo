/*
 * Copyright 2008 Open Source Applications Foundation
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

dojo.provide("cosmo.ui.widget.SharingDialog");
dojo.require("dijit._Templated");
dojo.require("dijit.form.ComboBox");
dojo.requireLocalization("cosmo.ui.widget", "SharingDialog");

dojo.declare("cosmo.ui.widget.SharingDialog", [dijit._Widget, dijit._Templated],
{
    templatePath: dojo.moduleUrl("cosmo", 'ui/widget/templates/SharingDialog.html'),
    l10n: dojo.i18n.getLocalization("cosmo.ui.widget", "SharingDialog"),

    // init params
    store: null,
    collection: null,
    ticketStore: null,

    // collection or subscription object
    displayName: "",
    urls: null,

    // attach points
    instructionsContainer: null,
    instructionsSelector: null,
    ticketContainer: null,

    changeInstructions: function(e){
        this.instructionsContainer.innerHTML =
            this.l10n[this.instructionsSelector.value + "Instructions"];
    },

    onTicket: function(ticket){
        var t = new cosmo.ui.widget._SharingDialogTicket(
            {ticketStore: this.ticketStore,
             ticket: ticket,
             urls: this.urls,
             l10n: this.l10n
            });
        var x = this.ticketContainer;
        dojo.place(t.domNode, this.ticketContainer, "last");
    },

    // lifecycle methods
    postMixInProperties: function(){
        var store = this.store;
        if (store){
            var collection = this.collection;
            this.displayName = store.getValue(collection, "displayName");
            this.urls = store.getValue(collection, "urls");
            dojo.addOnLoad(dojo.hitch(this, function(){
                this.tickets = this.ticketStore.fetch({
                onItem: dojo.hitch(this, "onTicket"),
                onError: function(e){console.debug(e);
            }})}));
        }
    }
});

dojo.declare("cosmo.ui.widget._SharingDialogTicket", [dijit._Widget, dijit._Templated], {
    templateString: '<div class="sharingDialogTicket">'
                    + '<div id="${id}Details"><span class="ticketKey">${key}</span><span class="ticketPermission">${permission}</span><span class=""></span></div>'
                    + '<div id="${id}Urls" dojoAttachPoint="urlsContainer">'
                     + '<a href="${urls.atom.uri}"><span class="sharingLink atomSharingLink">${l10n.atom}</span></a>'
                     + '<a href="${urls.webcal.uri}"><span class="sharingLink webcalSharingLink">${l10n.webcal}</span></a>'
                     + '<a href="${urls.dav.uri}"><span class="sharingLink davSharingLink">${l10n.dav}</span></a>'
                     + '<a href="${urls.html.uri}"><span class="sharingLink htmlSharingLink">${l10n.html}</span></a>'

                    + '</div>'
                    + '</div>',

    ticketStore: null,
    ticket: null,
    urls: null,

    key: null,
    permission: null,


    postMixInProperties: function(){
        this.key = this.ticketStore.getValue(this.ticket, "key");
        this.permission = this.ticketStore.getValue(this.ticket, "permission");
        if (!this.id) this.id = this.key + "SharingDialogTicket";
        var collectionUrls = this.urls;
        this.urls = {};
        for (urlName in collectionUrls){
            var url = collectionUrls[urlName];
            this.urls[urlName] = new dojo._Url(url.uri + (url.uri.indexOf("?") == -1 ? "?" : "&") + "ticket=" + this.key);
        }
    }
});
