package org.osaf.cosmo.ui;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * An {@link OSAFAction} that logs the current user out of the
 * application.
 */
public class LogoutAction extends CosmoAction {
    private static final Log log = LogFactory.getLog(LogoutAction.class);

    /**
     * Logs the user out of the application by invalidating his
     * {@link javax.servlet.http.HttpSession} and then returns the
     * <code>OK</code> forward.
     *
     * @see UIConstants#FWD_OK
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        request.getSession().invalidate();

        return mapping.findForward(UIConstants.FWD_OK);
    }
}
