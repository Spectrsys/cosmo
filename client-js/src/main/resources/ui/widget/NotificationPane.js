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
dojo.provide("cosmo.ui.widget.NotificationPane");

dojo.require("cosmo.util.html");
dojo.require("dijit._Templated");
dojo.require("cosmo.ui.widget.TimezonePicker");
dojo.require("cosmo.app.pim");

dojo.requireLocalization("cosmo.ui.widget", "NotificationPane");

PREF_PREFIX = "cosmo.scheduler.job.1.";

PREF_COLLECTION = PREF_PREFIX + "collection."; // + collection UUID
PREF_ENABLED = PREF_PREFIX + "enabled"; // true
PREF_FREQ = PREF_PREFIX + "reportType"; // daily, weekly
PREF_TIMEZONE = PREF_PREFIX + "timezone"; // olson tz id

PREF_NOTIFIER = PREF_PREFIX + "notifier.name"; //defaults to email
PREF_TYPE = PREF_PREFIX + "type"; // forward"

PREF_DEFAULT_DICT = {};
PREF_DEFAULT_DICT[PREF_NOTIFIER] = "email";
PREF_DEFAULT_DICT[PREF_TYPE]     = "forward";

dojo.declare("cosmo.ui.widget.NotificationPane", [dijit._Widget, dijit._Templated], {
    templatePath: dojo.moduleUrl("cosmo", "ui/widget/templates/NotificationPane.html"),
    widgetsInTemplate: true,

    title: null,
    preferences: null,
    checkboxes: null,

    // Attach points
    collectionsContainer: null,
    allCheckbox: null,
    sendFrequencySelector: null,
    timezonePicker: null,

    constructor: function(preferences){
        this.l10n = dojo.i18n.getLocalization("cosmo.ui.widget", "NotificationPane");
        this.title = this.l10n.title;
        this.preferences = preferences;
    },

    makeCollectionCheckbox: function(uuid, name){
        var dict = {};
        dict.div = document.createElement('div');
        dojo.addClass(dict.div, "collectionChoice");
        dict.input = document.createElement('input');
        dict.label = document.createElement('label');
        dict.input.type = "checkbox";
        var id = "collection_" + uuid;
        dict.input.id = id;
        dict.input.name = uuid;
        dict.label.setAttribute('for', id);
        dict.label.innerHTML = name;
        dict.div.appendChild(dict.input);
        dict.div.appendChild(dict.label);
        return dict;
    },

    postCreate: function(){
        // checkboxes
        var collections = cosmo.app.pim.collections;
        this.checkboxes = [];
        for (var i = 0; i < collections.length; i++){
            var collection = collections.getAtPos(i);
            var uuid = collection.getUid();
            var dict = this.makeCollectionCheckbox(uuid, collection.getDisplayName());

            dict.input.onchange = dojo.hitch(this, this.checkboxOnChange);
            this.checkboxes.push(dict.input);
            if (i % 3 == 0)
                dojo.addClass(dict.div, "clear");
            this.collectionsContainer.appendChild(dict.div);

            dict.input.checked = !!this.preferences[PREF_COLLECTION + uuid];
        }
        this.allCheckbox.checked = this.allCheckboxesAre(true);

        // frequency/enabled
        var freq = this.preferences[PREF_FREQ];
        var disabled = (!this.preferences[PREF_ENABLED] || !freq);
        this.sendFrequencySelector.value = (disabled) ? "never" : freq;

        // timezone
        var tzid = this.preferences[PREF_TIMEZONE];
        var tz = null;

        if (tzid == null)
            tz = cosmo.datetime.timezone.guessTimezone();
        else
            tz = cosmo.datetime.timezone.getTimezone(tzid);
        this.timezonePicker.updateFromTimezone(tz);
    },

    save: function(){
        var deferreds = [];
        var preferences = this.preferences;
        function setPref(key, val, deletePref) {
            if (val != null)
                val = val.toString();
            if (deletePref){
                if (preferences[key] != null)
                    deferreds.push(cosmo.app.pim.serv.deletePreference(key));
            }
            else if (preferences[key] != val)
                deferreds.push(cosmo.app.pim.serv.setPreference(key, val));
        }

        for (var pref in PREF_DEFAULT_DICT){
            setPref(pref, PREF_DEFAULT_DICT[pref]);
        }

        var frequency = this.sendFrequencySelector.value;
        if (frequency == "never")
            frequency = null;
        else
            setPref(PREF_FREQ, frequency);

        var enabled = (!!frequency && !this.allCheckboxesAre(false));
        setPref(PREF_ENABLED, enabled);

        var tzid = this.timezonePicker.timezoneIdSelector.value;
        if (!tzid)
            tzid = "";
        setPref(PREF_TIMEZONE, tzid);

        for (var i in this.checkboxes){
            var box = this.checkboxes[i];
            setPref(PREF_COLLECTION + box.name, box.checked, !box.checked);
        }

        return new dojo.DeferredList(deferreds);
    },

    // event handlers
    selectAllOnChange: function(e){
        for (var i in this.checkboxes){
            this.checkboxes[i].checked = this.allCheckbox.checked;
        }
    },

    allCheckboxesAre: function(value){
        for (var i in this.checkboxes){
            if (this.checkboxes[i].checked != value)
                return false;
        }
        return true;
    },

    checkboxOnChange: function(e){
        this.allCheckbox.checked = this.allCheckboxesAre(true);
    }
});