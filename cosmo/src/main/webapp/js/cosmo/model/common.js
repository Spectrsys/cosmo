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
dojo.provide("cosmo.model.common");

dojo.require("cosmo.model.util");
dojo.require("cosmo.datetime.serialize");

/**
 * A recurrence rule specifies how to repeat a given event.
 */
cosmo.model.declare("cosmo.model.RecurrenceRule", null, 
    [["frequency", {"default": null}],
     ["endDate", {"default": null} ],
     ["isSupported", {"default": true}],
     ["unsupportedRule", {"default": null}]
    ], 
    {
        initializer: function (kwArgs){
            this.initializeProperties(kwArgs);
        },
        
        isSupported: function isSupported(){
            return this.getIsSupported();  
        },
    
        equals: function(other){
            //TODO
            dojo.unimplemented();
        }
    },
    {
        immutable: true
    });

dojo.lang.mixin(cosmo.model.RecurrenceRule, {
    FREQUENCY_DAILY: "daily",
    FREQUENCY_WEEKLY: "weekly",
    FREQUENCY_BIWEEKLY: "biweekly",
    FREQUENCY_MONTHLY: "monthly",
    FREQUENCY_YEARLY: "yearly"
});

cosmo.model.declare("cosmo.model.Duration", null, 
    [["year",   {"default":0} ],
     ["month",  {"default":0} ],
     ["week",   {"default":0} ],
     ["day",    {"default":0} ],
     ["hour",   {"default":0} ],
     ["second", {"default":0} ],
     ["minute", {"default":0} ]
     ],
    {
        initializer:function(){
            //summary: create a new Duration using either the difference between two dates
            //         or kwArgs for the properties or a string with a iso8601 duration
            var kwArgs = null;
            if (arguments[0] instanceof cosmo.datetime.Date){
                var date1 = arguments[0];
                var date2 = arguments[1];
                kwArgs = cosmo.datetime.getDuration(date1,date2);
            } else if (typeof arguments[0] == "string"){
                kwArgs = cosmo.datetime.parseIso8601Duration(arguments[0]);
            } else {
                //arg[0] had better be an object!
                kwArgs = arguments[0];
            }
            
            this.initializeProperties(kwArgs);
            
        }
    }, 
    {
        immutable: true
    });
