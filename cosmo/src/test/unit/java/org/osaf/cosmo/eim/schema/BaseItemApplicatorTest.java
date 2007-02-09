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
package org.osaf.cosmo.eim.schema;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.eim.EimRecord;
import org.osaf.cosmo.eim.EimRecordField;
import org.osaf.cosmo.eim.TextField;
import org.osaf.cosmo.eim.schema.BaseApplicatorTestCase;

/**
 * Test Case for {@link BaseItemApplicator}.
 */
public class BaseItemApplicatorTest extends BaseApplicatorTestCase {
    private static final Log log =
        LogFactory.getLog(BaseItemApplicatorTest.class);

    private static final String PREFIX = "cosmo";
    private static final String NAMESPACE = "cosmo";

    private TestApplicator applicator;

    protected void setUp() throws Exception {
        applicator = new TestApplicator(PREFIX, NAMESPACE);
    }

    public void testApplyRecordNullNamespace() throws Exception {
        try {
            applicator.applyRecord(new EimRecord(null, null));
            fail("Applied record with null namespace");
        } catch (IllegalArgumentException e) {}
    }

    public void testApplyRecordWrongNamespace() throws Exception {
        try {
            applicator.applyRecord(new EimRecord("bad", "bad"));
            fail("Applied record with null namespace");
        } catch (IllegalArgumentException e) {}
    }

    public void testApplyDeletedRecord() throws Exception {
        EimRecord record = new EimRecord(PREFIX, NAMESPACE);
        record.setDeleted(true);
        try {
            applicator.applyRecord(record);
            fail("Applied deleted record");
        } catch (EimSchemaException e) {}
    }

    public void testApplyFields() throws Exception {
        EimRecord record = new EimRecord(PREFIX, NAMESPACE);
        TextField field = new TextField("foo", "bar");
        record.addField(field);

        try {
            applicator.expectField(field);
            applicator.applyRecord(record);
        } catch (IllegalArgumentException e) {
            fail("Applicator did not apply field " + field);
        }
    }

    private class TestApplicator extends BaseItemApplicator {
        private EimRecordField expectedField;

        public TestApplicator(String prefix,
                              String namespace) {
            super(prefix, namespace, null);
        }

        protected void applyField(EimRecordField field)
            throws EimSchemaException {
            if (! field.equals(expectedField))
                throw new IllegalArgumentException("field " + field + " does not match expected field " + expectedField);
        }

        public void expectField(EimRecordField field) {
            this.expectedField = field;
        }
    }
}
