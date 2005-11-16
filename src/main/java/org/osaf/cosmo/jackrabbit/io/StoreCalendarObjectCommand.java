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
package org.osaf.cosmo.jackrabbit.io;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;

import org.apache.jackrabbit.server.io.AbstractCommand;
import org.apache.jackrabbit.server.io.AbstractContext;
import org.apache.jackrabbit.server.io.ImportContext;
import org.apache.jackrabbit.webdav.DavException;

import org.apache.log4j.Logger;

import org.osaf.cosmo.UnsupportedFeatureException;
import org.osaf.cosmo.dav.CosmoDavResponse;
import org.osaf.cosmo.icalendar.CosmoICalendarConstants;
import org.osaf.cosmo.icalendar.DuplicateUidException;
import org.osaf.cosmo.jcr.CosmoJcrConstants;
import org.osaf.cosmo.jcr.JCREscapist;

/**
 * An import command for storing the calendar object attached to a
 * dav resource.
 */
public class StoreCalendarObjectCommand extends AbstractCommand {
    private static final Logger log =
        Logger.getLogger(StoreCalendarObjectCommand.class);

    /**
     */
    public boolean execute(AbstractContext context)
        throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        }
        else {
            return false;
        }
    }

    /**
     */
    public boolean execute(ImportContext context)
        throws Exception {
        Node resourceNode = context.getNode();
        if (resourceNode == null) {
            return false;
        }

        // if the node's parent is not a calendar collection, don't
        // bother storing, since we'll never query "webcal"
        // calendars.
        if (! resourceNode.getParent().
            isNodeType(CosmoJcrConstants.NT_CALDAV_COLLECTION)) {
            return false;
        }

        // ensure that the resource is a dav resource and that either
        // it is of type text/calendar or its name ends with .ics
        if (! (resourceNode.
               isNodeType(CosmoJcrConstants.NT_DAV_RESOURCE) &&
               (context.getContentType().
                startsWith(CosmoICalendarConstants.CONTENT_TYPE) ||
                resourceNode.getName().
                endsWith("." + CosmoICalendarConstants.FILE_EXTENSION)))) {
            return false;
        }

        // get a handle to the resource content
        Node content = resourceNode.getNode(CosmoJcrConstants.NN_JCR_CONTENT);
        InputStream in =
            content.getProperty(CosmoJcrConstants.NP_JCR_DATA).getStream();

        Calendar calendar = null;
        try {
            // parse the resource
            CalendarBuilder builder = new CalendarBuilder();
            calendar = builder.build(in);
        } catch (ParserException e) {
            if (log.isDebugEnabled()) {
                log.debug("Error parsing calendar resource", e);
            }
            throw new DavException(CosmoDavResponse.SC_FORBIDDEN);
        }

        // make sure the object contains an event
        if (calendar.getComponents().getComponents(Component.VEVENT).
            isEmpty()) {
            throw new UnsupportedFeatureException("No events found");
        }

        // make the node a caldav resource if it isn't already
        if (! resourceNode.isNodeType(CosmoJcrConstants.NT_CALDAV_RESOURCE)) {
            resourceNode.addMixin(CosmoJcrConstants.NT_CALDAV_RESOURCE);
        }

        // it's possible (tho pathological) that the client will
        // change the resource's uid on an update, so always
        // verify and set it
        Component event = (Component)
            calendar.getComponents().getComponents(Component.VEVENT).get(0);
        Property uid = (Property)
            event.getProperties().getProperty(Property.UID);
        if (! isUidUnique(resourceNode, uid.getValue())) {
            throw new DuplicateUidException(uid.getValue());
        }
        resourceNode.setProperty(CosmoJcrConstants.NP_CALDAV_UID,
                                 uid.getValue());

        return false;
    }

    /**
     */
    protected boolean isUidUnique(Node node, String uid)
        throws RepositoryException {
        // look for nodes anywhere below the parent calendar
        // collection that have this same uid 
        StringBuffer stmt = new StringBuffer();
        stmt.append("/jcr:root");
        if (! node.getParent().getPath().equals("/")) {
            stmt.append(JCREscapist.xmlEscapeJCRPath(node.getParent().
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
            node.getSession().getWorkspace().getQueryManager();
        QueryResult qr =
            qm.createQuery(stmt.toString(), Query.XPATH).execute();

        // if we are updating this node, then we expect it to show up
        // in the result, but nothing else
        for (NodeIterator i=qr.getNodes(); i.hasNext();) {
            Node n = (Node) i.next();
            if (! n.getPath().equals(node.getPath())) {
                return false;
            }
        }

        return true;
    }
}
