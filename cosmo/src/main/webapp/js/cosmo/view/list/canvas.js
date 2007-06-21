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

dojo.provide('cosmo.view.list.canvas');

dojo.require('dojo.event.*');
dojo.require("cosmo.app.pim");
dojo.require("cosmo.app.pim.layout");
dojo.require("cosmo.view.list.common");
dojo.require("cosmo.view.list.sort");
dojo.require("cosmo.util.i18n");
dojo.require("cosmo.util.hash");
dojo.require("cosmo.convenience");
dojo.require("cosmo.ui.ContentBox");

cosmo.view.list.canvas.Canvas = function (p) {
    var self = this;
    var params = p || {};
    this.domNode = null;
    this.id = '';
    this.currSelectedId = '';
    this.currSelectedItem = null;
    this.currSortCol = 'Triage';
    this.currSortDir = 'Desc';
    this.itemsPerPage = 5;
    this.itemCount = 0;
    this.pageCount = 0;
    this.currPageNum = 1;

    for (var n in params) { this[n] = params[n]; }

    // Interface methods
    this.renderSelf = function () {
        var reg = cosmo.view.list.itemRegistry;
        this._updateSize();
        this.setPosition(0, CAL_TOP_NAV_HEIGHT);
        this.setSize();

        cosmo.view.list.sort.doSort(reg, this.currSortCol, this.currSortDir);
        this.displayTable();
        if (this.currSelectedItem) {
            cosmo.app.pim.baseLayout.mainApp.rightSidebar.detailViewForm.updateFromItem(
                this.currSelectedItem);
        }

    }
    this.handleMouseOver = function (e) {
        if (e && e.target) {
            // get the UID from the row's DOM node id
            var p = e.target.parentNode;
            if (!p.id) { return false; }
            var ch = p.childNodes;
            for (var i = 0; i < ch.length; i++) {
                ch[i].className = 'listViewDataCell listViewSelectedCell';
            }
        }
    };
    this.handleMouseOut = function (e) {
        if (e && e.target) {
            // get the UID from the row's DOM node id
            var p = e.target.parentNode;
            if (!p.id || (p.id ==  self.currSelectedId)) { return false; }
            var ch = p.childNodes;
            for (var i = 0; i < ch.length; i++) {
                ch[i].className = 'listViewDataCell';
            }
        }
    };
    this.handleClick = function (e) {
        if (e && e.target) {
            var targ = e.target;
            // Header cell clicked
            if (targ.id && targ.id.indexOf('Header') > -1) {
                this._doSort(targ.id);
            }
            // Normal row cell clicked
            else {
                var p = targ.parentNode;
                if (!p.id) { return false; }
                // Deselect any original selection
                var orig = $(self.currSelectedId);
                if (orig) {
                    ch = orig.childNodes;
                    for (var i = 0; i < ch.length; i++) {
                        ch[i].className = 'listViewDataCell';
                    }
                }
                // The new selection
                self.currSelectedId = p.id;
                var ch = p.childNodes;
                for (var i = 0; i < ch.length; i++) {
                    ch[i].className = 'listViewDataCell listViewSelectedCell';
                }
                var id = p.id.replace('listView_item', '');
                var item = cosmo.view.list.itemRegistry.getItem(id);
                // Load the selected item's stuff into the detail-view form
                if (item) {
                    self.currSelectedItem = item;
                    cosmo.app.pim.baseLayout.mainApp.rightSidebar.detailViewForm.updateFromItem(item);
                }
            }
        }
    };
    // innerHTML will be much faster for table display with
    // lots of rows
    this.displayTable = function () {
        var _list = cosmo.view.list;
        var hash = _list.itemRegistry;
        var map = cosmo.view.list.triageStatusCodeNumberMappings;
        var t = '<table id="listViewTable" cellpadding="0" cellspacing="0" style="width: 100%;">';
        var r = '';
        r += '<tr>';
        r += '<td id="listViewTaskHeader" class="listViewHeaderCell" style="width: 24px;">&nbsp;</td>';
        r += '<td id="listViewTitleHeader" class="listViewHeaderCell">Title</td>';
        r += '<td id="listViewWhoHeader" class="listViewHeaderCell">Updated By</td>';
        r += '<td id="listViewStartDateHeader" class="listViewHeaderCell">Starts On</td>';
        r += '<td id="listViewTriageHeader" class="listViewHeaderCell" style="border-right: 0px;">Triage</td>';
        r += '</tr>';
        t += r;
        var getRow = function (key, val) {
            var item = val;
            var display = item.display;
            var selCss = 'listView_data' + display.uid == self.currSelectedId ? ' listViewSelectedCell' : '';
            var updatedBy = '';
            r = '';
            r += '<tr id="listView_data' + display.uid + '">';
            r += '<td class="listViewDataCell' + selCss + 
                '" style="text-align: center;">' + display.task + '</td>';
            r += '<td class="listViewDataCell' + selCss + '">' + display.title + '</td>';
            r += '<td class="listViewDataCell' + selCss + '">' + display.who + '</td>';
            r += '<td class="listViewDataCell' + selCss + '">' + display.startDate + '</td>';
            r += '<td class="listViewDataCell' + selCss + '">' + display.triage + '</td>';
            r += '</tr>';
            t += r;
        }

        var size = this.itemsPerPage;
        var st = (this.currPageNum * size) - size;
        hash.each(getRow, { start: st, items: size });

        t += '</table>';
        this.domNode.innerHTML = t;

        // Attach event listeners -- event will be delagated by row
        dojo.event.connect($('listViewTable'), 'onmouseover', this, 'handleMouseOver');
        dojo.event.connect($('listViewTable'), 'onmouseout', this, 'handleMouseOut');
        dojo.event.connect($('listViewTable'), 'onclick', this, 'handleClick');
        
        dojo.event.topic.publish('/calEvent', { action: 'navigateLoadedCollection',
            opts: null });
    };
    this.initListProps = function () {
        var items = cosmo.view.list.itemRegistry.length;
        var pages = parseInt(items/this.itemsPerPage);
        if (items % this.itemsPerPage > 0) {
            pages++;
        }
        this.itemCount =  items;
        this.pageCount = pages;
        this.currPageNum = 1;
    };
    this.goNextPage = function () {
        self.currPageNum++;
        self.render();
    };
    this.goPrevPage = function () {
        self.currPageNum--;
        self.render();
    };


    // Private methods
    this._updateSize = function () {
        if (this.parent) {
            this.width = this.parent.width - 2; // 2px for borders
            this.height = this.parent.height - CAL_TOP_NAV_HEIGHT;
        }
    };
    this._doSort = function (id) {
        var s = id.replace('listView', '').replace('Header', '');
        var reg = cosmo.view.list.itemRegistry;
        if (this.currSortCol == s) {
            this.currSortDir = this.currSortDir == 'Desc' ? 'Asc' : 'Desc';
        }
        else {
            this.currSortDir = cosmo.view.list.sort.defaultDirections[s.toUpperCase()];
        }

        this.currPageNum = 1;
        this.currSortCol = s;
        cosmo.view.list.sort.doSort(reg, this.currSortCol, this.currSortDir);
        this.displayTable();
    };

};

cosmo.view.list.canvas.Canvas.prototype =
  new cosmo.ui.ContentBox();

