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
dojo.provide("cosmo.model.util");

cosmo.model.util._upperFirstChar = function(str){
    return str.charAt(0).toUpperCase() + str.substr(1,str.length -1 );
}

cosmo.model.util.BasePropertyApplicator = function(){}
cosmo.model.util.BasePropertyApplicator.prototype = {

    enhanceClass: function(ctr, propertyArray, kwArgs){
        this.initializeClass(ctr, kwArgs);
        for (var x = 0; x < propertyArray.length; x++){
            this.addProperty.apply(this, [ctr, propertyArray[x][0], propertyArray[x][1]]);
        }
    },

    addProperty: function(ctr, propertyName, kwArgs){
        kwArgs = kwArgs || {};
        var upProp = cosmo.model.util._upperFirstChar(propertyName);
        var setterName = "set" + upProp;
        var getterName = "get" + upProp;

        ctr.prototype[setterName] = this.getSetter(ctr, propertyName, kwArgs);
        ctr.prototype[getterName] = this.getGetter(ctr, propertyName, kwArgs);
    },

    initializeClass: function(ctr, kwArgs){
        //implementers should override this.
    },

    getSetter: function(ctr, propertyName, kwArgs){
        //implementers should override this.
        return null;
    },

    getGetter: function(ctr, propertyName, kwArgs){
        //implementers should override this.
        return null;
    }
}

//TODO Make a clone() function
dojo.declare("cosmo.model.util.SimplePropertyApplicator", cosmo.model.util.BasePropertyApplicator, {
    addProperty: function(ctr, propertyName, kwArgs){
        kwArgs = kwArgs || {};
        this._inherited("addProperty", arguments);
        ctr.prototype.__propertyNames.push(propertyName);
        ctr.prototype.__defaults[propertyName] = kwArgs["default"];
    },

    getGetter: function(ctr, propertyName, kwArgs){
        return function(){
            return this.__getProperty(propertyName);
        }
    },

    getSetter: function(ctr, propertyName, kwArgs){
        return function(value){
            this.__setProperty(propertyName, value);
        }
    },

    initializeClass: function(ctr, kwArgs){
        if (ctr.prototype["__enhanced"]){
            //it's already been enhanced, which usually means that this is a subclass.
            //so let's just copy the arrays/objects to the new prototype
            //since we'll be adding to them and don't want to add to the parent's arrays/objects!
            ctr.prototype.__propertyNames = ctr.prototype.__propertyNames.slice();
            var newDefaults = {};
            dojo.lang.mixin(ctr.prototype.__defaults, newDefaults);
            ctr.prototype.__defaults = newDefaults;
            return;
        }
        if (!kwArgs){
            kwArgs = {};
        }
        ctr.prototype.__enhanced = true;
        ctr.prototype.__getProperty = this._genericGetter;
        ctr.prototype.__setProperty = this._genericSetter;
        ctr.prototype.__getDefault = this._getDefault;
        ctr.prototype.__propertyNames = [];
        ctr.prototype.__defaults = {};
        ctr.prototype.initializeProperties = this._initializeProperties;

        if (kwArgs["enhanceInitializer"]){
            var oldInitter = ctr.prototype.initializer;
            var when = kwArgs["enhanceInitializer"];
            //TODO use dojo AOP
            function newInitializer(){
                if (when == "before"){
                    this.initializeProperties(arguments);
                }

                oldInitter.apply(this,arguments);

                if (when != "before"){
                    this.initializeProperties(arguments);
                }
            }
            ctr.prototype.initializer = newInitializer;
        }
    },

    //These functions are "protected" - in other words they should only be used by this class,
    //or other classes in this package.
    _initializeProperties: function(kwProps){
        for (var x = 0; x < this.__propertyNames.length; x++){
            var propertyName = this.__propertyNames[x];
            if (dojo.lang.has(kwProps, propertyName)){
                this.__setProperty(propertyName, kwProps[propertyName]);
            } else {
                this.__setProperty(propertyName, this.__getDefault(propertyName));
            }
        }
    },

    _genericGetter: function genericGetter(propertyName){
        return this["_"+propertyName];
    },

    _genericSetter: function genericSetter(propertyName, value){
        this["_"+propertyName] = value;
    },

    _getDefault: function getDefault(propertyName){
        var propDefault = this.__defaults[propertyName];

        if (typeof(propDefault) == "function"){
            return propDefault();
        } else {
            return propDefault;
        }

        return propDefault;
    }
});

//instantiate the singleton
cosmo.model.util.simplePropertyApplicator = new cosmo.model.util.SimplePropertyApplicator();

dojo.declare("cosmo.model.util.InheritingSubclassCreator", null, {
    createSubClass: function(parentConstructor, childConstructorName,kwArgs, propertyArgsMap){
        dojo.declare(childConstructorName, parentConstructor, {
        });
    },

    //default functions
    _getParentDefault: function(){
       return this.parent;
    },

    _getterDefault: function(){

    }

});

cosmo.model.util.equals = function cosmoEquals(a,b){
    var type = typeof (a);
    if (type != typeof(b)){
        throw new Error("Both operands must be of the same type!\n You passed '"
           + a + "' and '" + b +"', a " + typeof(a) + " and a "+ typeof(b));
    }

    if (type == "object"){
       if (a == null){
           return b == null;
       }
       return a.equals(b);
    }

    return a == b;
}

  cosmo.model._occurrenceGetProperty =  function occurrenceGetProperty(propertyName){
        //get the master version
        var master = this.getMaster();
        var masterProperty = this._getMasterProperty(propertyName);

        //see if it's overridable
        //if it's not, just go right to the master
        if (this.__noOverride[propertyName]){
            return masterProperty;
        }

        //if it is check the modificaiton
        var modification = master.getModification(this.recurrenceId);

        //if no modification, return the master
        if (!modification){
            return masterProperty;
        }

        //there IS a modification, so let's check to see if it has
        //an overridden value for this particular property
        var modificationProperty = this._getModifiedProperty(propertyName);
        if (typeof(modificationProperty) != "undefined"){
            return modificationProperty;
        }
        return masterProperty;
}

cosmo.model._occurrenceSetProperty = function occurrenceSetProperty(propertyName, value){
    if (this.__noOverride[propertyName]){
        throw new Error("You can not override property '" + propertyName +"'");
    }

    var master = this.getMaster();

    var masterProperty = this._getMasterProperty(propertyName);

    //is there a modification?

    var modification = master.getModification(this.recurrenceId);
    if (modification){
        if (!typeof(this._getModifiedProperty(propertyName)) == "undefined"){
            this._setModifiedProperty(propertyName, value);
            return;
        } else if (!cosmo.model.util.equals(value, masterProperty)){
            this._setModifiedProperty(propertyName, value);
            return;
        }
    }
    //if the new value is the same as the master property,
    // no need to do anything
    if (cosmo.model.util.equals(value, masterProperty)){
        return;
    } else {
        var modification = new cosmo.model.Modification({
            recurrenceId: this.recurrenceId
        });
        master.addModification(modification);
        this._setModifiedProperty(propertyName, value);
    }
}