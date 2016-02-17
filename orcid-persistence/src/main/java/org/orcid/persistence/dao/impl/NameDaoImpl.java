package org.orcid.persistence.dao.impl;

import org.orcid.persistence.dao.NameDao;
import org.orcid.persistence.jpa.entities.NameEntity;

public class NameDaoImpl extends GenericDaoImpl<NameEntity, Long> implements NameDao {

    public NameDaoImpl() {
        super(NameEntity.class);
    }

    @Override
    public NameEntity getName(String orcid) {
        // TODO Auto-generated method stub
        return null;
    }
}
