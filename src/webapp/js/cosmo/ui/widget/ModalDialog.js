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
 * @fileoverview ModalDialog -- modal dialog, including full-window
 * masking div to prevent user from interacting with underlying
 * doc when dialog is showing. Content area can be a string of
 * HTML or a DOM node to insert. If the title property is not
 * empty, the dialog has a rectangular title bar at the top.
 * the defaultAction prop should be a function object to be
 * executed by default from the Enter key if the dialog is being
 * displayed.
 * @author Matthew Eernisse mde@osafoundation.org
 * @license Apache License 2.0
 */

dojo.provide("cosmo.ui.widget.ModalDialog");

dojo.require("dojo.widget.*");
dojo.require("dojo.html.common");
dojo.require("cosmo.env");
dojo.require("cosmo.util.i18n");
dojo.require("cosmo.ui.widget.ButtonPanel");
dojo.require("cosmo.ui.widget.Button");

dojo.widget.defineWidget("cosmo.ui.widget.ModalDialog", 
dojo.widget.HtmlWidget, {
        // Template stuff
        templatePath:dojo.uri.dojoUri(
            '../../cosmo/ui/widget/templates/ModalDialog/ModalDialog.html'),
        
        // Attach points
        containerNode: null,
        titleNode: null,
        promptNode: null,
        imageNode: null,
        contentNode: null,
        buttonPanelNode: null,
        
        INFO: 'info',
        ERROR: 'error',
        CONFIRM: 'confirm',
        title: '',
        prompt: '',
        content: null,
        btnsLeft: [],
        btnsCenter: [],
        btnsRight: [],
        btnPanel: null,
        uiFullMask: null,
        defaultAction: null,
        isDisplayed: false,
        
        // Instance methods
        setTop: function (n) {
            var s = n.toString();
            s = s.indexOf('%') > -1 ? s : parseInt(s) + 'px';
            this.domNode.style.top = s;
        },
        setLeft: function (n) {
            var s = n.toString();
            s = s.indexOf('%') > -1 ? s : parseInt(s) + 'px';
            this.domNode.style.left = s;
        },
        setWidth: function (n) {
            var s = n.toString();
            s = s.indexOf('%') > -1 ? s : parseInt(s) + 'px';
            this.domNode.style.width = s;
        },
        setHeight: function (n) {
            var s = n.toString();
            s = s.indexOf('%') > -1 ? s : parseInt(s) + 'px';
            this.domNode.style.height = s; 
        },
        setContentAreaHeight: function () {
            var spacer = this.buttonPanelNode.offsetHeight;
            spacer += 32;
            if (this.title) {
                spacer += this.titleNode.offsetHeight;
            }
            if (this.prompt) {
                spacer += this.promptNode.offsetHeight;
            }
            this.contentNode.style.height = (this.domNode.offsetHeight - spacer) + 'px';
        },
        setTitle: function (title) {
            this.title = title || this.title;
            if (this.title) {
                this.titleNode.className = 'dialogTitle';
                this.titleNode.innerHTML = this.title;
            }
            else {
                this.titleNode.className = 'invisible';
                this.titleNode.innerHTML = '';
            }
            return true;
        },
        setPrompt: function (prompt) {
            this.prompt = prompt || this.prompt;
            if (this.prompt) {
                this.promptNode.className = 'dialogPrompt';
                this.promptNode.innerHTML = this.prompt;
            }
            else {
                this.promptNode.className = 'invisible';
                this.promptNode.innerHTML = '';
            }
            return true;
        },
        setContent: function (content) {
            this.content = content || this.content;
            // Content area
            if (typeof this.content == 'string') {
                this.contentNode.innerHTML = this.content;
            }
            else {
                var ch = this.contentNode.firstChild;
                while(ch) {
                    this.contentNode.removeChild(ch);
                    ch = this.contentNode.firstChild;
                }
                this.contentNode.appendChild(this.content);
            }
            return true;
        },
        setButtons: function (l, c, r) {
            var bDiv = this.buttonPanelNode;
            
            function destroyButtons(b) {
                for (var i = 0; i < b.length; i++) {
                    b[i].destroy();
                }
            }

            // Clean up previous panel if any
            if (self.btnPanel) {
                destroyButtons(this.btnsLeft);    
                destroyButtons(this.btnsCenter);    
                destroyButtons(this.btnsRight);    
                self.btnPanel.destroy();
                if (bDiv.firstChild) {
                    bDiv.removeChild(bDiv.firstChild);
                }
            }
            
            // Reset buttons if needed
            this.btnsLeft = l || this.btnsLeft;
            this.btnsCenter = c || this.btnsCenter;
            this.btnsRight = r || this.btnsRight;
            
            // Create and append the panel
            self.btnPanel = dojo.widget.createWidget(
                'ButtonPanel', { btnsLeft: this.btnsLeft, btnsCenter: this.btnsCenter,
                btnsRight: this.btnsRight });
            bDiv.appendChild(self.btnPanel.domNode);
            return true;
        },
        render: function () {
            return (this.setTitle() &&
                this.setPrompt() &&
                this.setContent() &&
                this.setButtons());
        },
        center: function () {
            var w = dojo.html.getViewport().width;
            var h = dojo.html.getViewport().height;
            this.setLeft(parseInt((w - this.width)/2));
            this.setTop(parseInt((h - this.height)/2));
            return true;
        },
        renderUiMask: function () {
            if (!this.uiFullMask) {
                m = document.createElement('div');
                m.style.display = 'none';
                m.style.position = 'absolute';
                m.style.top = '0px';
                m.style.left = '0px';
                m.style.width = '100%';
                m.style.height = '100%';
                m.style.background = 'transparent';
                this.uiFullMask = m;
                document.body.appendChild(m);
            }
            this.uiFullMask.style.display = 'block';
            return true;
        },
        
        // Lifecycle stuff
        postMixInProperties: function () {
            this.toggleObj =
                dojo.lfx.toggle[this.toggle] || dojo.lfx.toggle.plain;
            // Clone original show method
            this.showOrig = eval(this.show.valueOf());
            // Do sizing, positioning, content update
            // before calling stock Dojo show
            this.show = function (content, l, c, r, title, prompt) {
                // Accommodate either original multiple param or
                // object param input
                if (typeof arguments[0] == 'object') {
                    var o = arguments[0];
                    if (o.content) { this.content = o.content; }
                    if (o.btnsLeft) { l = o.btnsLeft; }
                    if (o.btnsCenter) { l = o.btnsCenter; }
                    if (o.btnsRight) { l = o.btnsRight; }
                    if (o.title) { this.title = o.title; }
                    if (o.prompt) { this.prompt = o.prompt; }
                }
                else {
                    this.content = content || this.content;
                    this.btnsLeft = l || this.btnsLeft;
                    this.btnsCenter = c || this.btnsCenter;
                    this.btnsRight = r || this.btnsRight;
                    this.title = title || this.title;
                    this.prompt = prompt || this.prompt;
                }
                // Sizing
                this.width = this.width || DIALOG_BOX_WIDTH;
                this.height = this.height || DIALOG_BOX_HEIGHT;
                this.setWidth(this.width);
                this.setHeight(this.height);
                
                // Don't display until rendered and centered
                if (this.render() && this.center() && this.renderUiMask()) { 
                    this.domNode.style.display = 'block';
                    this.domNode.style.zIndex = 2000;
                    // Have to measure for content area height once div is actually on the page
                    this.setContentAreaHeight();
                    // Call the original Dojo show method
                    dojo.lang.hitch(this, this.showOrig);
                    this.isDisplayed = true;
                }
            };
            // Clone original hide method
            this.hideOrig = eval(this.hide.valueOf());
            // Clear buttons and actually take the div off the page
            this.hide = function () {
                // Call the original Dojo hide method
                dojo.lang.hitch(this, this.hideOrig);
                this.content = null;
                this.btnsLeft = [];
                this.btnsCenter = [];
                this.btnsRight = [];
                this.width = null;
                this.height = null;
                this.uiFullMask.style.display = 'none';
                this.domNode.style.display = 'none';
                this.isDisplayed = false;
            };
        },
        
        // Toggling visibility
        toggle: 'plain' } );
