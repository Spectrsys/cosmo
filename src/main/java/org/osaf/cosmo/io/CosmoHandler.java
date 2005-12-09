/*
 * Copyright 2005 Open Source Applications Foundation
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
package org.osaf.cosmo.io;

import java.io.IOException;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.jackrabbit.server.io.DefaultHandler;
import org.apache.jackrabbit.server.io.ExportContext;
import org.apache.jackrabbit.server.io.ImportContext;
import org.apache.jackrabbit.server.io.IOManager;
import org.apache.jackrabbit.webdav.DavResource;

import org.osaf.cosmo.UnsupportedFeatureException;
import org.osaf.cosmo.icalendar.DuplicateUidException;
import org.osaf.cosmo.jcr.CosmoJcrConstants;
import org.osaf.cosmo.jcr.JCREscapist;

/**
 * Extends {@link org.apache.jackrabbit.server.io.DefaultHandler}
 * to provide custom logic for importing and exporting Cosmo
 * resources.
 */
public class CosmoHandler extends DefaultHandler {
    private static final Log log = LogFactory.getLog(CosmoHandler.class);

    /**
     */
    public CosmoHandler(IOManager ioManager) {
        super(ioManager,
              CosmoJcrConstants.NT_DAV_COLLECTION,
              CosmoJcrConstants.NT_DAV_RESOURCE,
              CosmoJcrConstants.NT_RESOURCE);
    }

    /**
     */
    public CosmoHandler(IOManager ioManager,
                        String collectionNodetype,
                        String defaultNodetype,
                        String contentNodetype) {
        super(ioManager, collectionNodetype, defaultNodetype, contentNodetype);
    }

    /**
     * Extends the superclass method with the following logic:
     *
     * <ol>
     * <li> Adds the <code>mix:ticketable</code> mixin type to the
     * resource node if it does not already have that type.</li>
     * <li> If importing a calendar resource into a caldav collection:
     * <ol>
     * <li> Ensures that the calendar object contains at least one
     * event.</li>
     * <li> Ensures that uid of the calendar object is unique within
     * the calendar collection.</li>
     * <li> Adds the <code>caldav:resource</code> mixin type to the
     * resource node if it does not already have that type.</li>
     * </ol>
     * </ol>
     */
    public boolean importData(ImportContext context,
                              boolean isCollection,
                              Node contentNode)
        throws IOException, RepositoryException {
        if (! super.importData(context, isCollection, contentNode)) {
            return false;
        }

        CosmoImportContext cosmoContext = (CosmoImportContext) context;
        Node resourceNode = contentNode;
        if (! isCollection) {
            resourceNode = contentNode.getParent();
        }

        if (resourceNode.isNodeType(CosmoJcrConstants.NT_DAV_RESOURCE) ||
            resourceNode.isNodeType(CosmoJcrConstants.NT_DAV_COLLECTION)) {
            // add ticketable mixin type for all dav collections and
            // resources
            if (! resourceNode.isNodeType(CosmoJcrConstants.NT_TICKETABLE)) {
                resourceNode.addMixin(CosmoJcrConstants.NT_TICKETABLE);
            }
        }

        if (cosmoContext.isCalendarContent() &&
            inCaldavCollection(resourceNode)) {
            Calendar calendar = cosmoContext.getCalendar();

            // since we are importing a calendar resource into a
            // calendar collection, we have to:

            // 1) make sure that the calendar object contains at least
            // one event
            if (calendar.getComponents().getComponents(Component.VEVENT).
                isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Calendar contains no events");
                }
                throw new UnsupportedFeatureException("No events found");
            }

            // 2) make sure that the calendar object's uid is
            // unique with in the calendar collection
            Component event = (Component)
                calendar.getComponents().getComponents(Component.VEVENT).
                get(0);
            Property uid = (Property)
                event.getProperties().getProperty(Property.UID);
            if (! isUidUnique(resourceNode, uid.getValue())) {
                throw new DuplicateUidException(uid.getValue());
            }

            // 3) add caldav resource mixin type
            if (! resourceNode.
                isNodeType(CosmoJcrConstants.NT_CALDAV_RESOURCE)) {
                resourceNode.addMixin(CosmoJcrConstants.NT_CALDAV_RESOURCE);
            }
        } else if (cosmoContext.isCalendarCollection()) {
            // add caldav collection mixin type
            if (! resourceNode.
                isNodeType(CosmoJcrConstants.NT_CALDAV_COLLECTION)) {
                resourceNode.
                    addMixin(CosmoJcrConstants.NT_CALDAV_COLLECTION);
            }
        }

        return true;
    }

    /**
     * Extends the superclass method with the following logic:
     *
     * <ol>
     * <li> The (JCR-escaped) system id is used to set the resource
     * node's <code>dav:displayname</code> property.</li>
     * <li> If importing any type of <code>dav:resource</code>, set
     * the resource node's <code>dav:contentlanguage</code>
     * property.</li>
     * <li> If importing a calendar resource into a caldav collection,
     * set the resource node's <code>caldav:uid</code> property.</li>
     * <li> If importing a caldav collection, set the
     * <code>caldav:calendar-description</code> and
     * <code>xml:lang</code> properties on the resource node.</li>
     *</ol>
     */
    protected boolean importProperties(ImportContext context,
                                       boolean isCollection,
                                       Node contentNode) {
        if (! super.importProperties(context, isCollection, contentNode)) {
            return false;
        }

        CosmoImportContext cosmoContext = (CosmoImportContext) context;
        Node resourceNode = contentNode;
        String displayName = 
            JCREscapist.hexUnescapeJCRNames(context.getSystemId());

        try {
            if (! isCollection) {
                resourceNode = contentNode.getParent();
            }

            if (resourceNode.isNodeType(CosmoJcrConstants.NT_DAV_RESOURCE) ||
                resourceNode.isNodeType(CosmoJcrConstants.NT_DAV_COLLECTION)) {
                // set display name on all dav collections and resources
                resourceNode.setProperty(CosmoJcrConstants.NP_DAV_DISPLAYNAME,
                                        displayName);
            }

            if (resourceNode.isNodeType(CosmoJcrConstants.NT_DAV_RESOURCE)) {
                // set content language on all dav resources
                resourceNode.
                    setProperty(CosmoJcrConstants.NP_DAV_CONTENTLANGUAGE,
                                context.getContentLanguage());
            }

            if (resourceNode.isNodeType(CosmoJcrConstants.NT_CALDAV_RESOURCE)) {
                // set the uid property on caldav resources
                Calendar calendar = cosmoContext.getCalendar();
                Component event = (Component)
                    calendar.getComponents().getComponents(Component.VEVENT).
                    get(0);
                Property uid = (Property)
                    event.getProperties().getProperty(Property.UID);
                resourceNode.setProperty(CosmoJcrConstants.NP_CALDAV_UID,
                                         uid.getValue());
            }

            if (resourceNode.
                isNodeType(CosmoJcrConstants.NT_CALDAV_COLLECTION)) {
                // set caldav:calendar-description property on calendar
                // collections
                resourceNode.setProperty(CosmoJcrConstants.
                                         NP_CALDAV_CALENDARDESCRIPTION,
                                         displayName);

                // set xml:lang property on calendar collections
                resourceNode.setProperty(CosmoJcrConstants.NP_XML_LANG,
                                         Locale.getDefault().toString());
            }
        } catch (IOException e) {
            // XXX ugh swallowing
            log.error("error reading calendar stream", e);
            return false;
        } catch (RepositoryException e) {
            // XXX ugh swallowing
            log.error("error importing dav properties", e);
            return false;
        }

        return true;
    }

    /**
     * Returns true if the content is being imported into a CalDAV
     * collection. This implementation returns true if the resource
     * node's parent node is a <code>caldav:collection</code>.
     */
    protected boolean inCaldavCollection(Node resourceNode)
        throws RepositoryException {
        return resourceNode.getParent().
            isNodeType(CosmoJcrConstants.NT_CALDAV_COLLECTION);
    }

    /**
     * Returns true if the given uid value is not already in use by
     * any calendar resource node within the parent calendar
     * collection node of the given resource node.
     */
    protected boolean isUidUnique(Node resourceNode, String uid)
        throws RepositoryException {
        // look for nodes anywhere below the parent calendar
        // collection that have this same uid 
        StringBuffer stmt = new StringBuffer();
        stmt.append("/jcr:root");
        if (! resourceNode.getParent().getPath().equals("/")) {
            stmt.append(JCREscapist.xmlEscapeJCRPath(resourceNode.getParent().
                                                     getPath()));
        }
        stmt.append("//element(*, ").
            append(CosmoJcrConstants.NT_CALDAV_RESOURCE).
            append(")");
        stmt.append("[@").
            append(CosmoJcrConstants.NP_CALDAV_UID).
            append(" = '").
            append(uid).
            append("']");

        QueryManager qm =
            resourceNode.getSession().getWorkspace().getQueryManager();
        QueryResult qr =
            qm.createQuery(stmt.toString(), Query.XPATH).execute();

        // if we are updating this node, then we expect it to show up
        // in the result, but nothing else
        for (NodeIterator i=qr.getNodes(); i.hasNext();) {
            Node n = (Node) i.next();
            if (! n.getPath().equals(resourceNode.getPath())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Extends the superclass method with the following logic:
     *
     * <ol>
     * <li> If the resource node is a <code>dav:resource</code>, its
     * <code>dav:contentlanguage</code> property is used to set the
     * export context's content language.
     *</ol>
     */
    protected void exportProperties(ExportContext context,
                                    boolean isCollection,
                                    Node contentNode)
        throws IOException {
        super.exportProperties(context, isCollection, contentNode);

        try {
            Node resourceNode = isCollection ?
                contentNode : contentNode.getParent();
            // get content language
            if (resourceNode.
                hasProperty(CosmoJcrConstants.NP_DAV_CONTENTLANGUAGE)) {
                String contentLanguage = resourceNode.
                    getProperty(CosmoJcrConstants.NP_DAV_CONTENTLANGUAGE).
                    getString();
                context.setContentLanguage(contentLanguage);
            }
        } catch (RepositoryException e) {
            log.error("error exporting dav properties", e);
            throw new IOException(e.getMessage());
        }
    }
}
