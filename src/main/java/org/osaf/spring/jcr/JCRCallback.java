package org.osaf.spring.jcr;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author Brian Moseley
 */
public interface JCRCallback {

    /**
     * Called by {@link JCRTemplate#execute} within an active JCR
     * {@link javax.jcr.JCRSession}. Is not responsible for logging
     * out of the <code>Session</code> or handling transactions.
     *
     * Allows for returning a result object created within the
     * callback, i.e. a domain object or a collection of domain
     * objects. A thrown {@link RuntimeException} is treated as an
     * application exeception; it is propagated to the caller of the
     * template.
     */
    public Object doInJCR(Session session) throws RepositoryException;
}
