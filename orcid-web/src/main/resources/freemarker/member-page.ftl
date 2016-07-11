<#--

    =============================================================================

    ORCID (R) Open Source
    http://orcid.org

    Copyright (c) 2012-2014 ORCID, Inc.
    Licensed under an MIT-Style License (MIT)
    http://orcid.org/open-source-license

    This copyright and license information (including a link to the full license)
    shall be included in its entirety in all copies or substantial portion of
    the software.

    =============================================================================

-->
<@public classes=['home']>
    <div class="row" ng-controller="MemberPageController" ng-init="membersListSrvc.getCurrentMemberDetailsBySlug('${memberSlug}')">
        <div class="col-md-9 col-md-offset-3 col-sm-12 col-xs-12">
            <h1>{{membersListSrvc.currentMemberDetails.member.name}}</h1>
            <p>{{membersListSrvc.currentMemberDetails.member.researchCommunity}} | {{membersListSrvc.currentMemberDetails.member.country}}</p>
            <p>
                <img class="member-logo" ng-hide="membersListSrvc.currentMemberDetails.member.logoUrl == null" src="{{membersListSrvc.currentMemberDetails.member.logoUrl}}">
                <span ng-bind-html="renderHtml(membersListSrvc.currentMemberDetails.member.description)" ng-show="membersListSrvc.currentMemberDetails.member.description"></span>
            </p>
            <div class="clear-fix">
                <p>
                    <b>Consortium/Parent Organization: </b> 
                    <span ng-show="membersListSrvc.currentMemberDetails.parentOrgName">{{membersListSrvc.currentMemberDetails.parentOrgName}}</span>
                    <span ng-hide="membersListSrvc.currentMemberDetails.parentOrgName">None</span>
                </p>
                <h3>Integrations</h3>
                <div ng-show="membersListSrvc.currentMemberDetails.integrations" ng-repeat="integration in membersListSrvc.currentMemberDetails.integrations">
                    <p><b>{{integration.name}}</b> <em>{{integration.stage}}</em></p>
                    <p>
                        <span ng-bind-html="renderHtml(integration.description)" ng-show="integration.description">
                            
                        </span>
                   </p>
                   <p>
                        <span ng-show="integration.resourceUrl">
                            <a href="{{integration.resourceUrl}}" target="_blank">Learn more about this integration</a>
                        </span>
                    </p>
                </div>
                <div ng-hide="membersListSrvc.currentMemberDetails.integrations.length"> 
                    <p>This member has not completed any integrations.</p>
                </div>
            </div>
        </div>
    </div>
</@public>