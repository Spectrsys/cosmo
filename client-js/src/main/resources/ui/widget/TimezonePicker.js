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
dojo.provide("cosmo.ui.widget.TimezonePicker");

dojo.require("cosmo.util.html");
dojo.require("dijit._Templated");
dojo.require("cosmo.datetime.timezone");

dojo.requireLocalization("cosmo.ui.widget", "TimezonePicker");

dojo.declare("cosmo.ui.widget.TimezonePicker", [dijit._Widget, dijit._Templated], {
    templatePath: dojo.moduleUrl("cosmo", "ui/widget/templates/TimezonePicker.html"),
    widgetsInTemplate: false,

    initItem: null,

    // Attach points
    timezoneRegionSelector: null,
    timezoneIdSelector: null,

    constructor: function(){
        this.l10n = dojo.i18n.getLocalization("cosmo.ui.widget", "TimezonePicker");
    },

    updateFromTimezone: function(tz){
        if (tz){
            var tzId = tz.tzId;
            var region = tzId.split("/")[0];
            this.updateFromTimezoneRegion(region);
            cosmo.util.html.setSelect(this.timezoneIdSelector, tzId);
        } else {
            this.clearTimezoneSelectors();
        }
    },

    updateFromTimezoneRegion: function(region){
        if (region){
            cosmo.util.html.setSelect(this.timezoneRegionSelector, region);
            cosmo.util.html.setSelectOptions(this.timezoneIdSelector, this.getTimezoneIdOptions(region));
        }
        this.setTimezoneSelectorVisibility();
    },

    setTimezoneSelectorVisibility: function(){
        if (this.timezoneRegionSelector.value)
            this.showTimezoneSelectors();
        else
            this.clearTimezoneSelectors();
    },

    showTimezoneSelectors: function(){
        dojo.removeClass(this.timezoneIdSelector, "cosmoTimezoneHidden");
        dojo.removeClass(this.timezoneRegionSelector, "expandWidth");
    },

    clearTimezoneSelectors: function(){
        this.timezoneRegionSelector.value = "";
        this.timezoneIdSelector.value = "";
        dojo.addClass(this.timezoneIdSelector, "cosmoTimezoneHidden");
        dojo.addClass(this.timezoneRegionSelector, "expandWidth");
    },

    getTimezoneIdOptions: function(region){
        return [{text: this.l10n.noTzId,
                 value: "" }
               ].concat(dojo.map(cosmo.datetime.timezone.getTzIdsForRegion(region),
                   function(id){
                       return {
                           text: id.substr(
                               id.indexOf("/") + 1).replace(/_/g," "),
                               value: id
                       };
                   }));
    },

    // event handlers
    tzRegionOnChange: function(e){
        this.updateFromTimezoneRegion(e.target.value);
    }

});