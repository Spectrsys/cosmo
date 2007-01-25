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

dojo.provide("cosmo.ui.conf");

dojo.require("cosmo.env");
// Configurable UI options


dj_global.TEMPLATE_DIRECTORY = '/default'; // Template directory to use
dj_global.DISPLAY_WIDTH_PERCENT = 1.0; // Percent of window width to draw entire display
dj_global.DISPLAY_HEIGHT_PERCENT = 1.0; // Percent of window height to draw entire display
dj_global.TOP_MENU_HEIGHT = 48; // Height for top navigation area -- Month name and nav arrows
dj_global.LEFT_SIDEBAR_WIDTH = 172; // Width of lefthand sidebar
dj_global.RIGHT_SIDEBAR_WIDTH = 256; // Width of righthand sidebar
dj_global.CAL_TOP_NAV_HEIGHT = 36; // Height for top navigation area -- Month name and nav arrows
dj_global.DAY_LIST_DIV_HEIGHT = 16; // Height for list of days/dates for each day col
dj_global.ALL_DAY_RESIZE_HANDLE_HEIGHT = 8; // Height for resizer handle under area for 'no time' events
dj_global.ALL_DAY_RESIZE_AREA_HEIGHT = 60-ALL_DAY_RESIZE_HANDLE_HEIGHT;
dj_global.HOUR_UNIT_HEIGHT = 80; // Height of one hour block in each day col
dj_global.HOUR_DIV_HEIGHT = (HOUR_UNIT_HEIGHT - 1); // Allow one px for border on outside of box per retarded CSS spec
dj_global.VIEW_DIV_HEIGHT = (HOUR_UNIT_HEIGHT*24); // 24 hours' worth of height
dj_global.BLOCK_RESIZE_LIP_HEIGHT = 6; // Height in pixels of resizeable 'grab' area at top and bottom of block
dj_global.EVENT_DETAIL_FORM_HEIGHT = 380;
dj_global.EVENT_INFO_FORM_WIDTH = RIGHT_SIDEBAR_WIDTH-12; // Width of the form on the right that displays info for the selected event
dj_global.SCROLLBAR_SPACER_WIDTH = 20; // Spacer to prevent horizontal scrollbar in view pane
dj_global.HOUR_LISTING_WIDTH = 42; // Space for the vertical listing of hours on the left of the events
dj_global.PROCESSING_ANIM_HEIGHT = 36;
dj_global.PROCESSING_ANIM_WIDTH = 120;
dj_global.DIALOG_BOX_WIDTH = 380;
dj_global.DIALOG_BOX_HEIGHT = 280;
dj_global.LOGO_GRAPHIC = 'logo_main.gif';
dj_global.LOGO_GRAPHIC_SM = 'logo_sm.gif';
dj_global.BUTTON_DIR_PATH = cosmo.env.getBaseUrl() + '/templates' + TEMPLATE_DIRECTORY + '/images/';

