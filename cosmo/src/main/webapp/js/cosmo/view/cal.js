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

dojo.provide('cosmo.view.cal');

dojo.require("cosmo.app.pim");
dojo.require("cosmo.util.i18n");
dojo.require("cosmo.convenience");
dojo.require("cosmo.util.hash");
dojo.require("cosmo.model");
dojo.require("cosmo.datetime");
dojo.require("cosmo.datetime.util");
dojo.require('cosmo.view.cal.dialog');

cosmo.view.cal = new function () {

    var self = this;
    var ranges = {
        'daily': [dojo.date.dateParts.DAY, 1],
        'weekly': [dojo.date.dateParts.WEEK, 1],
        'biweekly': [dojo.date.dateParts.WEEK, 2],
        'monthly': [dojo.date.dateParts.MONTH, 1],
        'yearly': [dojo.date.dateParts.YEAR, 1]
    }
    // Public attributes
    // ********************
    // What view we're using -- currently only week view exists
    this.viewtype = 'week';
    // Start date for events to display
    this.viewStart = null;
    // End date for events to display
    this.viewEnd = null;
    // Options for saving/removing recurring events
    this.recurringEventOptions = {
        ALL_EVENTS: 'master',
        ALL_FUTURE_EVENTS: 'occurrenceAndFuture',
        ONLY_THIS_EVENT: 'occurrence'
    };
    // How many updates/removals are in-flight
    this.processingQueue = [];
    // Last clicked cal event -- used for selection persistence.
    this.lastSent = null;


    // Saving changes
    // =========================
    /**
     * Main function call for saving new events or changes to existing
     * events -- invokes confirmation dialog for changes to event properties
     * for recurring events (changes to the recurrence rule spawn no dialog).
     * For normal events, this simply passes through to saveEventChanges
     * @param ev A CalEvent object, the event to be saved.
     */
    function saveEventChangesConfirm(ev, delta) {
        var changeTypes = delta.getApplicableChangeTypes();
        var size = 0;
        var change = null;
        for (change in changeTypes){
            if (changeTypes[change]){
                size++;
            }
        }
        
        //only one type of change
        if (size == 1){
            delta.applyChangeType(change);
            dojo.event.topic.publish('/calEvent', {action:'save', data:ev});
        } else {
          cosmo.app.showDialog(cosmo.view.cal.dialog.getProps('saveRecurConfirm', opts));
        }
        
        
    }
    
    /**
     * Called for after passthrough from saveEventChangesConfirm. Routes to
     * the right save operation (i.e., for recurring events, 'All Future,'
     * 'Only This Event,' etc.)
     * @param ev A CalEvent object, the object to be saved.
     * @param qual String, flag for different variations of saving
     * recurring events. May be one of the three recurringEventOptions, or
     * the two special cases of adding recurrence to a single event or
     * removing recurrence completely from a master event for a recurrence.
     */
     //XINT
    function saveEventChanges(ev, qual) {

        // f is a function object gets set based on what type
        // of edit is occurring -- executed from a very brief
        // setTimeout to allow the 'processing ...' state to
        // display
        var f = null;
        // A second function object used as a callback from
        // the first f callback function -- used when the save
        // operation needs to be made on a recurrence instance's
        // master event rather than the instance -- basically
        // just a way of chaining two async calls together
        var h = null;

        // Kill any confirmation dialog that might be showing
        if (cosmo.app.modalDialog.isDisplayed) {
            cosmo.app.hideDialog();
        }

        var opts = self.recurringEventOptions;

        /*
        // Lozenge stuff
        // FIXME: Actually this stuff should be oWnZ0Rd by view.cal.canvas
        // ---------
        // Reset the lozenge because we may be changing to the new type --
        // e.g., between all-day and normal, or between normal single
        // and normal composite
        if (ev.dataOrig &&
            !((ev.data.allDay || ev.data.anyTime) &&
            (ev.dataOrig.allDay || ev.dataOrig.anyTime))) {
            ev.replaceLozenge();
        }
        // Reset the lozenge properties from the event
        ev.lozenge.updateFromEvent(ev, true);
        // Do visual updates to size, position
        ev.lozenge.updateElements();
        // Display processing animation
        ev.lozenge.showProcessing();
        */

        // Recurring event
        if (qual) {
            switch(qual) {
                // Adding recurrence to a normal one-shot
                case 'singleEventAddRecurrence':
                    f = function () { doSaveEvent(ev, { 'saveType': 'singleEventAddRecurrence',
                        'instanceEvent': ev } ) };
                    break;

                // Removing recurrence from a recurring event (along with other possible edits)
                case 'recurrenceMasterRemoveRecurrence':
                    f = function () { doSaveEvent(ev, {
                        'saveType': 'recurrenceMasterRemoveRecurrence', 'instanceEvent': null }) }
                    break;

                // Changing the master event in the recurring sequence
                case opts.ALL_EVENTS:
                    // User is making the edit directly on the master event
                    // no need to go look it up
                    if (ev.data.masterEvent) {
                        f = function () { doSaveEvent(ev, {
                            'saveType': 'recurrenceMaster', 'instanceEvent': null }) }
                    }
                    // User is making the edit from a recurrence instance --
                    // have to look up the master event. This means two chained
                    // async calls
                    else {
                        h = function (evData, err) {
                            if (err) {
                                cosmo.app.showErr('Could not retrieve master event for this recurrence.',
                                    getErrDetailMessage(err));
                                // Broadcast failure
                                dojo.event.topic.publish('/calEvent', { 'action': 'saveFailed',
                                    'qualifier': 'editExisting', 'data': ev });
                            }
                            else {
                                // Basic properties
                                // ----------------------
                                var changedProps = ev.hasChanged(); // Get the list of changed properties
                                var startOrEndChange = false; // Did the start or end of the event change
                                for (var p in changedProps.changes) {
                                    // Changes for start and end have to be calculated relative to
                                    // the date they're on -- other prop changes can just be copied
                                    if (p == 'start' || p == 'end') {
                                        startOrEndChange = true;
                                    }
                                    else {
                                        evData[p] = changedProps.changes[p].newValue;
                                    }
                                }
                                // Start and end
                                // ----------------------
                                if (startOrEndChange) {
                                    var masterStart = evData.start; // The start for the master event
                                    var masterEnd = null; // An empty date for the new master end
                                    var origStart = ev.dataOrig.start; // The pre-edit start for the edited instance
                                    var newStart = ev.data.start; // The start for the edited instance
                                    // The number of minutes between the start and end for the edited instance
                                    var minutesToEnd = cosmo.datetime.Date.diff(dojo.date.dateParts.MINUTE,
                                        ev.data.start, ev.data.end);
                                    // Date parts for the edited instance start
                                    var mon = newStart.getMonth();
                                    var dat = newStart.getDate();
                                    var hou = newStart.getHours();
                                    var min = newStart.getMinutes();

                                    // Modify the master's start based on values for the edited instance
                                    switch (ev.data.recurrenceRule.frequency) {
                                        // Only time can change -- this happens below
                                        case 'daily':
                                            // No changes possible
                                            break;
                                        // Move the start on the master over the same number of
                                        // days as the diff between the original and edited values
                                        // for the instance event
                                        case 'weekly':
                                        case 'biweekly':
                                            var diff = cosmo.datetime.Date.diff(dojo.date.dateParts.DAY,
                                                origStart, newStart);
                                            masterStart.setDate(masterStart.getDate() + diff);
                                            break;
                                        // Set the date of the month for the master event to the
                                        // same date as the edited start for the instance
                                        case 'monthly':
                                            masterStart.setDate(dat);
                                            break;
                                        // Set the month and date of the month for the master to
                                        // the same values as the edited start for the instance
                                        case 'yearly':
                                            masterStart.setMonth(mon);
                                            masterStart.setDate(dat);
                                            break;
                                    }
                                    // All recurrence frequencies
                                    // Set the hours/minutes of the master to the same as the
                                    // values for the edited instance
                                    masterStart.setHours(hou);
                                    masterStart.setMinutes(min);

                                    // Calculate the new end for the master -- set the end
                                    // the same minutes distance from the start as in the original
                                    masterEnd = cosmo.datetime.Date.clone(masterStart);
                                    masterEnd = cosmo.datetime.Date.add(masterEnd,
                                        dojo.date.dateParts.MINUTE, minutesToEnd);
                                    // Set the values in the original end for the master event
                                    evData.end.setYear(masterEnd.getFullYear());
                                    evData.end.setMonth(masterEnd.getMonth());
                                    evData.end.setDate(masterEnd.getDate());
                                    evData.end.setHours(masterEnd.getHours());
                                    evData.end.setMinutes(masterEnd.getMinutes());
                                }

                                // doSaveEvent expects a CalEvent with attached CalEventData
                                var saveEv = new CalEvent();
                                saveEv.data = evData;
                                doSaveEvent(saveEv, { 'saveType': 'recurrenceMaster',
                                    'instanceEvent': ev });
                            }
                        };
                        // Look up the master event for the recurrence and pass the result
                        // on to function h
                        f = function () {
                            var reqId = cosmo.app.pim.currentCollection.conduit.getEvent(
                                cosmo.app.pim.currentCollection.collection.uid, ev.data.id,
                                cosmo.app.pim.currentCollection.transportInfo, h);
                            };
                    }
                    break;

                // Break the previous recurrence and start a new one
                case opts.ALL_FUTURE_EVENTS:
                    var newEv = new CalEvent();
                    var freq = ev.dataOrig.recurrenceRule.frequency;
                    var start = ev.dataOrig.start;
                    // The date (no time values ) of the start time for the
                    // instance being edited -- used to calculate the new end
                    // date for the current recurrence being ended
                    var startNoTime = new cosmo.datetime.Date(start.getFullYear(),
                        start.getMonth(), start.getDate());
                    // These values will tell us where to end the recurrence
                    var unit = ranges[freq][0];
                    var incr = (ranges[freq][1] * -1);
                    // Instances all have the same id as the master event
                    var masterEventDataId = ev.data.id;

                    // Calc the new end date for the original recurrence --
                    // go back 'one recurrence unit' (e.g., go back one day for
                    // a daily event, one week for a weekly event, etc.)
                    recurEnd = cosmo.datetime.Date.add(startNoTime, unit, incr);

                    // Pass a CalEvent obj with an attached CalEventData obj
                    newEv.data = CalEventData.clone(ev.data);

                    // If the original recurrence had an end date, and the new event
                    // is also recurring, set the end date on the new recurrence
                    // based on the original end date, relative to the new start of
                    // the new event -- is this the correct behavior?
                    if (newEv.data.recurrenceRule && newEv.data.recurrenceRule.endDate) {
                        var recurEndOrig = newEv.data.recurrenceRule.endDate;
                        var recurEndDiff = cosmo.datetime.Date.diff(dojo.date.dateParts.DAY,
                            startNoTime, recurEndOrig);
                        newEv.data.recurrenceRule.endDate = cosmo.datetime.Date.add(
                            newEv.data.start, dojo.date.dateParts.DAY, recurEndDiff);
                    }

                    f = function () { doSaveEventBreakRecurrence(newEv, masterEventDataId,
                        recurEnd, { 'saveType': 'instanceAllFuture',
                        'instanceEvent': ev, 'masterEventDataId': masterEventDataId, 'recurEnd': recurEnd }); };
                    break;

                // Modifications
                case opts.ONLY_THIS_EVENT:
                    var rrule = ev.data.recurrenceRule;
                    var changedProps = ev.hasChanged(); // The list of what has changed
                    var mod = new Modification(); // New Modification obj to append to the list
                    mod.event = new CalEventData(); // Empty CalEventData to use for saving
                    // instanceDate of the mod serves as the recurrenceId
                    mod.instanceDate = cosmo.datetime.Date.clone(ev.data.instanceDate);
                    for (var p in changedProps.changes) {
                        mod.modifiedProperties.push(p);
                        mod.event[p] = changedProps.changes[p].newValue;
                    }
                    for (var i = 0; i < rrule.modifications.length; i++) {
                        var m = rrule.modifications[i];
                        // Is there already an existing mod?
                        if (m.instanceDate.toUTC() == mod.instanceDate.toUTC()) {
                            // Copy over any previous changes
                            // Overwrite if it's also a current edited prop
                            var mods = m.modifiedProperties;
                            for (var j = 0; j < mods.length; j++) {
                                var p = mods[j];
                                if (typeof changedProps.changes[p] == 'undefined') {
                                    mod.modifiedProperties.push(p);
                                    mod.event[p] = m.event[p];
                                }
                            }
                            // Throw out the old mod
                            rrule.modifications.splice(i, 1);
                        }
                    }
                    rrule.modifications.push(mod);

                    f = function () { doSaveRecurrenceRule(ev, rrule, { 'saveAction': 'save',
                        'saveType': 'instanceOnlyThisEvent', opts: { instanceEvent: ev } }) };
                    break;

                // Default -- nothing to do
                case 'new':
                    dojo.debug("doSaveChanges: new")
                    f = function () { doSaveEvent(ev, { 'saveType': 'singleEvent', 'new': true } ) };
                    break;
                default:
                    break;
            }
        }
        // Normal one-shot event
        else {
            f = function () { doSaveEvent(ev, { 'saveType': 'singleEvent' } ) };
        }

        // Give a sec for the processing state to show
        setTimeout(f, 500);
    }
    /**
     * Call the service to do a normal event save.
     * Creates an anonymous function to pass as the callback for the
     * async service call that saves the changes to the event.
     * Response to the async request is handled by handleSaveEvent.
     * @param ev A CalEvent object, the event to be saved.
     * @param opts An Object, options for the save operation.
     */
     //XINT
    function doSaveEvent(ev, opts) {
        dojo.debug("doSaveEvent");
        // Pass the original event and opts object to the handler function
        // along with the original params passed back in from the async response

        var f = function (newEvId, err, reqId) {
            handleSaveEvent(ev, newEvId, err, reqId, opts); 
        };
        
        var deferred = null;
        var requestId = null;
        var newItem = opts['new'] || false;
        var note = ev.data;
        
        if (newItem){
            deferred = cosmo.app.pim.serv.createItem(note, cosmo.app.pim.currentCollection,{});
        } else {
            deferred = cosmo.app.pim.serv.saveItem(note);
        }
        deferred.addCallback(f);
        
        requestId = deferred.id;
        
        // Add to processing queue -- canvas will not re-render until
        // queue is empty
        self.processingQueue.push(requestId);

        // Selection persistence
        // --------------------
        // In these cases, the events concerned will be re-rendered
        // after re-expanding the recurrence on the server -- no
        // way to preserve the original selection pointer. This means
        // that figuring out where selection goes will require some
        // calcluation
        if (opts.saveType == 'recurrenceMaster' ||
            opts.saveType == 'singleEventAddRecurrence') {
            self.lastSent = null;
        }
        // Just remember the original event that held the selection
        // we'll keep it there after re-render
        else {
            self.lastSent = ev;
        }
    }
    
    /**
     * Call the service to break a recurrence and save a new
     * event. The new event may or may not itself have recurrence.
     * Creates an anonymous function to pass as the callback for the
     * async service call that saves the changes to the event.
     * Response to the async request is handled by handleSaveEvent.
     * @param ev A CalEvent object, the event to be saved.
     * @param origId String, the id of the event for the original recurrence.
     * @param recurEnd A cosmo.datetime.Date, the date the original recurrence
     * should end.
     * @param opts A JS Object, options for the save operation.
     */
     //XINT
    function doSaveEventBreakRecurrence(ev, origId, recurEnd, opts) {
        // Pass the original event and opts object to the handler function
        // along with the original params passed back in from the async response
        var f = function (newEvId, err, reqId) {
            handleSaveEvent(ev, newEvId, err, reqId, opts); };
        var requestId = null;
        requestId = cosmo.app.pim.currentCollection.conduit.saveNewEventBreakRecurrence(
            cosmo.app.pim.currentCollection.collection.uid, ev.data, origId, recurEnd,
            cosmo.app.pim.currentCollection.transportInfo, f);

        self.processingQueue.push(requestId);
        self.lastSent = null;
    }
    /**
     * Handles the response from the async call when saving changes
     * to events.
     * @param ev A CalEvent object, the original event clicked on,
     * or created by double-clicking on the cal canvas.
     * @param newEvId String, the id for the event returned when creating a
     * new event
     * @param err A JS object, the error returned from the server when
     * a save operation fails.
     * @param reqId Number, the id of the async request.
     * @param optsParam A JS Object, options for the save operation.
     */
     //XINT
    function handleSaveEvent(ev, newEvId, err, reqId, optsParam) {
        var saveEv = ev;
        var opts = optsParam || {};
        // Simple error message to go along with details from Error obj
        var errMsg = '';
        var act = '';
        var qual = {};

        qual.saveType = opts.saveType || 'singleEvent'; // Default to single event

        // Failure -- display exception info
        // ============
        if (err) {
            act = 'saveFailed';
            // Failed update
            if ((qual.saveType == 'singleEvent' && saveEv.dataOrig) ||
                qual.saveType == 'instanceAllFuture' ||
                qual.saveType == 'recurrenceMaster') {
                errMsg = _('Main.Error.EventEditSaveFailed');
                qual.newEvent = false;
            }
            // Failed create
            else {
                errMsg = _('Main.Error.EventNewSaveFailed');
                qual.newEvent = true;
            }
            cosmo.app.showErr(errMsg, getErrDetailMessage(err));
        }
        // Success
        // ============
        else {
            act = 'saveSuccess';
            // Set the CalEventData ID from the value returned by server
            // This is for (1) new event creation (the original saved
            // event is waiting to get its id from the server) or
            // (2) new recurring events created by the 'All Future Events'
            // option -- note that newEvId is actually set for these
            // events down below after updating the saved event to
            // point to opts.instanceEvent
            if (!saveEv.data.id || opts.saveType == 'instanceAllFuture') {
                qual.newEvent = true;
                saveEv.data.id = newEvId;
            }
            else {
                qual.newEvent = false;
            }

            // If the event has been edited such that it is now out of
            // the viewable range, remove the event from display
            if (saveEv.isOutOfViewRange()) {
                qual.onCanvas = false;
            }
            // Otherwise update display
            else {
                qual.onCanvas = true;
            }
        }

        // Success for recurring events -- repaint canvas
        if (act == 'saveSuccess' &&
            (opts.saveType == 'recurrenceMaster' ||
            opts.saveType == 'instanceAllFuture' ||
            opts.saveType == 'singleEventAddRecurrence')) {
            // Either (1) single master with recurrence or (2) 'All Future'
            // master/detached-event combo where the new detached event
            // has recurrence -- we need to expand the recurrence(s) by querying the server
            if (saveEv.data.recurrenceRule) {
                loadRecurrenceExpansion(this.viewStart, this.viewEnd, saveEv, opts);
            }
            // If the 'All Future' detached event has a frequency of 'once,'
            // it's a one-shot -- so, no need to go to the server for expansion
            else {
                // Remove this request from the processing queue
                self.processingQueue.shift();
                // saveEv is the dummy CalEvent obj created for saving
                // Replace this with the original clicked-on event that
                // used to be part of the recurrence -- it has an
                // associated lozenge, etc. -- replace the id (which would
                // have been the same as the master) the new CalEventData id
                // from the server
                // FIXME: Assigning newEvId should probably be done above
                // like it is for normal new events
                saveEv = opts.instanceEvent;
                saveEv.data.id = newEvId;
                dojo.event.topic.publish('/calEvent', { 'action': 'eventsAddSuccess',
                   'data': { 'saveEvent': saveEv, 'eventRegistry': null,
                   'opts': opts } });
            }
        }
        // Success/failure for all other cases
        else {
            self.processingQueue.shift();
            // Broadcast message for success/failure
            dojo.event.topic.publish('/calEvent', { 'action': act,
                'qualifier': qual, 'data': saveEv, 'opts': opts });
        }
    }

    // Remove
    // =========================
    /**
     * Main function call for removing events -- invokes different
     * confirmation dialog for recurring events.
     * @param ev A CalEvent object, the event to be removed.
     */
     //XINT
    function removeEventConfirm(ev) {
        var str = '';
        var opts = {};
        opts.masterEvent = false;
        // Recurrence is a ball-buster
        // Display the correct confirmation dialog based on
        // whether or not the event recurs
        if (ev.data.recurrenceRule) {
            str = 'removeRecurConfirm';
            if (ev.data.masterEvent) {
                opts.masterEvent = true;
            }
        }
        else {
            str = 'removeConfirm';
        }
        cosmo.app.showDialog(cosmo.view.cal.dialog.getProps(str, opts));
    }
    /**
     * Called for after passthrough from removeEventConfirm. Routes to
     * the right remove operation (i.e., for recurring events, 'All Future,'
     * 'Only This Event,' etc.)
     * @param ev A CalEvent object, the object to be saved.
     * @param qual String, flag for different variations of removing
     * recurring events. Will be one of the three recurringEventOptions.
     */
     //XINT
    function removeEvent(ev, qual) {
        // f is a function object gets set based on what type
        // of edit is occurring -- executed from a very brief
        // setTimeout to allow the 'processing ...' state to
        // display
        var f = null;
        // A second function object used as a callback from
        // the first f callback function -- used when the save
        // operation needs to be made on a recurrence instance's
        // master event rather than the instance -- basically
        // just a way of chaining two async calls together
        var h = null;
        var opts = self.recurringEventOptions;

        // Kill any confirmation dialog that might be showing
        if (cosmo.app.modalDialog.isDisplayed) {
            cosmo.app.hideDialog();
        }
        // Recurring event
        if (qual) {
            switch(qual) {
                // This is easy -- remove the master event
                case opts.ALL_EVENTS:
                    // User is removing the master event directly --
                    // no need to go look it up
                    if (ev.data.masterEvent) {
                        f = function () { doRemoveEvent(ev, { 'removeType': 'recurrenceMaster' }) };
                    }
                    // User is removing all the events in the recurrence from an
                    // instance -- have to look up the master event. This means
                    // two chained async calls
                    else {
                        h = function (evData, err) {
                            if (err) {
                                cosmo.app.showErr('Could not retrieve master event for this recurrence.', getErrDetailMessage(err));
                                // Broadcast failure
                                dojo.event.topic.publish('/calEvent', { 'action': 'removeFailed',
                                    'data': ev });
                            }
                            else {
                                // doRemoveEvent expects a CalEvent with attached CalEventData
                                var removeEv = new CalEvent();
                                removeEv.data = evData;
                                doRemoveEvent(removeEv, { 'removeType': 'recurrenceMaster',
                                    'instanceEvent': ev });
                            }
                        };
                        f = function () {
                            var reqId = cosmo.app.pim.currentCollection.conduit.getEvent(
                                cosmo.app.pim.currentCollection.collection.uid, ev.data.id,
                                cosmo.app.pim.currentCollection.transportInfo, h);
                        };
                    }
                    break;
                // 'Removing' all future events really just means setting the
                // end date on the recurrence
                case opts.ALL_FUTURE_EVENTS:
                        // Have to go get the recurrence rule -- this means two chained async calls
                        h = function (hashMap, err) {
                            if (err) {
                                cosmo.app.showErr('Could not retrieve recurrence rule for this recurrence.', getErrDetailMessage(err));
                                // Broadcast failure
                                dojo.event.topic.publish('/calEvent', { 'action': 'removeFailed',
                                    'data': ev });
                            }
                            else {
                                // JS object with only one item in it -- get the RecurrenceRule
                                for (var a in hashMap) {
                                    var saveRule = hashMap[a];
                                }
                                var freq = ev.data.recurrenceRule.frequency;
                                var start = ev.data.start;
                                // Use the date of the selected event to figure the
                                // new end date for the recurrence
                                var recurEnd = new cosmo.datetime.Date(start.getFullYear(),
                                    start.getMonth(), start.getDate());
                                var unit = ranges[freq][0];
                                var incr = (ranges[freq][1] * -1);
                                // New end should be one 'recurrence span' back -- e.g.,
                                // the previous day for a daily recurrence, one week back
                                // for a weekly, etc.
                                recurEnd = cosmo.datetime.Date.add(recurEnd, unit, incr);
                                saveRule.endDate = recurEnd;
                                doSaveRecurrenceRule(ev, saveRule, { 'saveAction': 'remove',
                                    'removeType': 'instanceAllFuture', 'recurEnd': recurEnd });
                            }
                        };
                        // Look up the RecurrenceRule and pass the result on to function h
                        f = function () {
                            var reqId = cosmo.app.pim.currentCollection.conduit.getRecurrenceRules(
                                cosmo.app.pim.currentCollection.collection.uid, [ev.data.id],
                                cosmo.app.pim.currentCollection.transportInfo, h);
                            };
                    break;
                // Save the RecurrenceRule with a new exception added for this instance
                case opts.ONLY_THIS_EVENT:
                    var rrule = ev.data.recurrenceRule;
                    var dates = rrule.exceptionDates;
                    var d = cosmo.datetime.Date.clone(ev.data.instanceDate);
                    dates.push(d);
                    rrule.removeModification(d);

                    f = function () { doSaveRecurrenceRule(ev, rrule, { 'saveAction': 'remove',
                        'saveType': 'instanceOnlyThisEvent' }) };
                    break;
                default:
                    // Do nothing
                    break;
            }
        }
        // Normal one-shot event
        else {
            f = function () { doRemoveEvent(ev, { 'removeType': 'singleEvent' }) }
        }
        // Give a sec for the processing state to show
        setTimeout(f, 500);
    }
    /**
     * Call the service to do event removal -- creates an anonymous
     * function to pass as the callback for the async service call.
     * Response to the async request is handled by handleRemoveEvent.
     * @param ev A CalEvent object, the event to be saved.
     * @param opts A JS Object, options for the remove operation.
     */
     //XINT
    function doRemoveEvent(ev, opts) {
        // Pass the original event and opts object to the handler function
        // along with the original params passed back in from the async response
        var f = function (newEvId, err, reqId) {
            handleRemoveResult(ev, newEvId, err, reqId, opts); };

        var requestId = cosmo.app.pim.currentCollection.conduit.removeEvent(
            cosmo.app.pim.currentCollection.collection.uid, ev.data.id,
            cosmo.app.pim.currentCollection.transportInfo, f);
    }
    /**
     * Handles the response from the async call when removing an event.
     * @param ev A CalEvent object, the original event clicked on,
     * or created by double-clicking on the cal canvas.
     * @param newEvId String, FIXME -- Why is this included in Remove?
     * @param err A JS object, the error returned from the server when
     * a remove operation fails.
     * @param reqId Number, the id of the async request.
     * @param optsParam A JS Object, options for the save operation.
     */
    //XINT
    function handleRemoveResult(ev, newEvId, err, reqId, opts) {
        var removeEv = ev;
        // Simple error message to go along with details from Error obj
        var errMsg = _('Main.Error.EventRemoveFailed');
        if (err) {
            act = 'removeFailed';
            cosmo.app.showErr(errMsg, getErrDetailMessage(err));
        }
        else {
            act = 'removeSuccess';
        }

        // Broadcast success
        dojo.event.topic.publish('/calEvent', { 'action': act,
            'data': removeEv, 'opts': opts });
    }
    /**
     * Call the service to save a recurrence rule -- creates an anonymous
     * function to pass as the callback for the async service call.
     * Response to the async request is handled by handleSaveRecurrenceRule.
     * @param ev A CalEvent object, the event originally clicked on.
     * @param rrule A RecurrenceRule, the updated rule for saving.
     * @param opts A JS Object, options for the remove operation.
     */
    //XINT
    function doSaveRecurrenceRule(ev, rrule, opts) {
        // Pass the original event and opts object to the handler function
        // along with the original params passed back in from the async response
        var f = function (ret, err, reqId) {
            handleSaveRecurrenceRule(ev, err, reqId, opts); };
        var requestId = cosmo.app.pim.currentCollection.conduit.saveRecurrenceRule(
            cosmo.app.pim.currentCollection.collection.uid,
            ev.data.id, rrule,
            cosmo.app.pim.currentCollection.transportInfo, f);
    }
    /**
     * Handles the response from the async call when saving changes
     * to a RecurrenceRule.
     * @param ev A CalEvent object, the original event clicked on,
     * or created by double-clicking on the cal canvas.
     * @param err A JS object, the error returned from the server when
     * a remove operation fails.
     * @param reqId Number, the id of the async request.
     * @param opts A JS Object, options for the save operation.
     */
    //XINT
    function handleSaveRecurrenceRule(ev, err, reqId, opts) {

        var rruleEv = ev;
        // Saving the RecurrenceRule can be part of a 'remove'
        // or 'save' -- set the message for an error appropriately
        var errMsgKey = opts.saveAction == 'remove' ?
            'EventRemoveFailed' : 'EventEditSaveFailed';
        // Simple error message to go along with details from Error obj
        var errMsg = _('Main.Error.' + errMsgKey);
        var qual = {};

        if (err) {
            act = opts.saveAction + 'Failed';
            cosmo.app.showErr(errMsg, getErrDetailMessage(err));
        }
        else {
            act = opts.saveAction + 'Success';
        }

        // If the event has been edited such that it is now out of
        // the viewable range, remove the event from display
        if (rruleEv.isOutOfViewRange()) {
            qual.onCanvas = false;
        }
        // Otherwise update display
        else {
            qual.onCanvas = true;
        }

        // Sync changes to the modifications/exceptions in the
        // RecurrenceRule to all the other instances on the canvas
        if (syncRecurrence(rruleEv)) {
            // Broadcast success
            dojo.event.topic.publish('/calEvent', { 'action': act,
                'data': rruleEv, 'opts': opts, 'qualifier': qual });
        }
    }

    function getErrDetailMessage(err) {
        var msg = '';
        switch (true) {
            case (err instanceof cosmo.service.exception.ConcurrencyException):
                msg = _('Main.Error.Concurrency');
                break;
            default:
               msg = err;
               break;
        }
        return msg;
    }
    /**
     * Loads the recurrence expansion for a group of
     * recurring events. Doing it as a group allows you to
     * grab the expansions for several recurrences at once.
     * @param start Number, timestamp for the start of the
     * recurrence
     * @param end Number, timestamp for the end of the
     * recurrence
     * @param ev A CalEvent object, an event in the recurrence
     * @opts A JS Object, options from the original save/remove
     * operation that need to be passed along to the canvas
     * re-render.
     * FIXME: The call to self.processingQueue.shift(); should
     * be moved into handleSaveEvent, which calls this function.
     */
     //XINT
    function loadRecurrenceExpansion(start, end, ev, opts) {
        var id = ev.data.id;
        var s = start.getTime();
        var e = end.getTime();
        var f = function (hashMap) {
            var expandEventHash = createEventRegistry(hashMap);
            self.processingQueue.shift();
            dojo.event.topic.publish('/calEvent', { 'action': 'eventsAddSuccess',
               'data': { 'saveEvent': ev, 'eventRegistry': expandEventHash,
               'opts': opts } });
        }

        cosmo.app.pim.currentCollection.conduit.expandEvents(
            cosmo.app.pim.currentCollection.getUid(), [id], s, e,
            cosmo.app.pim.currentCollection.transportInfo, f);

    }
    /**
     * Take an array of CalEventData objects, and create a Hash of
     * CalEvent objects with attached CalEventData objects.
     * @param arrParam Either an Array, or JS Object with multiple Arrays,
     * containing CalEventData objects
     * @return Hash, the keys are randomized strings, and the values are
     * the CalEvent objects.
     */
    function createEventRegistry(arrParam) {
        var h = new Hash();
        var arr = [];

        // Param may be a single array, or hashmap of arrays -- one
        // for each recurring event sequence
        // ---------------------------------
        // If passed a simple array, use it as-is
        if (arrParam.length) {
            arr = arrParam;
        }
        // If passed a hashmap of arrays, suck all the array items
        // into one array
        else {
            for (var j in arrParam) {
                var a = arrParam[j];
                for (var i = 0; i < a.length; i++) {
                    arr.push(a[i]);
                }
            }
        }

        for (var i = 0; i < arr.length; i++) {
            var note = arr[i];
            var eventStamp = note.getEventStamp();
            var id = self.generateTempId();
            var ev = new CalEvent(id, null);
            ev.data = note;
            h.setItem(id, ev);
        }
        return h;
    }

    // Subscribe to the '/calEvent' channel
    dojo.event.topic.subscribe('/calEvent', self, 'handlePub_calEvent');
    // Subscribe to the '/app' channel
    dojo.event.topic.subscribe('/app', self, 'handlePub_app');

    /**
     * Handle events published on the '/calEvent' channel, including
     * self-published events
     * @param cmd A JS Object, the command containing orders for
     * how to handle the published event.
     */
    this.handlePub_calEvent = function (cmd) {
        var act = cmd.action;
        var qual = cmd.qualifier || null;
        var data = cmd.data || {};
        var opts = cmd.opts;
        switch (act) {
            case 'loadCollection':
                var f = function () { self.triggerLoadEvents(opts); };
                //self.uiMask.show();
                // Give processing message a brief instant to show
                setTimeout(f, 100);
                break;
            case 'saveConfirm':
                var confirmEv = cmd.data;
                var delta = cmd.delta;
                saveEventChangesConfirm(confirmEv, delta);
                break;
            case 'save':
                var saveEv = cmd.data;
                saveEventChanges(saveEv, qual);
                break;
            case 'removeConfirm':
                var confirmEv = cmd.data;
                removeEventConfirm(confirmEv);
                break;
            case 'remove':
                var removeEv = cmd.data;
                removeEvent(removeEv, qual);
                break;
            default:
                // Do nothing
                break;
        }
    };
    /**
     * Handle events published on the '/app' channel -- app-wide
     * events
     * @param cmd A JS Object, the command containing orders for
     * how to handle the published event.
     */
    this.handlePub_app = function (cmd) {
        var e = cmd.appEvent;
        var t = cmd.type;
        // Handle keyboard input
        if (t == 'keyboardInput') {
            // Grab any elem above the event that has an id
            var elem = cosmo.ui.event.handlers.getSrcElemByProp(e, 'id');
            var ev = cosmo.view.cal.canvas.getSelectedEvent();
            switch (e.keyCode) {
                // Enter key
                case 13:
                    // Go-to date
                    if (elem.id.toLowerCase() == 'jumpto') {
                        //cosmo.app.pim.calForm.goJumpToDate();
                    }
                    // Save an event from the Enter key -- requires:
                    //  * a selected event, not in 'processing' state
                    //  * Enter key input from either one of the event
                    //    detail form text inputs, or the document body
                    //  * Write access for the current collection
                    // Currently all other text inputs belong to the event detail form
                    // FIXME -- check for custom prop that says this field belongs
                    // to the event detail form
                    else if (ev && !ev.lozenge.getInputDisabled() &&
                        ((elem.id == 'body') || (elem.className == 'inputText' &&
                            elem.type == 'text')) &&
                        cosmo.app.pim.currentCollection.isWriteable()) {
                        dojo.event.topic.publish('/calEvent',
                            { 'action': 'saveFromForm' });
                    }
                    break;
                // Delete key
                case 46:
                    // Remove an event from the Delete key -- requires:
                    //  * A selected event, not in 'processing' state
                    //  * Enter key input must be from the document body
                    //  * Write access for the current collection
                    if (ev && !ev.lozenge.getInputDisabled() && (elem.id == 'body') &&
                        cosmo.app.pim.currentCollection.isWriteable()) {
                        dojo.event.topic.publish('/calEvent',
                            { 'action': 'removeConfirm', 'data': ev });
                    }
                    break;
            }
        }
    };
    
    this.triggerLoadEvents = function (o) {
        var opts = {};
        var collection = opts.collection;
        var goTo = o.goTo;
        
        // Changing collection
        // --------
        if (collection) {
            // Update pointer to currently selected collection
            self.currentCollection = collection;
            cosmo.app.pim.currentCollection = collection;
        }
        // Changing dates
        // --------
        if (goTo) {
            // param is 'back' or 'next'
            if (typeof goTo == 'string') {
                var key = goTo.toLowerCase();
                var incr = key.indexOf('back') > -1 ? -1 : 1;
                queryDate = cosmo.datetime.Date.add(cosmo.view.cal.viewStart,
                    dojo.date.dateParts.WEEK, incr);
            }
            // param is actual Date
            else {
                queryDate = goTo;
            }
            // Update cosmo.view.cal.viewStart and cosmo.view.cal.viewEnd with new dates
            self.setQuerySpan(queryDate);
        }

        // Data obj to pass to topic publishing
        opts = {
            collection: cosmo.app.pim.currentCollection,
            viewStart: cosmo.view.cal.viewStart,
            viewEnd: cosmo.view.cal.viewEnd,
            currDate: cosmo.app.pim.currDate
        }
        // Pass along the original opts
        for (var n in o) { opts[n] = o[n]; }

        self.loadEvents(opts);
    };
    /**
     * Loading events in the initial app setup, and week-to-week
     * navigation.
     * @param start Number, timestamp for the start of the query
     * period
     * @param end Number, timestamp for the end of the query
     * period
     * @return Boolean, true
     */
    this.loadEvents = function (o) {
        var opts = o;
        var collection = opts.collection;
        var start = opts.viewStart;
        var end = opts.viewEnd;
        var eventLoadList = null;
        var eventLoadHash = new Hash();
        var isErr = false;
        var detail = '';
        var evData = null;
        var id = '';
        var ev = null;
        
        dojo.event.topic.publish('/calEvent', { action: 'eventsLoadStart', 
            opts: opts });
        // Load the array of events
        // ======================
        try {
            var deferred = cosmo.app.pim.serv.getItems(collection, 
                { start: start, end: end }, { sync: true });
            eventLoadList = deferred.results[0];
        }
        catch(e) {
            cosmo.app.showErr(_('Main.Error.LoadEventsFailed'), 
                getErrDetailMessage(e));
            return false;
        }

        var eventLoadHash = createEventRegistry(eventLoadList);
        dojo.event.topic.publish('/calEvent', { action: 'eventsLoadSuccess',
            data: eventLoadHash, opts: opts });
        return true;
    };
    /**
     * Generate a scratch ID to use for events in the UI
     * @return A random string to use as an ID for a CalEvent obj
     */
    this.generateTempId = function () {
        var chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz";
        var len = 12;
        var randomString = '';
        for (var i = 0; i < len; i++) {
            var rnum = Math.floor(Math.random() * chars.length);
            randomString += chars.substring(rnum,rnum+1);
        }
        return 'ev' + randomString;
    };
    /**
     * Get the start and end for the span of time to view in the cal
     * Eventually this will change depending on what type of view is selected
     */
    this.setQuerySpan = function (dt) {
        this.viewStart = cosmo.datetime.util.getWeekStart(dt);
        this.viewEnd = cosmo.datetime.util.getWeekEnd(dt);
        return true;
};
    /**
     * Get the datetime for midnight Sunday given the current Sunday
     * and the number of weeks to move forward or backward
     */
    this.getNewViewStart = function (key) {
        var queryDate = null;
        var incr = 0;
        // Increment/decrement week
        if (key.indexOf('next') > -1) {
            incr = 1;
        }
        else if (key.indexOf('back') > -1) {
            incr = -1;
        }
        queryDate = cosmo.datetime.Date.add(this.viewStart,
            dojo.date.dateParts.WEEK, incr);
        return queryDate;
    };
};

