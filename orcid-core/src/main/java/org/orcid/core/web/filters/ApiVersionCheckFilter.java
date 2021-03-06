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
package org.orcid.core.web.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.orcid.core.exception.OrcidBadRequestException;
import org.orcid.core.locale.LocaleManager;
import org.orcid.core.manager.impl.OrcidUrlManager;
import org.orcid.pojo.ajaxForm.PojoUtil;
import org.orcid.utils.OrcidStringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sun.jersey.api.core.InjectParam;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

@Provider
public class ApiVersionCheckFilter implements ContainerRequestFilter {

    @InjectParam("v1xDisabled")
    private Boolean v1xDisabled;
    
    @InjectParam("localeManager")
    private LocaleManager localeManager;
    
    @Context private HttpServletRequest httpRequest;

    private static final Pattern VERSION_PATTERN = Pattern.compile("v(\\d.*?)/");

    private static final String WEBHOOKS_PATH_PATTERN = OrcidStringUtils.ORCID_STRING + "/webhook/.+";
    
    public ApiVersionCheckFilter() {
    }
    
    public ApiVersionCheckFilter(HttpServletRequest req) {
        this.httpRequest = req;
    }
    
    public ApiVersionCheckFilter(LocaleManager locale, HttpServletRequest req) {
        this.httpRequest = req;
        this.localeManager = locale;
    }
    
    @Override
    public ContainerRequest filter(ContainerRequest request) {
        String path = request.getPath();
        String method = request.getMethod() == null ? null : request.getMethod().toUpperCase();
        Matcher matcher = VERSION_PATTERN.matcher(path);        
        String version = null;
        if (matcher.lookingAt()) {
            version = matcher.group(1);
        }
        
        if(PojoUtil.isEmpty(version) && !PojoUtil.isEmpty(method) && !"oauth/token".equals(path) && !path.matches(WEBHOOKS_PATH_PATTERN)) {
            if(!RequestMethod.GET.name().equals(method)) {
                Object params[] = {method};
                throw new OrcidBadRequestException(localeManager.resolveMessage("apiError.badrequest_missing_version.exception", params));    
            }
        } else if (version != null && version.startsWith("1.1") && v1xDisabled) {
            throw new OrcidBadRequestException(localeManager.resolveMessage("apiError.badrequest_version_disabled.exception"));
        } else if(version != null && version.startsWith("2.0")) {
            if(!OrcidUrlManager.isSecure(httpRequest)) {
                throw new OrcidBadRequestException(localeManager.resolveMessage("apiError.badrequest_secure_only.exception"));
            }
        }

        return request;
    }
}