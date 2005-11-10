/*
 * Copyright 2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.osaf.cosmo.dav.report;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.jackrabbit.webdav.property.AbstractDavProperty;
import org.apache.jackrabbit.webdav.version.DeltaVConstants;
import org.jdom.Element;

/**
 * This class is copied pretty much verbatim from
 * org.apache.jackrabit.webdav.version.SupportedReportSetProperty.
 */

/**
 * <code>SupportedReportSetProperty</code> represents the
 * DAV:supported-report-set property defined by RFC 3253. It identifies the
 * reports that are supported by the given resource.
 * 
 * <pre>
 *   &lt;!ELEMENT supported-report-set (supported-report*)&gt;
 *   &lt;!ELEMENT supported-report report&gt;
 *   &lt;!ELEMENT report ANY&gt;
 *   ANY value: a report element type
 * </pre>
 */
public class SupportedReportSetProperty extends AbstractDavProperty {

    private final HashSet reportTypes = new HashSet();

    /**
     * Create a new empty <code>SupportedReportSetProperty</code>.
     */
    public SupportedReportSetProperty() {
        super(DeltaVConstants.SUPPORTED_REPORT_SET, true);
    }

    /**
     * Create a new <code>SupportedReportSetProperty</code> property.
     * 
     * @param reportTypes
     *            that are supported by the resource having this property.
     */
    public SupportedReportSetProperty(ReportType[] reportTypes) {
        super(DeltaVConstants.SUPPORTED_REPORT_SET, true);
        for (int i = 0; i < reportTypes.length; i++) {
            addReportType(reportTypes[i]);
        }
    }

    /**
     * Add an additional report type to this property's value.
     * 
     * @param reportType
     */
    public void addReportType(ReportType reportType) {
        reportTypes.add(reportType);
    }

    /**
     * Returns true if the report type indicated in the specified
     * <code>RequestInfo</code> object is included in the supported reports.
     * 
     * @param reqInfo
     * @return true if the requested report is supported.
     */
    public boolean isSupportedReport(ReportInfo reqInfo) {
        Iterator it = reportTypes.iterator();
        while (it.hasNext()) {
            ReportType rt = (ReportType) it.next();
            if (rt.isRequestedReportType(reqInfo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the Xml representation of this property.
     * 
     * @return Xml representation listing all supported reports
     * @see org.apache.jackrabbit.webdav.property.DavProperty#toXml()
     */
    public Element toXml() {
        Element elem = getName().toXml();
        Iterator it = reportTypes.iterator();
        while (it.hasNext()) {
            Element sr = new Element(DeltaVConstants.XML_SUPPORTED_REPORT,
                    DeltaVConstants.NAMESPACE);
            Element r = new Element(DeltaVConstants.XML_REPORT,
                    DeltaVConstants.NAMESPACE);
            elem.addContent(sr.addContent(r.addContent(((ReportType) it.next())
                    .toXml())));
        }
        return elem;
    }

    /**
     * Returns a set of report types.
     * 
     * @return set of {@link ReportType}.
     * @see org.apache.jackrabbit.webdav.property.DavProperty#getValue()
     */
    public Object getValue() {
        return reportTypes;
    }
}