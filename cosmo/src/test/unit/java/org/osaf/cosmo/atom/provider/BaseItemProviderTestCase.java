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
package org.osaf.cosmo.atom.provider;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.model.EventStamp;
import org.osaf.cosmo.model.NoteItem;

/**
 * Base class for for {@link ItemProvider} tests.
 */
public abstract class BaseItemProviderTestCase extends BaseProviderTestCase {
    private static final Log log =
        LogFactory.getLog(BaseItemProviderTestCase.class);

    protected BaseProvider createProvider() {
        ItemProvider provider = new ItemProvider();
        provider.setProcessorFactory(helper.getProcessorFactory());
        provider.setContentService(helper.getContentService());
        return provider;
    }

    protected Properties serialize(NoteItem item) {
        Properties props = new Properties();

        props.setProperty("uid", item.getUid());

        EventStamp es = EventStamp.getStamp(item);
        if (es != null)
            props.setProperty("startDate", es.getStartDate().toString());

        return props;
    }
}
