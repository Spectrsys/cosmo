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
package org.osaf.cosmo.ui;

import org.osaf.commons.struts.OSAFErrorAction;
import org.osaf.commons.struts.OSAFStrutsConstants;

import java.net.ConnectException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.springframework.mail.MailSendException;
import org.springframework.orm.ObjectRetrievalFailureException;

/**
 * An OSAFErrorAction that chooses the appropriate error view based on
 * the discovered exception.
 */
public class CosmoErrorAction extends OSAFErrorAction {
    private static final Log log = LogFactory.getLog(CosmoErrorAction.class);

    public static final String ATTR_EXCEPTION = "Exception";
    /**
     * The Struts forward representing the "General Error" page:
     * <code>error.general</code>
     */
    public static final String FWD_ERROR_GENERAL = "error.general";
    /**
     * The Struts forward representing the "Messaging Error" page:
     * <code>error.messaging</code>
     */
    public static final String FWD_ERROR_MESSAGING = "error.messaging";
    /**
     * The Struts forward representing the "Resource Not Found" page:
     * <code>error.notfound</code>
     */
    public static final String FWD_ERROR_NOT_FOUND = "error.notfound";

    /**
     * Examines the discovered exception to choose the appropriate
     * error view:
     *
     * <ol>
     * <li> MailSendException: <code>FWD_ERROR_MESSAGING</code></li>
     * <li> ObjectRetrievalFailureException: <code>
     * FWD_ERROR_NOT_FOUND</code></li>
     * <li> all others: <code>FWD_ERROR_GENERAL</code></li>
     * </ol>
     */
    public ActionForward findForward(ActionMapping mapping,
                                     ActionForm form,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        Throwable t = (Throwable)
            request.getAttribute(OSAFStrutsConstants.ATTR_EXCEPTION);

        if (isMessagingError(t)) {
            return mapping.findForward(FWD_ERROR_MESSAGING);
        }
        else if (isNotFoundError(t)) {
            return mapping.findForward(FWD_ERROR_NOT_FOUND);
        }
        return mapping.findForward(FWD_ERROR_GENERAL);
    }

    private boolean isMessagingError(Throwable t) {
        return t instanceof MailSendException;
    }

    private boolean isNotFoundError(Throwable t) {
        return t instanceof ObjectRetrievalFailureException;
    }
}
