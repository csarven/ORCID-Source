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
package org.orcid.persistence.jpa.entities;

import java.io.Serializable;
import java.util.Comparator;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.orcid.persistence.jpa.entities.keys.NotificationWorkEntityPk;

@Entity
@Table(name = "notification_work")
@IdClass(NotificationWorkEntityPk.class)
public class NotificationWorkEntity extends BaseEntity<NotificationWorkEntityPk> implements Comparable<NotificationWorkEntity> {

    private static final long serialVersionUID = -3187757614938904392L;

    private NotificationEntity notification;
    private LegacyWorkEntity work;

    @Override
    @Transient
    public NotificationWorkEntityPk getId() {
        return null;
    }

    @Id
    @ManyToOne(cascade = { CascadeType.REFRESH }, fetch = FetchType.EAGER)
    @JoinColumn(name = "notification_id", nullable = false)
    public NotificationEntity getNotification() {
        return notification;
    }

    public void setNotification(NotificationEntity notification) {
        this.notification = notification;
    }

    /**
     * @return the work
     */
    @Id
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "work_id", nullable = false)
    public LegacyWorkEntity getWork() {
        return work;
    }

    /**
     * @param work
     *            the work to set
     */
    public void setWork(LegacyWorkEntity work) {
        this.work = work;
    }

    @Override
    public int compareTo(NotificationWorkEntity other) {
        if (other == null) {
            throw new NullPointerException("Can't compare with null");
        }
        if (other.getWork() == null) {
            if (work == null) {
                return 0;
            } else {
                return 1;
            }
        }
        if (work == null) {
            return -1;
        }
        return work.compareTo(other.getWork());
    }

    public static class ChronologicallyOrderedNotificationWorkEntityComparator implements Comparator<NotificationWorkEntity>, Serializable {

        private static final long serialVersionUID = 1L;

        public int compare(NotificationWorkEntity profileWork1, NotificationWorkEntity profileWork2) {
            if (profileWork2 == null) {
                throw new NullPointerException("Can't compare with null");
            }

            if (profileWork2.getWork() == null) {
                if (profileWork1.getWork() == null) {
                    return 0;
                } else {
                    return 1;
                }
            } else if (profileWork1.getWork() == null) {
                return -1;
            }

            LegacyWorkEntity work1 = profileWork1.getWork();
            LegacyWorkEntity work2 = profileWork2.getWork();

            LegacyWorkEntity.ChronologicallyOrderedWorkEntityComparator workEntityComparator = new LegacyWorkEntity.ChronologicallyOrderedWorkEntityComparator();

            return workEntityComparator.compare(work1, work2);
        }
    }

}
