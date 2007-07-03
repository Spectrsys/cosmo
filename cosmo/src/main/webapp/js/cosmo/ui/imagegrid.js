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
 * @fileoverview The event detail form that displays info for the
 * selected event
 * @author Matthew Eernisse mailto:mde@osafoundation.org
 * @license Apache License 2.0
 */

dojo.provide("cosmo.ui.imagegrid");

dojo.require("dojo.io.*");
dojo.require("dojo.event.*");
dojo.require("cosmo.env");
dojo.require("cosmo.convenience");

cosmo.ui.imagegrid.config = {};
cosmo.ui.imagegrid.DISABLED_OPACITY = 0.3;
cosmo.ui.imagegrid.IMAGE_PATH = cosmo.env.getImagesUrl() +
  'image_grid.png';

cosmo.ui.imagegrid.readConfig = function (type, data, obj) {
    cosmo.ui.imagegrid.config = data;
};

// Get the config data file that tells us
// which images are where, and what sizes
dojo.io.bind({ url: cosmo.env.getImagesUrl() + 'imagegrid.json',
    method: 'POST', // This is not RESTful, but POST avoids Prototype hijacking
    sync: true,
    mimetype: 'application/json',
    content: '',
    load: function(type, data, obj) {
        cosmo.ui.imagegrid.readConfig(type, data, obj); },
    error: function(type, error) { alert(error.message); } });

cosmo.ui.imagegrid.Image = function (p) {
    this.row = p.row;
    this.column = p.column;
    this.width = p.width + 'px';
    this.height = p.height + 'px';
    this.left = 0 - ((this.column * 45 - 45));
    this.top = 0 - ((this.row * 45) - 45);
    this.bgImg = 'url(' + cosmo.ui.imagegrid.IMAGE_PATH + ')';
    this.bgPos = cosmo.ui.imagegrid.getPosString(this);
};

cosmo.ui.imagegrid.getImage = function (key) {
    var props = cosmo.ui.imagegrid.config[key];
    return new cosmo.ui.imagegrid.Image(props);
}

cosmo.ui.imagegrid.getPosString = function (o) {
    var s = o.left + 'px ' + o.top + 'px';
    return s;
};

cosmo.ui.imagegrid.createImageIcon = function (p) {
    //domNode, defaultState
    var params = p || {};
    params.defaultState = p.iconState;
    params.isButton = false;
    return cosmo.ui.imagegrid._createImageBox(params);
};

cosmo.ui.imagegrid.createImageButton = function (p) {
    //domNode, defaultState, rolloverState
    var params = p || {};
    params.isButton = true;
    return cosmo.ui.imagegrid._createImageBox(params);
};

cosmo.ui.imagegrid._createImageBox = function(p) {
    //domNode, defaultState, rolloverState, isButton
    var d = p.domNode;
    var img = cosmo.ui.imagegrid.getImage(p.defaultState);
    var enabled = typeof p.enabled == 'boolean' ? p.enabled : true;
    d.style.width = img.width;
    d.style.height = img.height;
    d.style.backgroundImage = img.bgImg;
    d.style.backgroundPosition = img.bgPos;
    if (enabled) {
        // Give buttons the hand
        if (p.isButton) {
            d.style.cursor = 'pointer';
        }
        // Selected/default state -- default state gets the rollover
        if (p.selected) {
          var imgOver = cosmo.ui.imagegrid.getImage(p.rolloverState);
          d.style.backgroundPosition = imgOver.bgPos;
        }
        else if (p.rolloverState) {
            var over = function () {
                // Do this look up locally so the value doesn't persist
                // in the closure
                var imgOver = cosmo.ui.imagegrid.getImage(p.rolloverState);
                d.style.backgroundPosition = imgOver.bgPos;
            };
            var out = function () {
                d.style.backgroundPosition = img.bgPos;
            };
            dojo.event.connect(d, 'onmouseover', over);
            dojo.event.connect(d, 'onmouseout', out);
        }
        // Attach click handler is there is one
        if (typeof p.handleClick == 'function') {
            dojo.event.connect(d, 'onclick', p.handleClick);
        }
    }
    else {
        var opac = cosmo.ui.imagegrid.DISABLED_OPACITY;
        d.style.opacity = opac;
        if (document.all) {
            d.style.filter = "alpha(opacity=" + opac * 100 + ")";
        }
    }
    return d;
};
