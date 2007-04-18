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
package org.osaf.cosmo.atom.generator;

import org.apache.abdera.Abdera;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.atom.AtomConstants;
import org.osaf.cosmo.server.ServiceLocator;

/**
 * Standard implementation of <code>GeneratorFactory</code>.
 *
 * @see GeneratorFactory
 * @see FeedGenerator
 */
public class StandardGeneratorFactory
    implements GeneratorFactory, AtomConstants {
    private static final Log log =
        LogFactory.getLog(StandardGeneratorFactory.class);

    private Abdera abdera;
    private ContentFactory contentFactory;

    // GeneratorFactory methods

    /**
     * Creates an instance of <code>FeedGenerator</code> that can
     * service the given projection and format.
     * <p>
     * The following feed generators are supported:
     * <dl>
     * <dt>{@link AtomConstants#PROJECTION_BASIC}</dt>
     * <dd>{@link BasicFeedGenerator}</dd>
     * <dt>{@link AtomConstants#PROJECTION_FULL}</dt>
     * <dd>{@link FullFeedGenerator}</dd>
     * </dl>
     * <p>
     * If no projection is specified, the basic feed generator is
     * returned.
     * <p>
     * The following data formats are supported:
     * <dl>
     * <dt>{@link AtomConstants#FORMAT_EIM_JSON}</dt>
     * <dd>EIM over JSON</dd>
     * </dl>
     * <p>
     * The basic feed generator ignores the format argument. For all
     * other generators, if no format is provided, EIM over JSON is
     * used.
     *
     * @param projection the projection name
     * @param format the format name
     * @param serviceLocator the service locator from which feed URLs
     * are calculated
     * @return the feed generator, or null if no generator is
     * supported for the named projection
     */
    public FeedGenerator createFeedGenerator(String projection,
                                             String format,
                                             ServiceLocator serviceLocator)
        throws UnsupportedProjectionException, UnsupportedFormatException {
        if (projection == null ||
            projection.equals(PROJECTION_BASIC))
            return new BasicFeedGenerator(abdera.getFactory(), contentFactory,
                                          serviceLocator);
        if (projection.equals(PROJECTION_FULL))
            return new FullFeedGenerator(abdera.getFactory(), contentFactory,
                                         serviceLocator, format);
        throw new UnsupportedProjectionException(projection);
    }

    // our methods

    public Abdera getAbdera() {
        return abdera;
    }

    public void setAbdera(Abdera abdera) {
        this.abdera = abdera;
    }

    public ContentFactory getContentFactory() {
        return contentFactory;
    }

    public void setContentFactory(ContentFactory factory) {
        contentFactory = factory;
    }
}
