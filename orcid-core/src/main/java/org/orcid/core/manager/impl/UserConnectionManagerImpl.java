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

import java.util.List;

import javax.annotation.Resource;

import org.orcid.core.manager.UserConnectionManager;
import org.orcid.persistence.dao.UserConnectionDao;
import org.orcid.persistence.jpa.entities.UserconnectionEntity;
import org.orcid.persistence.jpa.entities.UserconnectionPK;

public class UserConnectionManagerImpl implements UserConnectionManager{

    @Resource
    private UserConnectionDao userConnectionDao;
    
    @Override
    public List<UserconnectionEntity> findByOrcid(String orcid) {
        return userConnectionDao.findByOrcid(orcid);
    }

    @Override
    public void remove(UserconnectionPK id) {
        userConnectionDao.remove(id);
    }

}
