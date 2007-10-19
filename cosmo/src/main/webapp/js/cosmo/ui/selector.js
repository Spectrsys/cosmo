/*
 * Copyright 2007 Open Source Applications Foundation
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

dojo.provide("cosmo.ui.selector");
dojo.require("cosmo.ui.ContentBox"); // Superclass

dojo.require("dojo.event.*");
dojo.require("cosmo.app.pim");
dojo.require('cosmo.convenience');
dojo.require("cosmo.topics");
dojo.require("cosmo.view.names");
dojo.require("cosmo.util.html");

cosmo.ui.selector.CollectionSelector = function (p) {
    var _this = this;
    this.parent = null;
    this.domNode = null;

    var params = p || {};
    for (var n in params) { this[n] = params[n]; }

    dojo.event.topic.subscribe('/calEvent', _this, 'handlePub_calEvent');
    dojo.event.topic.subscribe(cosmo.topics.CollectionUpdatedMessage.topicName, _this, 'handlePub_app');
    dojo.event.topic.subscribe(cosmo.topics.SubscriptionUpdatedMessage.topicName, _this, 'handlePub_app');

    // Private vars
    this._selectedIndex = null;
    this._doRolloverEffect =  function(e, colorString) {
        // Safari 2 sucks -- DOM-event/DOM-node contention problems
        if (navigator.userAgent.indexOf('Safari/41') > -1) {
            return false;
        }
        if (e && e.target) {
            var targ = e.target;
            while (!targ.className) { targ = targ.parentNode; }
            if (targ.id == 'body') { return false; }
            var prefix = 'collectionSelector';
            if (targ.className.indexOf(prefix) > -1) {
                var par = targ.parentNode;
                var ch = par.childNodes;
                for (var i = 0; i < ch.length; i++) {
                    var node = ch[i];
                    if (node.className != 'collectionSelectorDetails') {
                        ch[i].style.backgroundColor = colorString;
                    }
                }
            }
        }
    };

    // Interface methods
    this.handlePub_calEvent = function (cmd) {
        var act = cmd.action;
        switch (act) {
            case 'eventsLoadSuccess':
                this.render();
                break;
            default:
                // Do nothing
                break;
        }
    };
    // Interface methods
    this.handlePub_app = function (cmd) {
        this.render();
    };
    this.renderSelf = function () {
        var collections = cosmo.app.pim.collections;
        var currColl = cosmo.app.pim.currentCollection;
        var container = _createElem('div');
        container.id = 'collectionSelectorContainer';
        var form = _createElem('form');
        var table = _createElem('table');
        table.cellPadding = 0;
        table.cellSpacing = 0;
        table.id = 'collectionSelectorTable';
        var tbody = _createElem('tbody');
        var tr = null;
        var td = null;
        var displayColl = function (key, c) {
            var cUid = c.getUid();
            var sel = cUid == currColl.getUid();
            var className = '';
            tr = _createElem('tr');

            if (cosmo.app.pim.currentView == cosmo.view.names.CAL) {
                td = _createElem('td');
                var isChecked = !!c.isOverlaid;
                var ch = cosmo.util.html.createInput({
                    type: 'checkbox',
                    name: 'collectionSelectorItemCheck',
                    id: 'collectionSelectorItemCheck_' + cUid,
                    checked: isChecked 
                });
                td.appendChild(ch);
                className = 'collectionSelectorCheckbox';
                if (sel) {
                    className += ' collectionSelectorSel';
                }
                td.className = className;
                tr.appendChild(td);
            }

            td = _createElem('td');
            td.id = 'collectionSelectorItemSel_' + cUid;
            td.appendChild(_createText(c.getDisplayName()));
            className = 'collectionSelectorCollectionName';
            if (sel) {
                className += ' collectionSelectorSel';
            }
            td.className = className;
            tr.appendChild(td);

            td = _createElem('td');
            var icon = cosmo.util.html.createRollOverMouseDownImage(
                cosmo.env.getImageUrl('collection_details.png'));
            icon.style.cursor = 'pointer';
            icon.id = 'collectionSelectorItemDetails_' + cUid;
            td.className = 'collectionSelectorDetails';
            td.appendChild(icon);
            tr.appendChild(td);

            tbody.appendChild(tr);
        };

        // Clear the DOM
        this.clearAll();
        collections.each(displayColl);
        table.appendChild(tbody);
        form.appendChild(table);
        container.appendChild(form);
        container.style.height = COLLECTION_SELECTOR_HEIGHT + 'px';

        // Attach event listeners -- event will be delagated
        // to clicked cell or checkbox
        dojo.event.connect(container, 'onmouseover',
            this, 'handleMouseOver');
        dojo.event.connect(container, 'onmouseout',
            this, 'handleMouseOut');
        dojo.event.connect(container, 'onclick',
            this, 'handleClick');

        this.domNode.appendChild(container);
    };
    this.handleMouseOver = function (e) {
        this._doRolloverEffect(e, '#deeeff');
    };
    this.handleMouseOut = function (e) {
        this._doRolloverEffect(e, '');
    };
    this.handleClick = function (e) {
        if (e && e.target) {
            var targ = e.target;
            while (!targ.id) { targ = targ.parentNode; }
            if (targ.id == 'body') { return false; }
            var prefix = 'collectionSelectorItem';
            if (targ.id.indexOf(prefix) > -1) {
                var collections = cosmo.app.pim.collections;
                var currColl = cosmo.app.pim.currentCollection;
                var currId = currColl.getUid();
                var newCurrColl = null;;
                if (targ.id.indexOf(prefix + 'Details_') > -1) {
                    var id = targ.id.replace(prefix + 'Details_', '');
                    cosmo.app.showDialog(
                        cosmo.ui.widget.CollectionDetailsDialog.getInitProperties(
                            cosmo.app.pim.collections.getItem(id)));
                    return true;
                }
                if (targ.id.indexOf(prefix + 'Sel_') > -1) {
                    var id = targ.id.replace(prefix + 'Sel_', '');
                    newCurrColl = collections.getItem(id);
                    if (id != currId) {
                        var ch = $(prefix + 'Check_' + currId);
                        if (!ch || (ch && !ch.checked)) {
                            currColl.doDisplay = false;
                        }
                        newCurrColl.doDisplay = true;
                        cosmo.view.cal.displayCollections(newCurrColl);
                    }
                }
                else if (targ.id.indexOf(prefix + 'Check_') > -1) {
                    var id = targ.id.replace(prefix + 'Check_', '');
                    var d = targ.checked;
                    newCurrColl = collections.getItem(id);
                    newCurrColl.doDisplay = d;
                    newCurrColl.isOverlaid = d;
                    if (id == currId) { return false; }
                    cosmo.view.cal.displayCollections();
                }
            }
        }
    };
};

cosmo.ui.selector.CollectionSelector.prototype =
    new cosmo.ui.ContentBox();

