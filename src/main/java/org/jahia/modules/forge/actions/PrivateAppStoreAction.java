/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms & Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.forge.actions;

import org.jahia.bin.LicensedAction;
import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.security.license.LicenseCheckerService;

/**
 * Base class for Pribvate App Store related rendering actions.
 * 
 * @author Sergiy Shyrkov
 */
public abstract class PrivateAppStoreAction extends LicensedAction {

    private static final String LICENSE_FEATURE = "org.jahia.privateAppStore";

    /**
     * Ensures that the Private App Store feature is allowed by current license terms. If not an exception is thrown.
     */
    public static void ensureLicense() {
        if (!LicenseCheckerService.Stub.isAllowed(LICENSE_FEATURE)) {
            throw new JahiaRuntimeException(new JahiaInitializationException(
                    "The Private App Store feature is not allowed by the current license terms"));
        }
    }

    /**
     * Initializes an instance of this class.
     */
    public PrivateAppStoreAction() {
        super();
        setLicenseFeature(LICENSE_FEATURE);
    }

}
