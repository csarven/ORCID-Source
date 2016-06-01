/**
 * =============================================================================
 *
 * ORCID (R) Open Source
 * http://orcid.org
 *
 * Copyright (c) 2012-2014 ORCID, Inc.
 * Licensed under an MIT-Style License (MIT)
 * http://orcid.org/open-source-license
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 *
 * =============================================================================
 */
package org.orcid.core.manager;

import java.util.Set;

import org.orcid.persistence.jpa.entities.OrcidClientDetailsEntity;

public interface OrcidSSOManager {

    OrcidClientDetailsEntity grantSSOAccess(String orcid, String name, String description, String website, Set<String> redirectUris);
    OrcidClientDetailsEntity getUserCredentials(String orcid);
    OrcidClientDetailsEntity updateUserCredentials(String orcid, String name, String description, String website, Set<String> redirectUris);
    void revokeSSOAccess(String orcid);   
    boolean resetClientSecret(String clientDetailsId);    
}
