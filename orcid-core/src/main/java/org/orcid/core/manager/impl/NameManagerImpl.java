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
package org.orcid.core.manager.impl;

import javax.annotation.Resource;

import org.orcid.core.manager.NameManager;
import org.orcid.persistence.dao.NameDao;
import org.orcid.persistence.jpa.entities.NameEntity;

public class NameManagerImpl implements NameManager {

    @Resource
    private NameDao nameDao;

    @Override
    public NameEntity getName(String orcid) {
        return nameDao.getName(orcid);
    }

    @Override
    public boolean updateName(NameEntity nameEntity) {
        return nameDao.merge(nameEntity) != null ? true : false;
    }
}
