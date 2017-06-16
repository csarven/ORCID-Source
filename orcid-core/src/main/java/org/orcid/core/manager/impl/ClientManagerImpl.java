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
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import org.orcid.core.adapter.JpaJaxbClientAdapter;
import org.orcid.core.manager.AppIdGenerationManager;
import org.orcid.core.manager.ClientManager;
import org.orcid.core.manager.EncryptionManager;
import org.orcid.core.manager.ProfileEntityCacheManager;
import org.orcid.core.manager.SourceManager;
import org.orcid.core.manager.read_only.ClientManagerReadOnly;
import org.orcid.jaxb.model.client_v2.Client;
import org.orcid.jaxb.model.clientgroup.ClientType;
import org.orcid.jaxb.model.clientgroup.MemberType;
import org.orcid.persistence.dao.ClientDetailsDao;
import org.orcid.persistence.dao.ClientRedirectDao;
import org.orcid.persistence.dao.ClientSecretDao;
import org.orcid.persistence.jpa.entities.ClientAuthorisedGrantTypeEntity;
import org.orcid.persistence.jpa.entities.ClientDetailsEntity;
import org.orcid.persistence.jpa.entities.ClientGrantedAuthorityEntity;
import org.orcid.persistence.jpa.entities.ClientResourceIdEntity;
import org.orcid.persistence.jpa.entities.ClientScopeEntity;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class ClientManagerImpl implements ClientManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientManagerImpl.class);

    @Resource
    private JpaJaxbClientAdapter jpaJaxbClientAdapter;

    @Resource
    private ClientDetailsDao clientDetailsDao;

    @Resource
    private ClientSecretDao clientSecretDao;

    @Resource
    private ClientRedirectDao clientRedirectDao;

    @Resource
    private EncryptionManager encryptionManager;

    @Resource
    private AppIdGenerationManager appIdGenerationManager;

    @Resource
    private SourceManager sourceManager;

    @Resource
    private ProfileEntityCacheManager profileEntityCacheManager;

    @Resource
    private TransactionTemplate transactionTemplate;
    
    @Resource
    private ClientManagerReadOnly clientManagerReadOnly;

    @Override
    public Client create(Client newClient) throws IllegalArgumentException {
        String memberId = sourceManager.retrieveSourceOrcid();
        ProfileEntity memberEntity = profileEntityCacheManager.retrieve(memberId);

        //Verify if the member type allow him to create another client
        validateCreateClientRequest(memberId);
        
        ClientDetailsEntity newEntity = jpaJaxbClientAdapter.toEntity(newClient);
        Date now = new Date();
        newEntity.setDateCreated(now);
        newEntity.setLastModified(now);
        newEntity.setId(appIdGenerationManager.createNewAppId());
        newEntity.setClientSecretForJpa(encryptionManager.encryptForInternalUse(UUID.randomUUID().toString()), true);
        newEntity.setGroupProfileId(memberId);

        // Set persistent tokens enabled by default
        newEntity.setPersistentTokensEnabled(true);

        // Set ClientType
        newEntity.setClientType(getClientType(memberEntity.getGroupType()));

        // Set ClientResourceIdEntity
        Set<ClientResourceIdEntity> clientResourceIdEntities = new HashSet<ClientResourceIdEntity>();
        ClientResourceIdEntity clientResourceIdEntity = new ClientResourceIdEntity();
        clientResourceIdEntity.setClientDetailsEntity(newEntity);
        clientResourceIdEntity.setResourceId("orcid");
        clientResourceIdEntities.add(clientResourceIdEntity);
        newEntity.setClientResourceIds(clientResourceIdEntities);

        // Set ClientAuthorisedGrantTypeEntity
        Set<ClientAuthorisedGrantTypeEntity> clientAuthorisedGrantTypeEntities = new HashSet<ClientAuthorisedGrantTypeEntity>();
        for (String clientAuthorisedGrantType : Arrays.asList("client_credentials", "authorization_code", "refresh_token")) {
            ClientAuthorisedGrantTypeEntity grantTypeEntity = new ClientAuthorisedGrantTypeEntity();
            grantTypeEntity.setClientDetailsEntity(newEntity);
            grantTypeEntity.setGrantType(clientAuthorisedGrantType);
            clientAuthorisedGrantTypeEntities.add(grantTypeEntity);
        }
        newEntity.setClientAuthorizedGrantTypes(clientAuthorisedGrantTypeEntities);

        // Set ClientGrantedAuthorityEntity
        List<ClientGrantedAuthorityEntity> clientGrantedAuthorityEntities = new ArrayList<ClientGrantedAuthorityEntity>();
        ClientGrantedAuthorityEntity clientGrantedAuthorityEntity = new ClientGrantedAuthorityEntity();
        clientGrantedAuthorityEntity.setClientDetailsEntity(newEntity);
        clientGrantedAuthorityEntity.setAuthority("ROLE_CLIENT");
        clientGrantedAuthorityEntities.add(clientGrantedAuthorityEntity);
        newEntity.setClientGrantedAuthorities(clientGrantedAuthorityEntities);

        // Set ClientScopeEntity
        Set<ClientScopeEntity> clientScopeEntities = new HashSet<ClientScopeEntity>();
        for (String clientScope : ClientType.getScopes(newEntity.getClientType())) {
            ClientScopeEntity clientScopeEntity = new ClientScopeEntity();
            clientScopeEntity.setClientDetailsEntity(newEntity);
            clientScopeEntity.setScopeType(clientScope);
            clientScopeEntities.add(clientScopeEntity);
        }
        newEntity.setClientScopes(clientScopeEntities);

        try {
            clientDetailsDao.persist(newEntity);
        } catch (Exception e) {
            LOGGER.error("Unable to client client with id {}", newEntity.getId(), e);
            throw e;
        }

        return jpaJaxbClientAdapter.toClient(newEntity);
    }

    @Override
    public Client edit(Client existingClient) {
        ClientDetailsEntity clientDetails = clientDetailsDao.find(existingClient.getId());
        jpaJaxbClientAdapter.toEntity(existingClient, clientDetails);
        clientDetails.setLastModified(new Date());
        clientDetails = clientDetailsDao.merge(clientDetails);
        return jpaJaxbClientAdapter.toClient(clientDetails);
    }

    @Override
    public Boolean resetClientSecret(String clientId) {
        return transactionTemplate.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                try {
                    // Generate new secret
                    String clientSecret = encryptionManager.encryptForInternalUse(UUID.randomUUID().toString());
                    // Set all existing secrets as non primary
                    clientSecretDao.revokeAllKeys(clientId);
                    // #2 Create the new client secret as primary
                    boolean result = clientSecretDao.createClientSecret(clientId, clientSecret);
                    // #3 if it was created, update the last modified for the
                    // client
                    // details
                    if (result)
                        clientDetailsDao.updateLastModified(clientId);
                    return result;
                } catch (Exception e) {
                    LOGGER.error("Unable to reset client secret for client {}", clientId, e);
                    throw e;
                }
            }
        });
    }
    
    private void validateCreateClientRequest(String memberId) throws IllegalArgumentException {
        ProfileEntity member = profileEntityCacheManager.retrieve(memberId);
        if(member == null || member.getGroupType() == null) {
            throw new IllegalArgumentException("Illegal member id: " + memberId);            
        }
        switch (member.getGroupType()){
        case BASIC:
        case BASIC_INSTITUTION:
            Set<Client> clients = clientManagerReadOnly.getClients(memberId);
            if(clients != null && !clients.isEmpty()) {
                throw new IllegalArgumentException("Your membership doesn't allow you to have more clients");
            }
            break;
        case PREMIUM:
        case PREMIUM_INSTITUTION:
            break;
        }                        
    }
    
    private ClientType getClientType(MemberType memberType) {
        switch (memberType) {
        case BASIC:
            return ClientType.UPDATER;
        case BASIC_INSTITUTION:
            return ClientType.CREATOR;
        case PREMIUM:
            return ClientType.PREMIUM_UPDATER;
        case PREMIUM_INSTITUTION:
            return ClientType.PREMIUM_CREATOR;
        default:
            throw new IllegalArgumentException("Invalid member type: " + memberType);
        }
    }    
}
