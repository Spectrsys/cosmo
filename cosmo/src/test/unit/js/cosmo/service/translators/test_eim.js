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

dojo.provide("cosmotest.service.translators.test_eim");

dojo.require("cosmo.service.translators.eim");
dojo.require("cosmo.datetime.serialize");
//Initialization.
//TODO - once Dojo implements setUp() and tearDown() move this code there.

cosmotest.service.translators.test_eim = {
  
    test_parseRecordSet: function test_parseRecordSet(){
        var uid = "12345";
        var title = "o1";
        var triageStatus = "100";
        var autoTriage = "1";
        var rank = "-12345.67";
        var createdOn = "1178053319";
        var dtstartDateString = "00021231T000000";
        var dtstart = ";VALUE=DATE-TIME:" + dtstartDateString;
        var a = cosmotest.service.translators.test_eim.generateAtom(
            cosmotest.service.translators.test_eim.getEimRecordset({
              uuid: uid,
              title: title,
              triage: triageStatus + " " + rank + " " + autoTriage,
              createdOn: createdOn,
              dtstart: dtstart
              
            }));
        var objectList = cosmo.service.translators.eim.responseToObject(a);
        
        var o1 = objectList[uid];

        jum.assertEquals("uid does not match", uid, o1.getUid());
        jum.assertEquals("display name does not match title", title, o1.getDisplayName());
        jum.assertEquals("triage status does not match", triageStatus, o1.getTriageStatus());
        jum.assertEquals("auto triage does not match", autoTriage, o1.getAutoTriage());
        jum.assertEquals("triage rank does not match", rank, o1.getRank());
        jum.assertEquals("creation date does not match created on", createdOn, o1.getCreationDate());

        var e1 = o1.getStamp("event");
        jum.assertTrue("dtstart does not match start date", 
              cosmo.datetime.fromIso8601(dtstartDateString).equals(e1.getStartDate()));
              
    },
  
    generateAtom: function generateAtom(/*Object*/ content){
        
        var uuid = content.uuid;
        
        return cosmotest.service.translators.test_eim.toXMLDocument('<?xml version=\'1.0\' encoding=\'UTF-8\'?>' +
        '<feed xmlns="http://www.w3.org/2005/Atom" xmlns:xml="http://www.w3.org/XML/1998/namespace" xml:base="http://localhost:8080/cosmo/atom/">' +
        '<id>urn:uuid:56599b95-6676-4823-8c88-1eec17058f48</id>' +
        '<title type="text">Cosmo</title>' +
        '<generator uri="http://cosmo.osafoundation.org/" version="0.7.0-SNAPSHOT">Cosmo Sharing Server</generator>' +
        '<author><name>root</name><email>root@localhost</email></author>' +
        '<link rel="self" type="application/atom+xml" href="collection/56599b95-6676-4823-8c88-1eec17058f48/full/eim-json" />' +
        '<link rel="alternate" type="text/html" href="http://localhost:8080/cosmo/pim/collection/56599b95-6676-4823-8c88-1eec17058f48" />' +
        '<entry><id>urn:uuid:' + uuid + '</id>' +
        '<title type="text">Welcome to Cosmo</title>' +
        '<updated>2007-05-01T21:01:59.535Z</updated><published>2007-05-01T21:01:59.535Z</published>' +
        '<link rel="self" type="application/atom+xml" href="item/' + uuid + '/full/eim-json" />' +
        '<content type="application/eim+json">' + 
        content.toSource().slice(1,-1) + 
        '</content>' +
        '<link rel="edit" type="application/atom+xml" href="item/' + uuid + '"/>' +
        '<link rel="parent" type="application/atom+xml" href="collection/56599b95-6676-4823-8c88-1eec17058f48/full/eim-json" />' +
        '</entry></feed>')

    },
    
    toXMLDocument: function _toXMLDocument(/*String*/ text){
        if (window.ActiveXObject)
          {
          var doc=new ActiveXObject("Microsoft.XMLDOM");
          doc.async="false";
          doc.loadXML(text);
          }
        // code for Mozilla, Firefox, Opera, etc.
        else
          {
          var parser=new DOMParser();
          var doc=parser.parseFromString(text,"text/xml");
          }
          return doc;
    },
    
    getEimRecordset: function getEimRecordset(/*Object*/ props){
        props = props || {};
        
        var recordSet = 
        {"uuid":props.uuid || "0017e507-e087-487a-8eae-63afa7d865b5",
         "records":{
            "item":{"ns":"http://osafoundation.org/eim/item/0",
                    "key":{"uuid":["text", props.uuid || "0017e507-e087-487a-8eae-63afa7d865b5"]},
                    "fields":{"title":["text", props.title || "Welcome to Cosmo"],
                              "triage":["text", props.triage || "100 -1178053319.00 1"],
                              "hasBeenSent":["integer", props.hasBeenSent || "0"],
                              "needsReply":["integer", props.needsReply || "0"],
                              "createdOn":["decimal", props.createdOn || "1178053319"]}},
            "modby":{"ns":"http://osafoundation.org/eim/modifiedBy/0",
                     "key":{"uuid":["text", props.uuid || "0017e507-e087-487a-8eae-63afa7d865b5"],
                            "userid":["text", props.userid || "root@localhost"],
                            "timestamp":["decimal", props.timestamp || "1178053319"],
                            "action":["integer", props.action || "500"]}},
            "note":{"ns":"http://osafoundation.org/eim/note/0",
                    "key":{"uuid":["text", props.uuid || "0017e507-e087-487a-8eae-63afa7d865b5"]},
                    "fields":{"body":["clob", props.body || "Welcome to Cosmo"],
                              "icalUid":["text", props.icalUid || "dfad540e-eae5-4792-9d2d-d9642fcfc411"]}},
            "event":{"ns":"http://osafoundation.org/eim/event/0",
                     "key":{"uuid":["text", props.uuid || "0017e507-e087-487a-8eae-63afa7d865b5"]},
                     "fields":{"dtstart":["text", props.dtstart || ";VALUE=DATE-TIME:00021231T000000"],
                               "duration":["text", props.duration || "PT0S"],
                               "location":["text", props.location || ""],
                               "rrule":["text", props.rrule || null],
                               "exrule":["text",props.exrule || null],
                               "rdate":["text", props.rdate || null],
                               "exdate":["text", props.exdate || null],
                               "status":["text", props.status || null]}}}};
        return recordSet;
        
    }
}