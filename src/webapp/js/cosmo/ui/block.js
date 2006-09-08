/* * Copyright 2006 Open Source Applications Foundation *
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
 * @fileoverview Event blocks that represent the span of time
 * of an event on the calendar
 * @author Matthew Eernisse mailto:mde@osafoundation.org
 * @license Apache License 2.0
 *
 * Has two sub-classes, HasTimeBlock and NoTimeBlock to represent
 * the two main areas where blocks get displayed. HasTime are normal
 * events in the scrolling area, and NoTime are the all-day events
 * in the resizeable area at the top.
 * HasTimeBlock for a multi-day event may be a composite made up of 
 * a main div element and a bunch of auxilliary divs off to the side.
 */

/**
 * @object A visual block to represent the span of time of a calendar
 * event
 */
function Block() {
    // Properties for position and size
    this.top = 0;
    this.left = 0;
    this.height = 0;
    this.width = 0;
    // Whether or not this particular Block is selected.
    // TO-DO: Figure out if this is still used ... ?
    this.selected = false;
    // 30-min minimum height, minus a pixel at top and bottom
    // per retarded CSS spec for borders
    this.unit = (HOUR_UNIT_HEIGHT/2)-2;
    // DOM elem ref to the primary div for the Block
    this.div = null;
    // DOM elem ref for inner div of the Block
    this.innerDiv = null;
    // The separator plus ID -- convenience to avoid
    // concatenating the same thing over and over
    this.idPrefix = '';
    // Array of div elems appearing to the side on multi-day normal
    // events
    this.auxDivList = [];

    /**
     * Convenience method that does all the visual update stuff 
     * for a block at one time
     */
    this.updateDisplayMain = function() {
        this.updateElements();
        this.hideProcessing();
        this.updateText();
    };
    /**
     * Updates the info displayed on a block for the event time
     * and description
     */
    this.updateText = function() {
        var ev = Cal.eventRegistry.getItem(this.id);
        var strtime = ev.data.start.strftime('%I:%M%p');
        // Trim leading zero if need be
        strtime = strtime.indexOf('0') == 0 ? strtime.substr(1) : strtime;
        // Display timezone info for event if it has one
        if (ev.data.start.timezone) {
            strtime += ' (' + ev.data.start.timezone.name + ')';
        }
        var timeDiv = document.getElementById(this.divId + 'Start' +
            Cal.ID_SEPARATOR + ev.id);
        var titleDiv = document.getElementById(this.divId + 'Title' +
            Cal.ID_SEPARATOR + ev.id);
        if (timeDiv) { 
            this.setText(timeDiv, strtime);
        }
        this.setText(titleDiv, ev.data.title);
    };
    /**
     * A bit of a misnomer -- just static text at the moment
     * TO-DO: Add animation -- either GIF or using CSS effects
     */
    this.showStatusAnim = function() {
        var titleDiv = document.getElementById(this.divId + 'Title' +
            Cal.ID_SEPARATOR + this.id);
        this.setText(titleDiv, 'Processing ...');
    };
    /**
     * Toggle cursor to 'default' while block is in processing
     * state -- should not appear to be draggable
     */
    this.mainAreaCursorChange = function(isProc) {
        var cursorChange = isProc ? 'progress' : 'move';
        document.getElementById(this.divId + 'Content' +
            Cal.ID_SEPARATOR + this.id).style.cursor = cursorChange;
    };
    this.getPlatonicLeft = function() {
        var ev = Cal.eventRegistry.getItem(this.id);
        var diff = (Date.diff('d', Cal.viewStart.getTime(), ev.data.start.getTime()));
        return (diff * Cal.dayUnitWidth);
            
    };
    this.getPlatonicWidth = function() {
        var ev = Cal.eventRegistry.getItem(this.id);
        var diff = (ScoobyDate.diff('d', ev.data.start, ev.data.end))+3;
        return (diff * Cal.dayUnitWidth);
    }
    /**
     * Cross-browser wrapper for setting CSS opacity
     * Would like to use this again if I can figure out a
     * workaround for the weird scrolling-div-breakage it causes
     */
    this.setOpacity = function(opac) {
        elem = this.div;
        // =============
        // opac is a whole number to be used as the percent opacity
        // =============
        // IE uses a whole number as a percent (e.g. 75 for 75%)
        //  Moz/compat uses a fractional value (e.g. 0.75)
        var nDecOpacity = opac/100;
        if (document.all) {
            elem.filters.alpha.opacity = opac;
        }
        elem.style.opacity = nDecOpacity;
        return true;
    }
    /**
     * Use DOM to set text inside a node
     */
    this.setText = function(node, str) {
        if (node.firstChild) {
            node.removeChild(node.firstChild);
        }
        node.appendChild(document.createTextNode(str));
    };
    /**
     * Change color of block to indicate (1) selected (2) normal
     * or (3) processing
     */
    this.setState = function(isProc) {
        var isSel = null;
        var stateId = 0; // 1 selected 2 normal 3 processing
        // If this block is processing, change to 'processing' color
        if (isProc) {
            stateId = 3;
        }
        // Change block back after completing processing --
        // change it back to normal 'unselected' color, or 
        // 'selected' color if this block is the most recently
        // clicked one
        else {
            stateId = (Cal.currSelObj && this.id == Cal.currSelObj.id) ?
                1 : 2;
        }
        this.setLozengeAppearance(stateId);
    }
    /**
     * Change color of block to indicate (1) selected (2) processing
     * or (3) normal, unselected
     */
    this.setLozengeAppearance = function(stateId) {
        
        var ev = Cal.eventRegistry.getItem(this.id);
        var useLightColor = this.useLightColor(ev);
        var imgPath = '';
        var textColor = '';
        var borderColor = '';
        var borderStyle = 'solid';
        var blockColor = '';
        
        var mainDiv = document.getElementById(this.divId + Cal.ID_SEPARATOR +
            this.id);
        // If this block is processing, change to 'processing' color
        switch (stateId) {
            // Selected
            case 1:
                if (useLightColor) {
                    textColor = '#0064cb';
                    borderColor = '#3398ff';
                    blockColor = '#bedeff';
                    imgPath = '';
                }
                else {
                    textColor = '#ffffff';
                    borderColor = '#ffffff';
                    blockColor = '#0064cb';
                    imgPath = scooby.env.getImagesUrl() + 'block_gradient_dark.png';
                }
                break;
            // Unselected
            case 2:
                if (useLightColor) {
                    textColor = '#0064cb';
                    borderColor = '#3398ff';
                    blockColor = '#e6f2ff';
                    imgPath = '';
                }
                else {
                    textColor = '#ffffff';
                    borderColor = '#ffffff';
                    blockColor = '#3398ff';
                    imgPath = scooby.env.getImagesUrl() + 'block_gradient_light.png';
                }
                break;
            // Processing
            case 3:
                textColor = '#ffffff';
                borderColor = '#ffffff';
                blockColor = '#9fd1fc';
                imgPath = '';
                break;
            default:
                // Do nothing
                break;
        }
        
        if (ev.data.status && ev.data.status.indexOf('TENTATIVE') > -1) {
            borderStyle = 'dashed';
        }
        
        // Main div for block
        // ------------
        mainDiv.style.color = textColor;
        mainDiv.style.borderColor = borderColor;
        mainDiv.style.backgroundColor = blockColor;
        mainDiv.style.borderStyle = borderStyle;
        // Using the AlphaImageLoader hack b0rks normal z-indexing
        // No pretty transparent PNGs for IE6
        if (!document.all) {
            if (imgPath) {
                mainDiv.style.backgroundImage = 'url(' + imgPath + ')';
            }
            else {
                mainDiv.style.backgroundImage = null;
                
            }
        }
        // Aux divs for multi-day events
        // ------------
        if (this.auxDivList.length) {
            for (var i = 0; i < this.auxDivList.length; i++) {
                auxDiv = this.auxDivList[i];
                auxDiv.style.color = textColor;
                auxDiv.style.borderColor = borderColor;
                auxDiv.style.backgroundColor = blockColor;
                auxDiv.style.borderStyle = borderStyle;
                // Use transparent PNG background in non-IE6 browsers
                if (!document.all) {
                    if (imgPath) {
                        auxDiv.style.backgroundImage = 'url(' + imgPath + ')';
                    }
                    else {
                        auxDiv.style.backgroundImage = null;
                        
                    }
                }
            }
        }
    }
    /**
     * Use light or dark pallette colors
     */
    this.useLightColor = function(ev) {
        var ret = false;
        switch(true) {
            case (ev.data.status && ev.data.status.indexOf('CANCELLED') > -1):
            case (ev.data.start.getTime() == ev.data.end.getTime()):
                ret = true;
                break;
            default:
                // Do nothing
                break;
        }
        return ret;
    };
    /**
     * Make the block look selected -- change color and
     * move forward to z-index of 25
     */
    this.setSelected = function() {
        var auxDiv = null;

        this.setLozengeAppearance(1);
       
        // Set the z-index to the front
        this.div.style.zIndex = 25;
        if (this.auxDivList.length) {
            for (var i = 0; i < this.auxDivList.length; i++) {
                auxDiv = this.auxDivList[i];
                //auxDiv.style.background = SEL_BLOCK_COLOR;
                auxDiv.style.zIndex = 25;
            }
        }
    }
    /**
     * Make the block look unselected -- change color and
     * move back to z-index of 1
     */
    this.setDeselected = function() {
        var auxDiv = null;
        
        this.setLozengeAppearance(2);
        
        // Set the z-index to the back
        this.div.style.zIndex = 1;
        if (this.auxDivList.length) {
            for (var i = 0; i < this.auxDivList.length; i++) {
                auxDiv = this.auxDivList[i];
                //auxDiv.style.background = UNSEL_BLOCK_COLOR;
                auxDiv.style.zIndex = 1;
            }
        }
    }
}

/**
 * HasTimeBlock -- sub-class of Block
 * Normal events, 'at-time' events -- these sit in the scrollable 
 * area of the main viewing area
 */
function HasTimeBlock(id) {
    this.id = id;
}
HasTimeBlock.prototype = new Block();

// Div elem prefix -- all component divs of normal event blocks
// begin with this
HasTimeBlock.prototype.divId = 'eventDiv';
// Does a multi-day event start before the viewable area
HasTimeBlock.prototype.startsBeforeViewRange = false;
// Does a multi-day event extend past the viewable area
HasTimeBlock.prototype.endsAfterViewRange = false;

/**
 * Change block color to 'processing' color
 * Change the cursors for the resize handles at top and bottom,
 * and for the central content div
 * Display status animation
 */
HasTimeBlock.prototype.showProcessing = function() {
    this.setState(true);
    this.resizeHandleCursorChange(true);
    this.mainAreaCursorChange(true);
    this.showStatusAnim();
}

/**
 * Change the cursors for the resize handles at top and bottom
 * Change to 'default' when processing so it won't look draggable
 */
HasTimeBlock.prototype.resizeHandleCursorChange = function(isProc) {
    var topChange = isProc ? 'default' : 'n-resize';
    var bottomChange = isProc ? 'default' : 's-resize';
    var topDiv = document.getElementById(this.divId + 'Top' + 
        Cal.ID_SEPARATOR + this.id);
    var bottomDiv = document.getElementById(this.divId + 'Bottom' + 
        Cal.ID_SEPARATOR + this.id);
    topDiv.style.cursor = topChange;
    // Timed events that extend beyond the viewable area
    // will not have a bottom resize handle
    if (bottomDiv) {
        bottomDiv.style.cursor = bottomChange;
    }
}

/**
 * Return the block to normal after processing
 */
HasTimeBlock.prototype.hideProcessing = function() {
    this.resizeHandleCursorChange(false);
    this.mainAreaCursorChange(false);
    this.setState(false);
}

/**
 * Update the block properties from an event
 * Called when editing from the form, or on drop after resizing/dragging
 * the block has to be updated to show the changes to the event
 */
HasTimeBlock.prototype.updateFromEvent = function(ev) {
    var unit = HOUR_UNIT_HEIGHT/2;
    var startPos = 0;
    var endPos = 0;
    var height = 0;
    var left = 0;
    var width = 0;
    
    // Events edited out of range
    // Move the block from view -- if the update fails, we need
    // to put it back
    if (ev.isOutOfViewRange()) {
       startPos = -10000;
       height = 1;
       left = -10000;
       width = 1;
    }
    // Events still on the canvas
    else {
        if (this.startsBeforeViewRange) {
            startPos = 0;
            left = 0;
        }
        else {
            startPos = Cal.calcPosFromTime(Date.strftime('%H:%M',
            ev.data.start.getTime()));
            left = (ev.data.start.getLocalDay())*Cal.dayUnitWidth;
        }
        endPos = Cal.calcPosFromTime(Date.strftime('%H:%M',
            ev.data.end.getTime()));
        height = endPos - startPos;
        left += (ev.conflictDepth * 10);

        width = Cal.dayUnitWidth - (ev.maxDepth * 10);
        
        // BANDAID: set min height if not multi-day event
        if (!this.auxDivList.length && (height < unit)) {
            height = unit;
        }
    }
    this.left = left;
    this.top = startPos;
    // Show one-pixel border of underlying divs
    // And one-pixel border for actual block div
    // (1 + (2 * 1)) = 3 pixels
    this.height = height - 3; 
    this.width = width - 3;

}

/**
 * Update an event from changes to the block -- usually called
 * when an event block is dragged or resized
 * The updated event is then passed back to the backend for saving
 * If the save operation fails, the event can be restored from 
 * the backup copy of the CalEventData in the event's dataOrig property
 */
HasTimeBlock.prototype.updateEvent = function(ev, dragMode) {

    var evStart = Cal.calcDateFromPos(this.left);
    var diff = this.auxDivList.length;
    var evEnd = Date.add('d', diff, evStart);
    var startTime = Cal.calcTimeFromPos(this.top);
    // Add +1 to height for border on background
    // Add +2 to height for border on block div
    var endTime = Cal.calcTimeFromPos(this.top+(this.height + 3));

    evStart.setHours(Cal.extractHourFromTime(startTime));
    evStart.setMinutes(Cal.extractMinutesFromTime(startTime));
    evEnd.setHours(Cal.extractHourFromTime(endTime));
    evEnd.setMinutes(Cal.extractMinutesFromTime(endTime));

    // Update ScoobyDates with new UTC values
    ev.data.start.updateFromUTC(evStart.getTime());
    ev.data.end.updateFromUTC(evEnd.getTime());
    return true;
}

/**
 * Insert a new event block
 * This method places the block (single- or multi-div) on the
 * scrollable area for normal events. This just puts them on the 
 * canvas in a hidden state. After this we have two more steps:
 * (1) Update block to reflect event's times using updateFromEvent
 * (2) Do sizing/positioning, and turn on visibility with updateDisplayMain
 */
HasTimeBlock.prototype.insert = function(id) {

    var ev = Cal.eventRegistry.getItem(id);
    var startDay = 0;
    var endDay = 0;
    var auxDivCount = 0;
    var blockDiv = null;
    var blockDivSub = null;
    var d = null;
    var view = null;
    
    if (ev.startsBeforeViewRange()) {
        startDay = 0;
        this.startsBeforeViewRange = true;
    }
    else {
        startDay = ev.data.start.getLocalDay();
    }
    if (ev.endsAfterViewRange()) {
        endDay = 6;
        this.endsAfterViewRange = true;
    }
    else {
        endDay = ev.data.end.getLocalDay();
    }
    auxDivCount = (endDay - startDay);
    
    this.idPrefix = Cal.ID_SEPARATOR + id;
    this.width = 1; 
    this.auxDivList = [];
    
    // Append event block to appropriate screen area for the type of Block
    view = document.getElementById('timedContentDiv');

    blockDiv = document.createElement('div');
    blockDivSub = document.createElement('div');

    // Event lozenge main div and components
    // -----------------------
    // Main lozenge div
    blockDiv.id = this.divId + this.idPrefix;
    blockDiv.className = 'eventBlock';
    blockDiv.style.width = this.width + 'px';

    /*
    // Just a small little bit of fun to change the border style of a block
    // depending on the status of the event.
    if (ev.data.status && ev.data.status.indexOf('TENTATIVE') > -1) {
        blockDiv.style.borderStyle = 'dashed';
    }
    */

    // Resize-up handle
    blockDivSub.id = this.divId + 'Top' + this.idPrefix;
    blockDivSub.className = 'eventResizeTop';
    blockDivSub.style.height = BLOCK_RESIZE_LIP_HEIGHT + 'px';
    blockDiv.appendChild(blockDivSub);

    // Central content area
    blockDivSub = document.createElement('div');
    blockDivSub.id = this.divId + 'Content' + this.idPrefix;
    blockDivSub.className = 'eventContent';
    blockDivSub.style.marginLeft = BLOCK_RESIZE_LIP_HEIGHT + 'px';
    blockDivSub.style.marginRight = BLOCK_RESIZE_LIP_HEIGHT + 'px';

    // Start time display
    d = document.createElement('div');
    d.id = this.divId + 'Start' + this.idPrefix;
    d.className = 'eventTime';
    d.style.width = '100%'; // Needed for IE, which sucks
    blockDivSub.appendChild(d);

    // Title
    d = document.createElement('div');
    d.id = this.divId + 'Title' + this.idPrefix
    d.className = 'eventTitle';
    d.style.width = '100%'; // Needed for IE, which sucks
    blockDivSub.appendChild(d);

    blockDiv.appendChild(blockDivSub);

    // Before adding the bottom resize handle, add any intervening
    // auxilliary div elems for multi-day events
    // ------------------
    // Multi-day events -- for events that extend past the end of
    // the week, truncate number of added div elements
    // auxDivCount = auxDivCount > maxDiff ? maxDiff : auxDivCount;
    if (auxDivCount) {
        for (var i = 0; i < auxDivCount; i++) {
            // Append previous div
            view.appendChild(blockDiv);

            var blockDiv = document.createElement('div');
            blockDiv.id = this.divId + Cal.ID_SEPARATOR +  +
                'aux' + (i+1) + this.idPrefix;
            blockDiv.className = 'eventBlock';
            blockDiv.style.width = this.width + 'px';

            // Central content area
            blockDivSub = document.createElement('div');
            blockDivSub.id = this.divId + 'Content' + this.idPrefix;
            blockDivSub.className = 'eventContent';
            blockDiv.appendChild(blockDivSub);

            // Don't set height to 100% for empty content area of last aux div
            // It has resize handle at the bottom, so empty content area
            // gets an absolute numeric height when the Block gets placed and
            // sized in updateFromEvent
            if (this.endsAfterViewRange || (i < (auxDivCount-1))) {
                blockDivSub.style.height = '100%';
            }
        }
    }

    // Resize-down handle -- append either to single div,
    // or to final div for multi-day event -- don't append when
    // event extends past view area
    if (!this.endsAfterViewRange) {
        blockDivSub = document.createElement('div');
        blockDivSub.id = this.divId + 'Bottom' + this.idPrefix;
        blockDivSub.className = 'eventResizeBottom';
        blockDivSub.style.height = BLOCK_RESIZE_LIP_HEIGHT + 'px';
        blockDiv.appendChild(blockDivSub);
    }

    view.appendChild(blockDiv);

    // DOM node references
    this.div = document.getElementById(this.divId + this.idPrefix);
    this.innerDiv = document.getElementById(this.divId + 'Content' +
        this.idPrefix);
    // DOM node refs for multi-day divs
    if (auxDivCount) {
        for (var i = 0; i < auxDivCount; i++) {
            this.auxDivList[i] = document.getElementById(this.divId +
                Cal.ID_SEPARATOR +  + 'aux' + (i+1) + this.idPrefix);
        }
    }
    // All done
    return this.div;
}

/**
 * Removes the block -- including multiple divs for multi-day events
 */
HasTimeBlock.prototype.remove = function(id) {
    this.innerDiv.parentNode.removeChild(this.innerDiv);
    this.div.parentNode.removeChild(this.div);
    if (this.auxDivList.length) {
        for (var i = 0; i < this.auxDivList.length; i++) {
            auxDiv = this.auxDivList[i];
            auxDiv.parentNode.removeChild(auxDiv);
            this.auxDivList[i] = null;
        }
    }
    // Close IE memleak hole
    this.div = null;
    this.innerDiv = null;
}

/**
 * Move the left side of the block to the given pixel position
 * *** Note: the pos is passed in instead of using the property
 * because during dragging, we don't continuously update the
 * block properties -- we only update them on drop ***
 * @param pos The X pixel position for the block
 */
HasTimeBlock.prototype.setLeft = function(pos) {
    var leftPos = parseInt(pos);
    var auxDiv = null;
    this.div.style.left = leftPos + 'px';
    if (this.auxDivList.length) {
        for (var i = 0; i < this.auxDivList.length; i++) {
            leftPos += Cal.dayUnitWidth;
            auxDiv = this.auxDivList[i];
            auxDiv.style.left = leftPos + 'px';
        }
    }
}

/**
 * Move the top side of the block to the given pixel position
 * *** Note: the pos is passed in instead of using the property
 * because during dragging, we don't continuously update the
 * block properties -- we only update them on drop ***
 * @param pos The Y pixel position for the block
 */
HasTimeBlock.prototype.setTop = function(pos) {
    this.div.style.top = parseInt(pos) + 'px';
}

/**
 *
 */
HasTimeBlock.prototype.setWidth = function(width) {
    var w = parseInt(width);
    this.div.style.width = w + 'px';
    if (this.auxDivList.length) {
        for (var i = 0; i < this.auxDivList.length; i++) {
            auxDiv = this.auxDivList[i];
            auxDiv.style.width = w + 'px';
        }
    }
}

/**
 * Sizes an event block vertically -- or the starting and ending
 * blocks for a multi-day event. Note: in the case of a
 * multi-day event where the start time is later than the end time,
 * you will have a NEGATIVE value for 'size', which is WHAT YOU WANT.
 * @param size Int difference in start and end positions of the
 * event block, or of start and end blocks for a multi-day event
 */
HasTimeBlock.prototype.setHeight = function(size, overrideMulti) {
    var doMulti = ((this.auxDivList.length || this.endsAfterViewRange)
        && !overrideMulti);
    var mainSize = 0;
    var lastAuxSize = 0;

    // Do the head-scratching math stuff
    // -----------------------------------
    // Multi-day event
    if (doMulti) {
        // Height applied to FIRST div -- this div should stretch
        // all the rest of the way to the bottom of the scrolling area
        mainSize = (VIEW_DIV_HEIGHT - this.top);
        // Height applied to FINAL div -- this div should stretch
        // from the top of the scrolling area to the bottom of where the
        // normal size would be for a single-day event
        lastAuxSize = (this.top + size);
        lastAuxSize = lastAuxSize < this.unit ? this.unit : lastAuxSize;
    }
    // Single-day event
    else {
        // Set height for single div using the passed-in size
        size = size < this.unit ? this.unit : size;
        mainSize = size;
    }

    // Set the values
    // -----------------------------------
    // Main div and the inner content div
    this.div.style.height = mainSize + 'px';
    this.innerDiv.style.height =
        (mainSize-(BLOCK_RESIZE_LIP_HEIGHT*2)) + 'px';
    // If multi-day event, do the inner aux divs and final aux div
    if (doMulti) {
        for (var i = 0; i < this.auxDivList.length; i++) {
            auxDiv = this.auxDivList[i];
            // Inner aux div(s)
            if (this.endsAfterViewRange || (i < (this.auxDivList.length-1))) {
                auxDiv.style.height = VIEW_DIV_HEIGHT + 'px';
            }
            // Final aux div
            else if (i == (this.auxDivList.length-1)) {
                // Main outer div
                auxDiv.style.height = lastAuxSize + 'px';
                // Empty internal content div
                auxDiv.firstChild.style.height =
                    (lastAuxSize-BLOCK_RESIZE_LIP_HEIGHT) + 'px';
            }
        }
    }
}

/**
 * Position and resize the block, and turn on its visibility
 */
HasTimeBlock.prototype.updateElements = function() {
    this.setLeft(this.left);
    this.setTop(this.top);
    this.setHeight(this.height);
    this.setWidth(this.width);
    this.makeVisible();
}

HasTimeBlock.prototype.makeVisible = function() {
    // Turn on visibility for all the divs
    this.div.style.visibility = 'visible';
    if (this.auxDivList.length) {
        for (var i = 0; i < this.auxDivList.length; i++) {
            auxDiv = this.auxDivList[i];
            auxDiv.style.visibility = 'visible';
        }
    }
}

/**
 * Get the pixel position of the top of the block div, or for
 * the far-left div in a multi-day event
 */
HasTimeBlock.prototype.getTop = function() {
    var t = this.div.offsetTop;
    return parseInt(t);
}

/**
 * Get the pixel posiiton of the bottom of the block div, or for
 * the far-right div in a multi-day event
 */
HasTimeBlock.prototype.getBottom = function() {

    var t = 0;
    var h = 0;
    var lastAux = null;
    var ret = 0;

    // Multi-day event
    if (this.auxDivList.length) {
        lastAux = this.auxDivList[this.auxDivList.length-1];
        ret = parseInt(lastAux.offsetHeight);
    }
    // Single-day event
    else {
        t = this.div.offsetTop;
        h = this.div.offsetHeight;
        ret = parseInt(t+h);
    }
    return ret;
}

/**
 * Get the pixel position of the far-left edge of the event block
 * or blocks in a muli-day event
 */
HasTimeBlock.prototype.getLeft = function() {
    var l = this.div.offsetLeft;
    return parseInt(l);
}

/**
 * NoTimeBlock -- sub-class of Block
 * All-day events, 'any-time' events -- these sit up in the
 * resizable area at the top of the UI
 */
function NoTimeBlock(id) {
    this.id = id;
}
NoTimeBlock.prototype = new Block();

// All-day events are a fixed height --
// I just picked 16 because it looked about right
NoTimeBlock.prototype.height = 16;
// Div elem prefix -- all component divs of normal event blocks
// begin with this
NoTimeBlock.prototype.divId = 'eventDivAllDay';

/**
 * Change block color to 'processing' color
 * Change the cursors for the resize handles at top and bottom,
 * and for the central content div
 * Display status animation
 */
NoTimeBlock.prototype.showProcessing = function() {
    this.setState(true);
    this.mainAreaCursorChange(true);
    this.showStatusAnim();
}

/**
 * Return the block to normal after processing
 */
NoTimeBlock.prototype.hideProcessing = function() {
    this.setState(false);
    this.mainAreaCursorChange(false);
}

/**
 * Update the block properties from an event
 * Called when editing from the form, or on drop after resizing/dragging
 * the block has to be updated to show the changes to the event
 */
NoTimeBlock.prototype.updateFromEvent = function(ev, temp) {
    var diff = ScoobyDate.diff('d', ev.data.start, ev.data.end) + 1;

    this.left = this.getPlatonicLeft();
    this.width = (diff*Cal.dayUnitWidth)-3;
    if (!temp) {
        this.top = ev.allDayRow*19;
    }
}

/**
 * Update an event from changes to the block -- usually called
 * when an event block is dragged or resized
 * The updated event is then passed back to the backend for saving
 * If the save operation fails, the event can be restored from
 * the backup copy of the CalEventData in the event's dataOrig property
 */
NoTimeBlock.prototype.updateEvent = function(ev, dragMode) {
    // Dragged-to date
    var evDate = Cal.calcDateFromPos(this.left);
    // Difference in days
    var diff = Date.diff('d', ev.data.start.getTime(), evDate.getTime());
    // Increment start and end by number of days
    // User can't resize all-day events
    ev.data.start.add('d', diff);
    ev.data.end.add('d', diff);

    return true;
}

/**
 * Calculate the width of an all-day event block -- for events that
 * have an end past the current view span, make sure the width truncates
 * at the end of the view span properly -- this is currently hard-coded
 * to Saturday.
 * TO-DO: Check the view type to figure out the end of the view span
 */
NoTimeBlock.prototype.calcWidth = function(startDay, ev) {

    var diff = 0;
    var maxDiff = (7-startDay);
    var width = 0;

    diff = (ScoobyDate.diff('d', ev.data.start, ev.data.end))+1;

    diff = (diff > maxDiff) ? maxDiff : diff;
    width = (diff*Cal.dayUnitWidth)-1;

    return width;
}

/**
 * Insert a new event block
 * This method places the block on the resizable area for
 * all-day events. This just puts them on the canvas in a hidden state.
 * After this we have two more steps:
 * (1) Update block to reflect event's times using updateFromEvent
 * (2) Do sizing/positioning, and turn on visibility with updateDisplayMain
 */
NoTimeBlock.prototype.insert = function(id) {
    var ev = Cal.eventRegistry.getItem(id);
    var blockDiv = document.createElement('div');
    var blockDivSub = document.createElement('div');
    var d = null;
    var view = null;

    this.idPrefix = Cal.ID_SEPARATOR + id;
    this.width = 1;

    // Append event block to appropriate screen area for the type of Block
    view = document.getElementById('allDayContentDiv');
    // Event lozenge main div and components
    // -----------------------
    // Main lozenge div
    blockDiv.id = this.divId + this.idPrefix;
    blockDiv.className = 'eventBlock';
    // Set other style props separately because setAttribute() is broken in IE
    blockDiv.style.width = this.width + 'px';

    // Central content area
    blockDivSub.id = this.divId + 'Content' + this.idPrefix;
    blockDivSub.className = 'eventContent';
    blockDivSub.style.whiteSpace = 'nowrap';
    
    // Title
    d = document.createElement('div');
    d.id = this.divId + 'Title' + this.idPrefix;
    d.className = 'eventTitle';
    d.style.marginLeft = BLOCK_RESIZE_LIP_HEIGHT + 'px';
    blockDivSub.appendChild(d);
    
    blockDiv.appendChild(blockDivSub);

    view.appendChild(blockDiv);

    // DOM node references
    this.div = document.getElementById(this.divId + this.idPrefix);
    this.innerDiv = document.getElementById(this.divId + 'Content' +
        this.idPrefix);

    return this.div;
}

/**
 * Removes the block
 */
NoTimeBlock.prototype.remove = function(id) {
    this.innerDiv.parentNode.removeChild(this.innerDiv);
    this.div.parentNode.removeChild(this.div);
    this.div = null;
    this.innerDiv = null;
}

/**
 * Move the left side of the block to the given pixel position
 * *** Note: the pos is passed in instead of using the property
 * because during dragging, we don't continuously update the
 * block properties -- we only update them on drop ***
 * @param pos The X pixel position for the block
 */
NoTimeBlock.prototype.setLeft = function(pos) {
    this.div.style.left = parseInt(pos) + 'px';
}

/**
 * Move the top side of the block to the given pixel position
 * *** Note: the pos is passed in instead of using the property
 * because during dragging, we don't continuously update the
 * block properties -- we only update them on drop ***
 * @param pos The Y pixel position for the block
 */
NoTimeBlock.prototype.setTop = function(pos) {
    this.div.style.top = parseInt(pos) + 'px';
}

/**
 * Sets the pixel width of the all-day event block's
 * div element
 */
NoTimeBlock.prototype.setWidth = function(width) {
    this.div.style.width = parseInt(width) + 'px';
    // Needed for IE not to push the content out past
    // the width of the containing div
    this.innerDiv.style.width = parseInt(
        width - (BLOCK_RESIZE_LIP_HEIGHT*2)) + 'px';
}

/**
 * TO-DO: Figure out if this is needed anymore -- aren't these
 * a fixed height?
 */
NoTimeBlock.prototype.setHeight = function(size) {
    size = parseInt(size);
    this.div.style.height = size + 'px';
    this.innerDiv.style.height = size + 'px';
}

/**
 * Position and resize the block, and turn on its visibility
 */
NoTimeBlock.prototype.updateElements = function() {
    this.setLeft(this.left);
    this.setTop(this.top);
    this.setHeight(this.height);
    this.setWidth(this.width);
    this.makeVisible();
}

NoTimeBlock.prototype.makeVisible = function() {
    this.div.style.visibility = 'visible';
}

/**
 * TO-DO: Figure out if this is needed anymore
 */
NoTimeBlock.prototype.getTop = function() {
    var t = this.div.offsetTop;
    return parseInt(t);
}

/**
 * TO-DO: Figure out if this is needed anymore
 */
NoTimeBlock.prototype.getBottom = function() {
    var t = this.div.offsetTop;
    var h = this.div.offsetHeight;
    return parseInt(t+h);
}

/**
 * TO-DO: Figure out if this is needed anymore
 */
NoTimeBlock.prototype.getLeft = function() {
    var l = this.div.offsetLeft;
    return parseInt(l);
}

