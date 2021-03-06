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
package org.orcid.frontend.web.controllers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.helper.StringUtil;
import org.orcid.core.exception.OrcidDeprecatedException;
import org.orcid.core.exception.OrcidNotClaimedException;
import org.orcid.core.locale.LocaleManager;
import org.orcid.core.manager.ActivitiesSummaryManager;
import org.orcid.core.manager.ActivityCacheManager;
import org.orcid.core.manager.AddressManager;
import org.orcid.core.manager.EmailManager;
import org.orcid.core.manager.EncryptionManager;
import org.orcid.core.manager.ExternalIdentifierManager;
import org.orcid.core.manager.GroupIdRecordManager;
import org.orcid.core.manager.OtherNameManager;
import org.orcid.core.manager.PersonalDetailsManager;
import org.orcid.core.manager.ProfileEntityCacheManager;
import org.orcid.core.manager.ProfileEntityManager;
import org.orcid.core.manager.ProfileKeywordManager;
import org.orcid.core.manager.ResearcherUrlManager;
import org.orcid.core.manager.WorkManager;
import org.orcid.core.oauth.OrcidOauth2TokenDetailService;
import org.orcid.core.security.aop.LockedException;
import org.orcid.core.utils.RecordNameUtils;
import org.orcid.core.utils.SourceUtils;
import org.orcid.frontend.web.util.LanguagesMap;
import org.orcid.jaxb.model.common_v2.Visibility;
import org.orcid.jaxb.model.groupid_v2.GroupIdRecord;
import org.orcid.jaxb.model.message.CreationMethod;
import org.orcid.jaxb.model.record.summary_v2.ActivitiesSummary;
import org.orcid.jaxb.model.record_v2.Address;
import org.orcid.jaxb.model.record_v2.Addresses;
import org.orcid.jaxb.model.record_v2.Affiliation;
import org.orcid.jaxb.model.record_v2.Biography;
import org.orcid.jaxb.model.record_v2.Email;
import org.orcid.jaxb.model.record_v2.Emails;
import org.orcid.jaxb.model.record_v2.Funding;
import org.orcid.jaxb.model.record_v2.Keyword;
import org.orcid.jaxb.model.record_v2.Keywords;
import org.orcid.jaxb.model.record_v2.Name;
import org.orcid.jaxb.model.record_v2.OtherName;
import org.orcid.jaxb.model.record_v2.OtherNames;
import org.orcid.jaxb.model.record_v2.PeerReview;
import org.orcid.jaxb.model.record_v2.PersonExternalIdentifier;
import org.orcid.jaxb.model.record_v2.PersonExternalIdentifiers;
import org.orcid.jaxb.model.record_v2.PersonalDetails;
import org.orcid.jaxb.model.record_v2.ResearcherUrl;
import org.orcid.jaxb.model.record_v2.ResearcherUrls;
import org.orcid.jaxb.model.record_v2.Work;
import org.orcid.persistence.jpa.entities.CountryIsoEntity;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.orcid.pojo.ajaxForm.AffiliationForm;
import org.orcid.pojo.ajaxForm.Contributor;
import org.orcid.pojo.ajaxForm.FundingForm;
import org.orcid.pojo.ajaxForm.PeerReviewForm;
import org.orcid.pojo.ajaxForm.PojoUtil;
import org.orcid.pojo.ajaxForm.Text;
import org.orcid.pojo.ajaxForm.WorkForm;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class PublicProfileController extends BaseWorkspaceController {

    @Resource
    private LocaleManager localeManager;

    @Resource
    private WorkManager workManager;

    @Resource
    private EncryptionManager encryptionManager;

    @Resource
    private ActivityCacheManager activityCacheManager;

    @Resource(name = "languagesMap")
    private LanguagesMap lm;

    @Resource
    private ProfileEntityCacheManager profileEntityCacheManager;

    @Resource
    private ProfileEntityManager profileEntManager;
    
    @Resource
    private GroupIdRecordManager groupIdRecordManager;

    @Resource
    private AddressManager addressManager;

    @Resource
    private PersonalDetailsManager personalDetailsManager;

    @Resource
    private OrcidOauth2TokenDetailService orcidOauth2TokenService;

    @Resource
    private OtherNameManager otherNameManager;

    @Resource
    private ProfileKeywordManager keywordManager;

    @Resource
    private ResearcherUrlManager researcherUrlManager;

    @Resource
    private EmailManager emailManager;

    @Resource
    private ExternalIdentifierManager externalIdentifierManager;

    @Resource
    private SourceUtils sourceUtils;

    @Resource
    private ActivitiesSummaryManager activitiesSummaryManager;

    public static int ORCID_HASH_LENGTH = 8;

    private Long getLastModifiedTime(String orcid) {
        return profileEntManager.getLastModified(orcid);
    }
    
    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[x]}")
    public ModelAndView publicPreviewRedir(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "1") int pageNo,
            @RequestParam(value = "maxResults", defaultValue = "15") int maxResults, @PathVariable("orcid") String orcid) {
        RedirectView rv = new RedirectView();
        rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        rv.setUrl(getBasePath() + orcid.toUpperCase());
        return new ModelAndView(rv);
    }


    @RequestMapping(value = {"/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}", "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}/print"})
    public ModelAndView publicPreview(HttpServletRequest request, @RequestParam(value = "page", defaultValue = "1") int pageNo,
            @RequestParam(value = "v", defaultValue = "0") int v, @RequestParam(value = "maxResults", defaultValue = "15") int maxResults,
            @PathVariable("orcid") String orcid) {
               
        ProfileEntity profile = null; 
        
        try {
            profile = profileEntityCacheManager.retrieve(orcid);
        } catch(Exception e) {
            return new ModelAndView("error-404");
        }    
        
        try {
            // Check if the profile is deprecated, non claimed or locked
            orcidSecurityManager.checkProfile(orcid);
        } catch (OrcidDeprecatedException | OrcidNotClaimedException | LockedException e) {
            ModelAndView mav = new ModelAndView("public_profile_unavailable");
            mav.addObject("effectiveUserOrcid", orcid);
            String displayName = "";

            if (e instanceof OrcidDeprecatedException) {
                PersonalDetails publicPersonalDetails = personalDetailsManager.getPublicPersonalDetails(orcid);
                if (publicPersonalDetails.getName() != null) {
                    Name name = publicPersonalDetails.getName();
                    if (name.getVisibility().equals(org.orcid.jaxb.model.common_v2.Visibility.PUBLIC)) {
                        if (name.getCreditName() != null && !PojoUtil.isEmpty(name.getCreditName().getContent())) {
                            displayName = name.getCreditName().getContent();
                        } else {
                            if (name.getGivenNames() != null && !PojoUtil.isEmpty(name.getGivenNames().getContent())) {
                                displayName = name.getGivenNames().getContent() + " ";
                            }
                            if (name.getFamilyName() != null && !PojoUtil.isEmpty(name.getFamilyName().getContent())) {
                                displayName += name.getFamilyName().getContent();
                            }
                        }
                    }
                }
                mav.addObject("deprecated", true);
                mav.addObject("primaryRecord", profile.getPrimaryRecord().getId());
            } else if (e instanceof OrcidNotClaimedException) {
                displayName = localeManager.resolveMessage("orcid.reserved_for_claim");
            } else {
                mav.addObject("locked", true);
                mav.addObject("isPublicProfile", true);
                displayName = localeManager.resolveMessage("public_profile.deactivated.given_names") + " "
                        + localeManager.resolveMessage("public_profile.deactivated.family_name");
            }

            if (!PojoUtil.isEmpty(displayName)) {
                mav.addObject("title", getMessage("layout.public-layout.title", displayName, orcid));
                mav.addObject("displayName", displayName);
            }
            return mav;
        }

        long lastModifiedTime = getLastModifiedTime(orcid);
        
        ModelAndView mav = null;
        if (request.getRequestURI().contains("/print")) {
            mav = new ModelAndView("print_public_record");
            mav.addObject("hideUserVoiceScript", true);
        } else {
            mav = new ModelAndView("public_profile_v3");
        }
        mav.addObject("isPublicProfile", true);
        mav.addObject("effectiveUserOrcid", orcid);
        mav.addObject("lastModifiedTime", lastModifiedTime);

        boolean isProfileEmtpy = true;

        PersonalDetails publicPersonalDetails = personalDetailsManager.getPublicPersonalDetails(orcid);

        // Fill personal details
        if (publicPersonalDetails != null) {
            // Get display name
            String displayName = "";

            if (publicPersonalDetails.getName() != null) {
                Name name = publicPersonalDetails.getName();
                if (name.getVisibility().equals(org.orcid.jaxb.model.common_v2.Visibility.PUBLIC)) {
                    if (name.getCreditName() != null && !PojoUtil.isEmpty(name.getCreditName().getContent())) {
                        displayName = name.getCreditName().getContent();
                    } else {
                        if (name.getGivenNames() != null && !PojoUtil.isEmpty(name.getGivenNames().getContent())) {
                            displayName = name.getGivenNames().getContent() + " ";
                        }
                        if (name.getFamilyName() != null && !PojoUtil.isEmpty(name.getFamilyName().getContent())) {
                            displayName += name.getFamilyName().getContent();
                        }
                    }
                }
            }

            if (!PojoUtil.isEmpty(displayName)) {
                // <Published Name> (<ORCID iD>) - ORCID | Connecting Research
                // and Researchers
                mav.addObject("title", getMessage("layout.public-layout.title", displayName.trim(), orcid));
                mav.addObject("displayName", displayName);
            }

            // Get biography
            if (publicPersonalDetails.getBiography() != null) {
                Biography bio = publicPersonalDetails.getBiography();
                if (org.orcid.jaxb.model.common_v2.Visibility.PUBLIC.equals(bio.getVisibility()) && !PojoUtil.isEmpty(bio.getContent())) {
                    isProfileEmtpy = false;
                    mav.addObject("biography", bio);
                }
            }

            // Fill other names
            OtherNames publicOtherNames = publicPersonalDetails.getOtherNames();
            if (publicOtherNames != null && publicOtherNames.getOtherNames() != null) {
                Iterator<OtherName> it = publicOtherNames.getOtherNames().iterator();
                while (it.hasNext()) {
                    OtherName otherName = it.next();
                    if (!org.orcid.jaxb.model.common_v2.Visibility.PUBLIC.equals(otherName.getVisibility())) {
                        it.remove();
                    }
                }
            }
            Map<String, List<OtherName>> groupedOtherNames = groupOtherNames(publicOtherNames);
            mav.addObject("publicGroupedOtherNames", groupedOtherNames);
        }

        // Fill biography elements

        // Fill country
        Addresses publicAddresses = addressManager.getPublicAddresses(orcid, lastModifiedTime);
        Map<String, String> countryNames = new HashMap<String, String>();
        if (publicAddresses != null && publicAddresses.getAddress() != null) {
            Address publicAddress = null;
            // The primary address will be the one with the lowest display index
            for (Address address : publicAddresses.getAddress()) {
                countryNames.put(address.getCountry().getValue().value(), getcountryName(address.getCountry().getValue().value()));
                if (publicAddress == null) {
                    publicAddress = address;
                }
            }
            if (publicAddress != null) {
                mav.addObject("publicAddress", publicAddress);
                mav.addObject("countryNames", countryNames);
                Map<String, List<Address>> groupedAddresses = groupAddresses(publicAddresses);
                mav.addObject("publicGroupedAddresses", groupedAddresses);
            }
        }

        // Fill keywords
        Keywords publicKeywords = keywordManager.getPublicKeywords(orcid, lastModifiedTime);
        Map<String, List<Keyword>> groupedKeywords = groupKeywords(publicKeywords);
        mav.addObject("publicGroupedKeywords", groupedKeywords);

        // Fill researcher urls
        ResearcherUrls publicResearcherUrls = researcherUrlManager.getPublicResearcherUrls(orcid, lastModifiedTime);
        Map<String, List<ResearcherUrl>> groupedResearcherUrls = groupResearcherUrls(publicResearcherUrls);
        mav.addObject("publicGroupedResearcherUrls", groupedResearcherUrls);

        // Fill emails
        Emails publicEmails = emailManager.getPublicEmails(orcid, lastModifiedTime);
        Map<String, List<Email>> groupedEmails = groupEmails(publicEmails);
        mav.addObject("publicGroupedEmails", groupedEmails);

        // Fill external identifiers
        PersonExternalIdentifiers publicPersonExternalIdentifiers = externalIdentifierManager.getPublicExternalIdentifiers(orcid, lastModifiedTime);
        Map<String, List<PersonExternalIdentifier>> groupedExternalIdentifiers = groupExternalIdentifiers(publicPersonExternalIdentifiers);
        mav.addObject("publicGroupedPersonExternalIdentifiers", groupedExternalIdentifiers);

        LinkedHashMap<Long, WorkForm> minimizedWorksMap = new LinkedHashMap<>();
        LinkedHashMap<Long, Affiliation> affiliationMap = new LinkedHashMap<>();
        LinkedHashMap<Long, Funding> fundingMap = new LinkedHashMap<>();
        LinkedHashMap<Long, PeerReview> peerReviewMap = new LinkedHashMap<>();

        minimizedWorksMap = activityCacheManager.pubMinWorksMap(orcid, lastModifiedTime);
        if (minimizedWorksMap.size() > 0) {
            isProfileEmtpy = false;
        } else {
            mav.addObject("worksEmpty", true);
        }

        affiliationMap = affiliationMap(orcid, lastModifiedTime);
        if (affiliationMap.size() > 0) {
            isProfileEmtpy = false;
        } else {
            mav.addObject("affiliationsEmpty", true);
        }

        fundingMap = fundingMap(orcid, lastModifiedTime);
        if (fundingMap.size() > 0)
            isProfileEmtpy = false;
        else {
            mav.addObject("fundingEmpty", true);
        }

        peerReviewMap = peerReviewMap(orcid, lastModifiedTime);
        if (peerReviewMap.size() > 0) {
            isProfileEmtpy = false;
        } else {
            mav.addObject("peerReviewsEmpty", true);
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            String worksIdsJson = mapper.writeValueAsString(minimizedWorksMap.keySet());
            String affiliationIdsJson = mapper.writeValueAsString(affiliationMap.keySet());
            String fundingIdsJson = mapper.writeValueAsString(fundingMap.keySet());
            String peerReviewIdsJson = mapper.writeValueAsString(peerReviewMap.keySet());
            mav.addObject("workIdsJson", StringEscapeUtils.escapeEcmaScript(worksIdsJson));
            mav.addObject("affiliationIdsJson", StringEscapeUtils.escapeEcmaScript(affiliationIdsJson));
            mav.addObject("fundingIdsJson", StringEscapeUtils.escapeEcmaScript(fundingIdsJson));
            mav.addObject("peerReviewIdsJson", StringEscapeUtils.escapeEcmaScript(peerReviewIdsJson));
            mav.addObject("isProfileEmpty", isProfileEmtpy);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!profile.isReviewed()) {
            if (isProfileValidForIndex(profile)) {
                int countTokens = orcidOauth2TokenService.findCountByUserName(orcid, lastModifiedTime);
                if (!profile.isAccountNonLocked() || countTokens == 0
                        || (!CreationMethod.WEBSITE.value().equals(profile.getCreationMethod()) && !CreationMethod.DIRECT.value().equals(profile.getCreationMethod()))) {
                    mav.addObject("noIndex", true);
                }
            } else {
                mav.addObject("noIndex", true);
            }
        }

        return mav;
    }

    private LinkedHashMap<String, List<Keyword>> groupKeywords(Keywords keywords) {
        if (keywords == null || keywords.getKeywords() == null) {
            return null;
        }

        /* Grouping items */
        LinkedHashMap<String, List<Keyword>> groups = new LinkedHashMap<String, List<Keyword>>();
        for (Keyword k : keywords.getKeywords()) {
            if (groups.containsKey(k.getContent())) {
                groups.get(k.getContent()).add(k);
            } else {
                List<Keyword> list = new ArrayList<Keyword>();
                list.add(k);
                groups.put(k.getContent(), list);
            }
        }

        return groups;
    }

    private LinkedHashMap<String, List<Address>> groupAddresses(Addresses addresses) {
        if (addresses == null || addresses.getAddress() == null) {
            return null;
        }
        LinkedHashMap<String, List<Address>> groups = new LinkedHashMap<String, List<Address>>();
        for (Address k : addresses.getAddress()) {
            if (groups.containsKey(k.getCountry().getValue().value())) {
                groups.get(k.getCountry().getValue().value()).add(k);
            } else {
                List<Address> list = new ArrayList<Address>();
                list.add(k);
                groups.put(k.getCountry().getValue().value(), list);
            }
        }

        return groups;
    }

    private LinkedHashMap<String, List<OtherName>> groupOtherNames(OtherNames otherNames) {

        if (otherNames == null || otherNames.getOtherNames() == null) {
            return null;
        }
        LinkedHashMap<String, List<OtherName>> groups = new LinkedHashMap<String, List<OtherName>>();
        for (OtherName o : otherNames.getOtherNames()) {
            if (groups.containsKey(o.getContent())) {
                groups.get(o.getContent()).add(o);
            } else {
                List<OtherName> list = new ArrayList<OtherName>();
                list.add(o);
                groups.put(o.getContent(), list);
            }
        }
        return groups;

    }

    private Map<String, List<Email>> groupEmails(Emails emails) {
        if (emails == null || emails.getEmails() == null) {
            return null;
        }
        Map<String, List<Email>> groups = new TreeMap<String, List<Email>>();
        for (Email e : emails.getEmails()) {
            if (groups.containsKey(e.getEmail())) {
                groups.get(e.getEmail()).add(e);
            } else {
                List<Email> list = new ArrayList<Email>();
                list.add(e);
                groups.put(e.getEmail(), list);
            }
        }

        return groups;
    }

    private LinkedHashMap<String, List<ResearcherUrl>> groupResearcherUrls(ResearcherUrls researcherUrls) {
        if (researcherUrls == null || researcherUrls.getResearcherUrls() == null) {
            return null;
        }
        LinkedHashMap<String, List<ResearcherUrl>> groups = new LinkedHashMap<String, List<ResearcherUrl>>();
        for (ResearcherUrl r : researcherUrls.getResearcherUrls()) {
            String urlValue = r.getUrl() == null ? "" : r.getUrl().getValue();
            if (groups.containsKey(urlValue)) {
                groups.get(urlValue).add(r);
            } else {
                List<ResearcherUrl> list = new ArrayList<ResearcherUrl>();
                list.add(r);
                groups.put(urlValue, list);
            }
        }
        return groups;
    }

    private LinkedHashMap<String, List<PersonExternalIdentifier>> groupExternalIdentifiers(PersonExternalIdentifiers personExternalIdentifiers) {
        if (personExternalIdentifiers == null || personExternalIdentifiers.getExternalIdentifiers() == null) {
            return null;
        }
        LinkedHashMap<String, List<PersonExternalIdentifier>> groups = new LinkedHashMap<String, List<PersonExternalIdentifier>>();
        for (PersonExternalIdentifier ei : personExternalIdentifiers.getExternalIdentifiers()) {
            String pairKey = ei.getType() + ":" + ei.getValue();
            if (groups.containsKey(pairKey)) {
                groups.get(pairKey).add(ei);
            } else {
                List<PersonExternalIdentifier> list = new ArrayList<PersonExternalIdentifier>();
                list.add(ei);
                groups.put(pairKey, list);
            }
        }

        return groups;
    }

    private boolean isProfileValidForIndex(ProfileEntity profile) {
        String orcid = profile.getId();
        if (orcid != null) {
            int validAge = 3 + (Character.getNumericValue(orcid.charAt(orcid.length() - 2))) / 2;
            if (profile.getSubmissionDate() != null) {
                Date profileCreationDate = profile.getSubmissionDate();
                Date currentDate = new Date();

                Calendar temp = Calendar.getInstance();
                temp.setTime(profileCreationDate);
                temp.add(Calendar.DATE, validAge);
                profileCreationDate = temp.getTime();
                if (profileCreationDate.before(currentDate)) {
                    return true;
                }
            }
        }
        return false;
    }

    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}/affiliations.json")
    public @ResponseBody List<AffiliationForm> getAffiliationsJson(HttpServletRequest request, @PathVariable("orcid") String orcid,
            @RequestParam(value = "affiliationIds") String workIdsStr) {
        List<AffiliationForm> affs = new ArrayList<AffiliationForm>();
        Map<Long, Affiliation> affMap = affiliationMap(orcid, profileEntManager.getLastModified(orcid));
        String[] affIds = workIdsStr.split(",");
        for (String id : affIds) {
            Affiliation aff = affMap.get(Long.valueOf(id));
            validateVisibility(aff.getVisibility());
            AffiliationForm form = AffiliationForm.valueOf(aff);
            form.setCountryForDisplay(getMessage(buildInternationalizationKey(CountryIsoEntity.class, aff.getOrganization().getAddress().getCountry().name())));
            affs.add(form);            
        }

        return affs;
    }

    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}/fundings.json")
    public @ResponseBody List<FundingForm> getFundingsJson(HttpServletRequest request, @PathVariable("orcid") String orcid,
            @RequestParam(value = "fundingIds") String fundingIdsStr) {
        Map<String, String> languages = lm.buildLanguageMap(localeManager.getLocale(), false);
        List<FundingForm> fundings = new ArrayList<FundingForm>();
        Map<Long, Funding> fundingMap = fundingMap(orcid, getLastModifiedTime(orcid));
        String[] fundingIds = fundingIdsStr.split(",");
        for (String id : fundingIds) {
            Funding funding = fundingMap.get(Long.valueOf(id));
            validateVisibility(funding.getVisibility());
            sourceUtils.setSourceName(funding);
            FundingForm form = FundingForm.valueOf(funding);
            // Set type name
            if (funding.getType() != null) {
                form.setFundingTypeForDisplay(getMessage(buildInternationalizationKey(org.orcid.jaxb.model.message.FundingType.class, funding.getType().value())));
            }
            // Set translated title language name
            if (!(funding.getTitle().getTranslatedTitle() == null) && !StringUtils.isEmpty(funding.getTitle().getTranslatedTitle().getLanguageCode())) {
                String languageName = languages.get(funding.getTitle().getTranslatedTitle().getLanguageCode());
                form.getFundingTitle().getTranslatedTitle().setLanguageName(languageName);
            }
            // Set formatted amount
            if (funding.getAmount() != null && StringUtils.isNotBlank(funding.getAmount().getContent())) {
                BigDecimal bigDecimal = new BigDecimal(funding.getAmount().getContent());
                String formattedAmount = formatAmountString(bigDecimal);
                form.setAmount(Text.valueOf(formattedAmount));
            }

            // Set country name
            form.setCountryForDisplay(getMessage(buildInternationalizationKey(CountryIsoEntity.class, funding.getOrganization().getAddress().getCountry().name())));
            fundings.add(form);
        }
        return fundings;
    }

    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}/works.json")
    public @ResponseBody List<WorkForm> getWorkJson(HttpServletRequest request, @PathVariable("orcid") String orcid, @RequestParam(value = "workIds") String workIdsStr) {
        Map<String, String> countries = retrieveIsoCountries();
        Map<String, String> languages = lm.buildLanguageMap(localeManager.getLocale(), false);

        HashMap<Long, WorkForm> minimizedWorksMap = activityCacheManager.pubMinWorksMap(orcid, getLastModifiedTime(orcid));

        List<WorkForm> works = new ArrayList<WorkForm>();
        String[] workIds = workIdsStr.split(",");

        for (String workId : workIds) {
            if (minimizedWorksMap.containsKey(Long.valueOf(workId))) {
                WorkForm work = minimizedWorksMap.get(Long.valueOf(workId));                
                validateVisibility(work.getVisibility());                
                if (!PojoUtil.isEmpty(work.getCountryCode())) {
                    Text countryName = Text.valueOf(countries.get(work.getCountryCode().getValue()));
                    work.setCountryName(countryName);
                }
                // Set language name
                if (!PojoUtil.isEmpty(work.getLanguageCode())) {
                    Text languageName = Text.valueOf(languages.get(work.getLanguageCode().getValue()));
                    work.setLanguageName(languageName);
                }
                // Set translated title language name
                if (work.getTranslatedTitle() != null && !StringUtils.isEmpty(work.getTranslatedTitle().getLanguageCode())) {
                    String languageName = languages.get(work.getTranslatedTitle().getLanguageCode());
                    work.getTranslatedTitle().setLanguageName(languageName);
                }
                works.add(work);                
            }
        }

        return works;
    }

    /**
     * Returns the work info for a given work id
     * 
     * @param workId
     *            The id of the work
     * @return the content of that work
     */
    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}/getWorkInfo.json", method = RequestMethod.GET)
    public @ResponseBody WorkForm getWorkInfo(@PathVariable("orcid") String orcid, @RequestParam(value = "workId") Long workId) {
        Map<String, String> languages = lm.buildLanguageMap(localeManager.getLocale(), false);
        if (workId == null)
            return null;

        Work workObj = workManager.getWork(orcid, workId, profileEntManager.getLastModified(orcid));       
        if (workObj != null) {            
            validateVisibility(workObj.getVisibility());            
            sourceUtils.setSourceName(workObj);
            WorkForm work = WorkForm.valueOf(workObj);
            // Set country name
            if (!PojoUtil.isEmpty(work.getCountryCode())) {
                Text countryName = Text.valueOf(retrieveIsoCountries().get(work.getCountryCode().getValue()));
                work.setCountryName(countryName);
            }
            // Set language name
            if (!PojoUtil.isEmpty(work.getLanguageCode())) {
                Text languageName = Text.valueOf(languages.get(work.getLanguageCode().getValue()));
                work.setLanguageName(languageName);
            }
            // Set translated title language name
            if (work.getTranslatedTitle() != null && !StringUtils.isEmpty(work.getTranslatedTitle().getLanguageCode())) {
                String languageName = languages.get(work.getTranslatedTitle().getLanguageCode());
                work.getTranslatedTitle().setLanguageName(languageName);
            }

            if (work.getContributors() != null) {
                for (Contributor contributor : work.getContributors()) {
                    if (!PojoUtil.isEmpty(contributor.getOrcid())) {
                        String contributorOrcid = contributor.getOrcid().getValue();
                        if (profileEntManager.orcidExists(contributorOrcid)) {
                            ProfileEntity profileEntity = profileEntityCacheManager.retrieve(contributorOrcid);
                            String publicContributorCreditName = activityCacheManager.getPublicCreditName(profileEntity);
                            contributor.setCreditName(Text.valueOf(publicContributorCreditName));
                        }
                    }
                }
            }

            return work;
        }
        return null;
    }

    @RequestMapping(value = "/{orcid:(?:\\d{4}-){3,}\\d{3}[\\dX]}/peer-reviews.json")
    public @ResponseBody List<PeerReviewForm> getPeerReviewsJson(HttpServletRequest request, @PathVariable("orcid") String orcid,
            @RequestParam(value = "peerReviewIds") String peerReviewIdsStr) {
        Map<String, String> languages = lm.buildLanguageMap(localeManager.getLocale(), false);
        List<PeerReviewForm> peerReviews = new ArrayList<PeerReviewForm>();
        Map<Long, PeerReview> peerReviewMap = peerReviewMap(orcid, getLastModifiedTime(orcid));
        String[] peerReviewIds = peerReviewIdsStr.split(",");
        for (String id : peerReviewIds) {
            PeerReview peerReview = peerReviewMap.get(Long.valueOf(id));            
            validateVisibility(peerReview.getVisibility());            
            sourceUtils.setSourceName(peerReview);
            PeerReviewForm form = PeerReviewForm.valueOf(peerReview);
            // Set language name
            form.setCountryForDisplay(getMessage(buildInternationalizationKey(CountryIsoEntity.class, peerReview.getOrganization().getAddress().getCountry().name())));

            if (!PojoUtil.isEmpty(form.getTranslatedSubjectName())) {
                // Set translated title language name
                if (!StringUtils.isEmpty(form.getTranslatedSubjectName().getLanguageCode())) {
                    String languageName = languages.get(form.getTranslatedSubjectName().getLanguageCode());
                    form.getTranslatedSubjectName().setLanguageName(languageName);
                }
            }

            // Set the numeric id (the table id in the group_id_record table) of
            // the group id
            if (form.getGroupId() != null && !PojoUtil.isEmpty(form.getGroupId().getValue())) {
                GroupIdRecord groupId = groupIdRecordManager.findByGroupId(form.getGroupId().getValue()).get();
                form.setGroupIdPutCode(Text.valueOf(groupId.getPutCode()));
            }

            peerReviews.add(form);
        }
        return peerReviews;
    }

    /**
     * Get group information based on the group id
     * 
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "/public/group/{groupId}", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public @ResponseBody GroupIdRecord getGroupInformation(@PathVariable("groupId") Long groupId) {
        return groupIdRecordManager.getGroupIdRecord(groupId);
    }

    public LinkedHashMap<Long, Funding> fundingMap(String orcid, Long lastModified) {       
        return activityCacheManager.fundingMap(orcid, lastModified);
    }

    public LinkedHashMap<Long, Affiliation> affiliationMap(String orcid, Long lastModified) {
        return activityCacheManager.affiliationMap(orcid, lastModified);
    }

    public LinkedHashMap<Long, PeerReview> peerReviewMap(String orcid, Long lastModified) {
        return activityCacheManager.pubPeerReviewsMap(orcid, lastModified);
    }

    /**
     * Format a big decimal based on a locale
     * 
     * @param bigDecimal
     * @param currencyCode
     * @return a string with the number formatted based on the locale
     */
    private String formatAmountString(BigDecimal bigDecimal) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(localeManager.getLocale());
        return numberFormat.format(bigDecimal);
    }
    
    private void validateVisibility(Visibility v) {
        if(!Visibility.PUBLIC.equals(v)) {
            throw new IllegalArgumentException("Invalid request");
        }
    }
}

class OrcidInfo {
    public String orcid = "";
    public String name = "";
    public HashMap<String, String> values = new HashMap<String, String>();

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String name, String value) {
        values.put(name, value);
    }

    public String getValue(String name) {
        return values.get(name);
    }
}