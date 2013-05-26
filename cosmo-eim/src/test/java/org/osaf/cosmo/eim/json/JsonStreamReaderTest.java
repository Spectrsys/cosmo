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
package org.osaf.cosmo.eim.json;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordSet;
import org.osaf.cosmo.eim.TextField;
import org.osaf.cosmo.eim.eimml.EimmlStreamReader;
import org.osaf.cosmo.model.mock.TestHelper;

/**
 * Test Case for {@link EimmlStreamReader}.
 */
public class JsonStreamReaderTest extends TestCase
    implements JsonConstants {
    private static final Log log =
        LogFactory.getLog(JsonStreamReaderTest.class);

    private TestHelper testHelper = new TestHelper();

    byte[] unicode = new byte[] {
        (byte) 0xC3, (byte) 0xA5, (byte) 0xC3, (byte) 0x9F, (byte) 0xE2, (byte) 0x88, (byte) 0x82, (byte) 0xC6,
        (byte) 0x92, (byte) 0xC2, (byte) 0xA9, (byte) 0xCB, (byte) 0x99, (byte) 0xE2, (byte) 0x88, (byte) 0x86,
        (byte) 0xCB, (byte) 0x9A, (byte) 0xC2, (byte) 0xAC, (byte) 0xE2, (byte) 0x80, (byte) 0xA6, (byte) 0xE2,
        (byte) 0x80, (byte) 0xA8, (byte) 0xE2, (byte) 0x80, (byte) 0xA9
    };

    public void testReadUnicode() throws Exception {
        Reader in =
            testHelper.getReader("json/unicode-record-set.json");
        JsonStreamReader reader = new JsonStreamReader(in);
        EimRecordSet recordSet = reader.nextRecordSet();
        EimRecord record = recordSet.getRecords().get(0);

        byte[] unicodeResult = ((TextField)record.getFields().get(0)).getText().getBytes();
        for (int x = 0; x < unicode.length; x++){
            assertEquals(unicode[x], unicodeResult[x]);
        }
        assertTrue(Arrays.equals(unicode, unicodeResult));
    }
    
    public void testReadChandlerUpdate() throws Exception {
        Reader in =
            testHelper.getReader("json/simple-record-set.json");
        JsonStreamReader reader = new JsonStreamReader(in);
        EimRecordSet recordSet = reader.nextRecordSet();
        List<EimRecord> deletedRecords = getDeletedRecords(recordSet);
        assertEquals(1, deletedRecords.size());
        EimRecord deletedRecord = deletedRecords.get(0);
        assertEquals("deletedRecord", deletedRecord.getPrefix());
        assertEquals("http://deletedRecord.com", deletedRecord.getNamespace());
    }
    
    private List<EimRecord> getDeletedRecords(EimRecordSet recordSet){
        List<EimRecord> deletedRecords = new ArrayList<EimRecord>();
        for (EimRecord record : recordSet.getRecords()){
            if (record.isDeleted()){
                deletedRecords.add(record);
            }
        }
        return deletedRecords;
    }
}
