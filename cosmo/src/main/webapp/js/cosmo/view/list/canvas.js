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
dojo.require("cosmo.app.pim.layout");
dojo.require("cosmo.view.common");
dojo.require("cosmo.view.list.common");
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
    for (var n in params) { this[n] = params[n]; }

    // Interface methods
    this.renderSelf = function () {
        this._updateSize();
        this.setPosition(0, CAL_TOP_NAV_HEIGHT);
        this.setSize();

        cosmo.view.list.sort(cosmo.view.list.itemRegistry);
        this._displayTable(cosmo.view.list.itemRegistry);
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
            var p = e.target.parentNode;
            if (!p.id) { return false; }
            // get the UID from the row's DOM node id
            var orig = $(self.currSelectedId);
            if (orig) {
                ch = orig.childNodes;
                for (var i = 0; i < ch.length; i++) {
                    ch[i].className = 'listViewDataCell';
                }
            }
            self.currSelectedId = p.id;
            var ch = p.childNodes;
            for (var i = 0; i < ch.length; i++) {
                ch[i].className = 'listViewDataCell listViewSelectedCell';
            }
            var id = p.id.replace('listView_item', '');
            var item = cosmo.view.list.itemRegistry.getItem(id);
            if (item) {
                self.currSelectedItem = item;
                cosmo.app.pim.baseLayout.mainApp.rightSidebar.detailViewForm.updateFromItem(item);
            }
        }
    };

    // Private methods
    this._updateSize = function () {
        if (this.parent) {
            this.width = this.parent.width - 2; // 2px for borders
            this.height = this.parent.height - CAL_TOP_NAV_HEIGHT;
        }
    };
    // innerHTML will be much faster for table display with
    // lots of rows
    this._displayTable = function (hash) {
        var map = cosmo.view.list.triageStatusCodeNumberMappings;
        var t = '<table id="listViewTable" cellpadding="0" cellspacing="0" style="width: 100%;">';
        var r = '';
        r += '<tr>';
        r += '<td class="listViewHeaderCell" style="width: 24px;">&nbsp;</td>';
        r += '<td class="listViewHeaderCell">Title</td>';
        r += '<td class="listViewHeaderCell">Start</td>';
        r += '<td class="listViewHeaderCell" style="border-right: 0px;">Triage</td>';
        r += '</tr>';
        t += r;
        var getRow = function (key, val) {
            var item = val.data;
            var uid = item.getUid();
            var selCss = 'listView_item' + uid == self.currSelectedId ? ' listViewSelectedCell' : '';
            var eventStamp = item.getEventStamp();
            var start = eventStamp ? eventStamp.getStartDate() : '';
            var triage = item.getTriageStatus();
            var task = item.getTaskStamp() ? '[x]' : '';
            if (start) {
                start = start.strftime('%b %d, %Y %I:%M%p');
            }
            triage = map[triage];
            r = '';
            r += '<tr id="listView_item'  + item.getUid() +'">';
            r += '<td class="listViewDataCell' + selCss + '" style="text-align: center;">' + task + '</td>';
            r += '<td class="listViewDataCell' + selCss + '">' + item.getDisplayName() + '</td>';
            r += '<td class="listViewDataCell' + selCss + '">' + start + '</td>';
            r += '<td class="listViewDataCell' + selCss + '">' + triage + '</td>';
            r += '</tr>';
            t += r;
        }
        hash.each(getRow);
        t += '</table>';
        this.domNode.innerHTML = t;
        // Attach event listeners -- event will be delagated by row
        dojo.event.connect($('listViewTable'), 'onmouseover', this, 'handleMouseOver');
        dojo.event.connect($('listViewTable'), 'onmouseout', this, 'handleMouseOut');
        dojo.event.connect($('listViewTable'), 'onclick', this, 'handleClick');
    };
};

cosmo.view.list.canvas.Canvas.prototype =
  new cosmo.ui.ContentBox();

