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
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.orcid.core.manager.WorkCacheManager;
import org.orcid.persistence.dao.ProfileDao;
import org.orcid.persistence.dao.WorkDao;
import org.orcid.persistence.jpa.entities.MinimizedWorkEntity;
import org.orcid.persistence.jpa.entities.WorkLastModifiedEntity;
import org.orcid.utils.ReleaseNameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

/**
 * 
 * @author Will Simpson
 *
 */
public class WorkCacheManagerImpl implements WorkCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkCacheManagerImpl.class);

    @Resource
    private WorkDao workDao;

    @Resource
    private ProfileDao profileDao;

    @Resource(name = "workLastModifiedCache")
    private Cache workLastModifiedCache;

    @Resource(name = "publicWorkLastModifiedCache")
    private Cache publicWorkLastModifiedCache;

    @Resource(name = "minimizedWorkCache")
    private Cache minimizedWorkCache;

    private String releaseName = ReleaseNameUtils.getReleaseName();

    private LockerObjectsManager lockers = new LockerObjectsManager();

    private LockerObjectsManager publicLockers = new LockerObjectsManager();

    @Override
    public List<WorkLastModifiedEntity> retrieveWorkLastModifiedList(String orcid, long profileLastModified) {
        Object key = new ProfileCacheKey(orcid, profileLastModified, releaseName);
        List<WorkLastModifiedEntity> workLastModifiedList = toWorkLastModifiedList(workLastModifiedCache.get(key));
        if (workLastModifiedList == null) {
            try {
                synchronized (lockers.obtainLock(orcid)) {
                    workLastModifiedList = toWorkLastModifiedList(workLastModifiedCache.get(key));
                    if (workLastModifiedList == null) {
                        workLastModifiedList = workDao.getWorkLastModifiedList(orcid);
                        workLastModifiedCache.put(new Element(key, workLastModifiedList));
                    }
                }
            } finally {
                lockers.releaseLock(orcid);
            }
        }
        return workLastModifiedList;
    }

    @Override
    public List<WorkLastModifiedEntity> retrievePublicWorkLastModifiedList(String orcid, long profileLastModified) {
        Object key = new ProfileCacheKey(orcid, profileLastModified, releaseName);
        List<WorkLastModifiedEntity> workLastModifiedList = toWorkLastModifiedList(publicWorkLastModifiedCache.get(key));
        if (workLastModifiedList == null) {
            try {
                synchronized (publicLockers.obtainLock(orcid)) {
                    workLastModifiedList = toWorkLastModifiedList(publicWorkLastModifiedCache.get(key));
                    if (workLastModifiedList == null) {
                        workLastModifiedList = workDao.getPublicWorkLastModifiedList(orcid);
                        publicWorkLastModifiedCache.put(new Element(key, workLastModifiedList));
                    }
                }
            } finally {
                publicLockers.releaseLock(orcid);
            }
        }
        return workLastModifiedList;
    }

    @Override
    public MinimizedWorkEntity retrieveMinimizedWork(long workId, long workLastModified) {
        Object key = new WorkCacheKey(workId, workLastModified, releaseName);
        MinimizedWorkEntity minimizedWorkEntity = toMinimizedWork(minimizedWorkCache.get(key));
        if (minimizedWorkEntity == null) {
            minimizedWorkEntity = workDao.getMinimizedWorkEntity(workId);
            minimizedWorkCache.put(new Element(key, minimizedWorkEntity));
        }
        return minimizedWorkEntity;
    }

    @Override
    public List<MinimizedWorkEntity> retrieveMinimizedWorks(String orcid, long profileLastModified) {
        List<WorkLastModifiedEntity> workLastModifiedList = retrieveWorkLastModifiedList(orcid, profileLastModified);
        int numWorks = workLastModifiedList.size();
        LOGGER.info("Got work last modified list, orcid = {}, size = {}", orcid, numWorks);
        if (numWorks > 1000) {
            LOGGER.info("Found hyper-author while retrieving minimized works, orcid= {}", orcid);
        }
        List<MinimizedWorkEntity> works = workLastModifiedList.stream().map(e -> retrieveMinimizedWork(e.getId(), e.getLastModified().getTime()))
                .collect(Collectors.toList());
        int cacheSize = minimizedWorkCache.getSize();
        int diskStoreSize = minimizedWorkCache.getDiskStoreSize();
        LOGGER.info("Retrieved minimized works for {}, cacheSize = {}, diskStoreSize = {}", new Object[] { orcid, cacheSize, diskStoreSize });
        return works;
    }

    @Override
    public List<MinimizedWorkEntity> retrievePublicMinimizedWorks(String orcid, long profileLastModified) {
        List<WorkLastModifiedEntity> workLastModifiedList = retrievePublicWorkLastModifiedList(orcid, profileLastModified);
        int numWorks = workLastModifiedList.size();
        LOGGER.info("Got public work last modified list, orcid = {}, size = {}", orcid, numWorks);
        if (numWorks > 1000) {
            LOGGER.info("Found hyper-author while retrieving public minimized works, orcid= {}", orcid);
        }
        List<MinimizedWorkEntity> works = workLastModifiedList.stream().map(e -> retrieveMinimizedWork(e.getId(), e.getLastModified().getTime()))
                .collect(Collectors.toList());
        int cacheSize = minimizedWorkCache.getSize();
        int diskStoreSize = minimizedWorkCache.getDiskStoreSize();
        LOGGER.info("Retrieved public minimized works for {}, cacheSize = {}, diskStoreSize = {}", new Object[] { orcid, cacheSize, diskStoreSize });
        return works;
    }

    private MinimizedWorkEntity toMinimizedWork(Element element) {
        return (MinimizedWorkEntity) (element != null ? element.getObjectValue() : null);
    }

    @SuppressWarnings("unchecked")
    private List<WorkLastModifiedEntity> toWorkLastModifiedList(Element element) {
        return (List<WorkLastModifiedEntity>) (element != null ? element.getObjectValue() : null);
    }

}
