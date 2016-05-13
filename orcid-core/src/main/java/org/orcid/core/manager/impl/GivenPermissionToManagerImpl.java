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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.orcid.core.manager.GivenPermissionToManager;
import org.orcid.core.manager.NotificationManager;
import org.orcid.core.manager.ProfileEntityCacheManager;
import org.orcid.jaxb.model.message.ApprovalDate;
import org.orcid.jaxb.model.message.CreditName;
import org.orcid.jaxb.model.message.DelegateSummary;
import org.orcid.jaxb.model.message.DelegationDetails;
import org.orcid.jaxb.model.message.OrcidIdentifier;
import org.orcid.jaxb.model.message.OrcidProfile;
import org.orcid.persistence.dao.GivenPermissionToDao;
import org.orcid.persistence.dao.ProfileDao;
import org.orcid.persistence.jpa.entities.GivenPermissionToEntity;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.orcid.persistence.jpa.entities.ProfileSummaryEntity;
import org.orcid.utils.DateUtils;

public class GivenPermissionToManagerImpl implements GivenPermissionToManager{

    @Resource
    private ProfileDao profileDao;

    @Resource
    private GivenPermissionToDao givenPermissionToDao;
    
    @Resource(name = "profileEntityCacheManager")
    private ProfileEntityCacheManager profileEntityCacheManager;
    
    @Resource
    private NotificationManager notificationManager;
    
    public void setNotificationManager(NotificationManager notificationManager){
        this.notificationManager=notificationManager;
    }
    
    @Override
    public void grantPermission(String managedOrcid, String trustedOrcid, OrcidProfile currentUser) {
        GivenPermissionToEntity existing = givenPermissionToDao.findByGiverAndReceiverOrcid(managedOrcid, trustedOrcid);
        if (existing == null) {
            // Clear the delegate's profile from the cache so that
            // the granting
            // user is visible to them immediately
            Date delegateLastModified = profileDao.updateLastModifiedDate(trustedOrcid);
            GivenPermissionToEntity permission = new GivenPermissionToEntity();
            permission.setGiver(managedOrcid);
            ProfileSummaryEntity receiver = new ProfileSummaryEntity(trustedOrcid);
            receiver.setLastModified(delegateLastModified);
            permission.setReceiver(receiver);
            permission.setApprovalDate(new Date());
            givenPermissionToDao.merge(permission);
            ProfileEntity delegateProfile = profileEntityCacheManager.retrieve(trustedOrcid);
            DelegationDetails details = new DelegationDetails();
            details.setApprovalDate(new ApprovalDate(DateUtils.convertToXMLGregorianCalendar(permission.getApprovalDate())));
            DelegateSummary summary = new DelegateSummary();
            details.setDelegateSummary(summary);
            summary.setOrcidIdentifier(new OrcidIdentifier(trustedOrcid));
            String creditName = null;
            if(delegateProfile.getRecordNameEntity() != null) {
                creditName = delegateProfile.getRecordNameEntity().getCreditName();
            }
            if (StringUtils.isNotBlank(creditName)) {
                summary.setCreditName(new CreditName(creditName));
            }
            List<DelegationDetails> detailsList = new ArrayList<>(1);
            detailsList.add(details);
            // Send notifications
            notificationManager.sendNotificationToAddedDelegate(currentUser, detailsList);
        }
    }

}
