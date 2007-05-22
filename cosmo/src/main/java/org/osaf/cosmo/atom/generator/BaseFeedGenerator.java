/*
 * Copyright 2006-2007 Open Source Applications Foundation
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

import javax.activation.MimeTypeParseException;

import org.apache.abdera.i18n.iri.IRISyntaxException;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Generator;
import org.apache.abdera.model.Link;
import org.apache.abdera.model.Person;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osaf.cosmo.CosmoConstants;
import org.osaf.cosmo.model.User;
import org.osaf.cosmo.server.ServiceLocator;

/**
 * A base class for feed generators.
 *
 * @see Feed
 * @see Entry
 */
public abstract class BaseFeedGenerator {
    private static final Log log = LogFactory.getLog(BaseFeedGenerator.class);

    private StandardGeneratorFactory factory;
    private ServiceLocator locator;

    /** */
    public BaseFeedGenerator(StandardGeneratorFactory factory,
                             ServiceLocator locator) {
        this.factory = factory;
        this.locator = locator;
    }

    /**
     * Creates a <code>Feed</code> with id set to an IRI based on the
     * given uuid.
     *
     * @param uuid the collection uuid to use for the entry id
     * @throws GeneratorException
     */
    protected Feed newFeed(String uuid)
        throws GeneratorException {
        Feed feed = factory.getAbdera().getFactory().newFeed();

        String id = uuid2Iri(uuid);
        try {
            feed.setId(id);
        } catch (IRISyntaxException e) {
            throw new GeneratorException("Attempted to set invalid feed id " + id, e);
        }

        String baseUri = locator.getAtomBase();
        try {
            feed.setBaseUri(baseUri);
        } catch (IRISyntaxException e) {
            throw new GeneratorException("Attempted to set invalid base URI " + baseUri, e);
        }

        return feed;
    }

    /**
     * Creates a <code>Generator</code> specifying Cosmo product
     * information.
     *
     * @throws GeneratorException
     */
    protected Generator newGenerator()
        throws GeneratorException {
        Generator generator = factory.getAbdera().getFactory().newGenerator();
        try {
            generator.setUri(CosmoConstants.PRODUCT_URL);
            generator.setVersion(CosmoConstants.PRODUCT_VERSION);
            generator.setText(CosmoConstants.PRODUCT_NAME);
        } catch (IRISyntaxException e) {
            throw new GeneratorException("Attempted to set invalid generator URI " + CosmoConstants.PRODUCT_URL, e);
        }
        return generator;
    }

    /**
     * Creates a <code>Person</code> based on the given user.
     *
     * @param user the user
     * @throws GeneratorException
     */
    protected Person newPerson(User user)
        throws GeneratorException {
        Person author = factory.getAbdera().getFactory().newAuthor();

        author.setName(user.getUsername());
        // author.setEmail(user.getEmail());

        String uri = personIri(user);
        try {
            author.setUri(uri);
        } catch (IRISyntaxException e) {
            throw new GeneratorException("Attempted to set invalid person uri " + uri, e);
        }

        return author;
    }

    /**
     * Creates a <code>Entry</code> with id set to an IRI based on the
     * given item uuid.
     *
     * @param uuid the item uuid to use for the entry id
     * @param isDocument whether or not the entry represents an entire
     * document or is attached to a feed document
     * @throws GeneratorException
     */
    protected Entry newEntry(String uuid,
                             boolean isDocument)
        throws GeneratorException {
        Entry entry = factory.getAbdera().getFactory().newEntry();

        String id = uuid2Iri(uuid);
        try {
            entry.setId(id);
        } catch (IRISyntaxException e) {
            throw new GeneratorException("Attempted to set invalid entry id " + id, e);
        }

        if (isDocument) {
            String baseUri = locator.getAtomBase();
            try {
                entry.setBaseUri(baseUri);
            } catch (IRISyntaxException e) {
                throw new GeneratorException("Attempted to set invalid base URI " + baseUri, e);
            }
        }

        return entry;
    }

    /**
     * Creates a <code>Link</code> using the given parameters.
     *
     * @param rel the relation between the linked resource and the
     * linking resource
     * @param mimeType the mime type of the linked resource
     * @param href the href of the linked content
     * @throws GeneratorException
     */
    protected Link newLink(String rel,
                           String mimeType,
                           String href)
        throws GeneratorException {
        try {
            Link link = factory.getAbdera().getFactory().newLink();
            link.setRel(rel);
            link.setMimeType(mimeType);
            link.setHref(href);
            return link;
        } catch (MimeTypeParseException e) {
            throw new GeneratorException("Attempted to set invalid link mime type " + mimeType, e);
        } catch (IRISyntaxException e) {
            throw new GeneratorException("Attempted to set invalid link href " + href, e);
        }
    }

    /**
     * Returns the IRI of the given user. Requesting this IRI returns
     * a service document describing the user.
     *
     * @param user the user
     */
    protected String personIri(User user) {
        return locator.getAtomUrl(user, false);
    }

    /**
     * Returns an IRI incorporating the given uuid.
     *
     * @param uuid the uuid
     */
    protected String uuid2Iri(String uuid) {
        return "urn:uuid:" + uuid;
    }

    public StandardGeneratorFactory getFactory() {
        return factory;
    }

    public ServiceLocator getLocator() {
        return locator;
    }
}
