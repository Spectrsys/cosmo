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
 * @fileoverview Button - creates a push button that can be enabled or disabled, small or 
 *                        normal size.
 *                        Adapated from mde's button.js code.
 * @author Bobby Rullo br@osafoundation.org
 * @license Apache License 2.0
 */

dojo.require("dojo.widget.*");
dojo.require("dojo.event.*");
dojo.require("dojo.dom");
dojo.require("scooby.env");

dojo.provide("scooby.ui.widget.Button");

dojo.widget.defineWidget("scooby.ui.widget.Button", dojo.widget.HtmlWidget, {
    //Constants
    DISABLED_TABLE_OPACITY : 0.6,

    //FIXME - deal with skins
    buttonDirectory : scooby.env.getTemplateBase() + "Button/",
    templatePath : dojo.uri.dojoUri( "../../scooby/ui/widget/templates/Button/Button.html"),
    templateCssPath : dojo.uri.dojoUri("../../scooby/ui/widget/templates/Button/Button.css"),

    //attach points
    leftContainer: null,
    centerContainer: null,
    rightContainer: null,
    buttonTextContainer : null,
    tableContainer : null,
    
    //properties to be set by tag or constructor
    enabled : true,
    small : false,
    text : "",
    width : 100,
    handleOnClick: "",
    
    fillInTemplate: function(){
         if (typeof(this.handleOnClick) == "string"){
             eval("this.handleOnClick = function(){"+ this.handleOnClick +";}");
         }
         this.setText(this.text);
         this.setWidth(this.width);
         this.setEnabled(this.enabled);
    },
    
    setText: function(text){
        this.text = text;
        var textNode = document.createTextNode(text);
        if (this.buttonTextContainer.hasChildNodes){
		    dojo.dom.removeChildren(this.buttonTextContainer);
		}
		this.buttonTextContainer.appendChild(textNode);
    },
    
    setWidth: function(width){
        this.width = width;
        this.tableContainer.style.width = width + "px";
    },
    
    setEnabled: function(enabled){
        this.enabled = enabled;
        
        if (enabled){
            this._setTableOpacity(1.0) 
            
        } else {
            this._setTableOpactiy(this.DISABLED_TABLE_OPACITY);
        }
        
        this._setButtonImages();
        
    },
    
    getButtonHeight: function(){
        return this.small ? 18 : 24;
    },
    
    getCapWidth: function(){
        return this.small ? 9 : 10;
    },
    
    _setButtonImages: function(lit){
        this.leftContainer.style.background="url('"+this._getLeftButtonImagePath(this.enabled, this.small, lit)+"')";
        this.centerContainer.style.background="url('"+this._getCenterButtonImagePath(this.enabled, this.small, lit)+"')";
        this.rightContainer.style.background="url('"+this._getRightButtonImagePath(this.enabled, this.small, lit)+"')";
    },

    _getCenterButtonImagePath: function(enabled, small, lit){
		return this._getButtonPath("center", enabled, small, lit);
    },
    
    _getLeftButtonImagePath : function(enabled, small, lit){
		return this._getButtonPath("left", enabled, small, lit);
    },
    
    _getRightButtonImagePath: function(enabled, small, lit){
		return this._getButtonPath("right", enabled, small, lit);
    },
    
    _getButtonPath: function(leftRightCenter, enabled, small, lit){
        var path = this.buttonDirectory + "button_" + leftRightCenter;

        if (!enabled){
            path += "_dim";
        }
        
        if (lit){
            path += "_lit";
        }
        
        if (small) {
            path += "_sm";
        }
        
        path += ".gif";
        
        return path;
    },
    
    _handleMouseOver: function(){
         if (this.enabled){
             this._setButtonImages(true);
         }
    },
    
    _handleMouseOut: function(){
         if (this.enabled){
             this._setButtonImages();
         }
    },
    
    _handleOnClick: function(){
        if (this.enabled){
           this.handleOnClick();
        }
    },
    
    _setTableOpacity: function(tableOpacity){
	    this.tableContainer.style.opacity = tableOpacity;
	    if (document.all){
	        this.tableContainer.style.filter = "alpha(opacity="+ tableOpacity * 100 +")";
	    }
    }
    
  },

  "html",
  
  function() {
  }

);