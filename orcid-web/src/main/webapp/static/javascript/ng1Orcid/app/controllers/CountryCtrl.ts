declare var $: any;
declare var colorbox: any;
declare var contains: any;
declare var getBaseUri: any;
declare var logAjaxError: any;
declare var orcidVar: any;

import * as angular from 'angular';
import {NgModule} from '@angular/core';

// This is the Angular 1 part of the module
export const CountryCtrl = angular.module('orcidApp').controller(
    'CountryCtrl', 
    [
        '$scope', 
        '$rootScope', 
        '$compile', 
        'bioBulkSrvc', 
        'commonSrvc', 
        'emailSrvc', 
        'initialConfigService', 
        'utilsService', 
        function (
            $scope, 
            $rootScope, 
            $compile, 
            bioBulkSrvc, 
            commonSrvc, 
            emailSrvc, 
            initialConfigService, 
            utilsService
        ) {
            bioBulkSrvc.initScope($scope);
            $scope.commonSrvc = commonSrvc;
            $scope.countryForm = null;
            $scope.defaultVisibility = null;
            $scope.emailSrvc = emailSrvc;
            $scope.newElementDefaultVisibility = null;
            $scope.newInput = false;    
            $scope.orcidId = orcidVar.orcidId;
            $scope.primaryElementIndex = null;
            $scope.privacyHelp = false;
            $scope.scrollTop = 0;   
            $scope.showEdit = false;
            $scope.showElement = {};

            /////////////////////// Begin of verified email logic for work
            var configuration = initialConfigService.getInitialConfiguration();
            var emailVerified = false;
            var emails = {};
            var utilsService = utilsService;

            var showEmailVerificationModal = function(){
                $rootScope.$broadcast('emailVerifiedObj', {flag: emailVerified, emails: emails});
            };

            $scope.emailSrvc.getEmails(
                function(data) {
                    emails = data.emails;
                    if( $scope.emailSrvc.getEmailPrimary().verified == true ) {
                        emailVerified = true;
                    }
                }
            );
            /////////////////////// End of verified email logic for work

            $scope.addNewModal = function() {       
                var tmpObj = {
                    "errors":[],
                    "iso2Country": null,
                    "countryName":null,
                    "putCode":null,
                    "visibility":{
                        "errors":[],
                        "required":true,
                        "getRequiredMessage":null,
                        "visibility":$scope.newElementDefaultVisibility
                    },
                    "displayIndex":1,
                    "source":$scope.orcidId,
                    "sourceName":""
                };
                $scope.countryForm.addresses.push(tmpObj);
                $scope.updateDisplayIndex();
                $scope.newInput = true; 
            };

            $scope.closeEditModal = function(){
                $.colorbox.close();
            };

            $scope.closeModal = function(){     
                $.colorbox.close();
            };

            $scope.deleteCountry = function(country){
                var countries = $scope.countryForm.addresses;
                var len = countries.length;
                while (len--) {
                    if (countries[len] == country){
                        countries.splice(len,1);
                        $scope.countryForm.addresses = countries;
                    }       
                }
            };

            $scope.getCountryForm = function(){
                $.ajax({
                    url: getBaseUri() + '/account/countryForm.json',
                    dataType: 'json',
                    success: function(data) {
                        $scope.countryForm = data;                
                        $scope.newElementDefaultVisibility = $scope.countryForm.visibility.visibility;
                        //If there is at least one element, iterate over them to see if they have the same visibility, to set the default  visibility element
                        if($scope.countryForm != null && $scope.countryForm.addresses != null && $scope.countryForm.addresses.length > 0) {
                            var highestDisplayIndex = null;
                            var itemVisibility = null;
                            
                            for(var i = 0; i < $scope.countryForm.addresses.length; i ++) {
                                if($scope.countryForm.addresses[i].visibility != null && $scope.countryForm.addresses[i].visibility.visibility) {
                                    itemVisibility = $scope.countryForm.addresses[i].visibility.visibility;
                                }
                                /**
                                 * The default visibility should be set only when all elements have the same visibility, so, we should follow this rules: 
                                 * 
                                 * Rules: 
                                 * - If the default visibility is null:
                                 *  - If the item visibility is not null, set the default visibility to the item visibility
                                 * - If the default visibility is not null:
                                 *  - If the default visibility is not equals to the item visibility, set the default visibility to null and stop iterating 
                                 * */
                                if($scope.defaultVisibility == null) {
                                    if(itemVisibility != null) {
                                        $scope.defaultVisibility = itemVisibility;
                                    }                           
                                } else {
                                    if(itemVisibility != null) {
                                        if($scope.defaultVisibility != itemVisibility) {
                                            $scope.defaultVisibility = null;
                                            break;
                                        }
                                    } else {
                                        $scope.defaultVisibility = null;
                                        break;
                                    }
                                }                                                                   
                            }
                            //We have to iterate on them again to select the primary address
                            for(var i = 0; i < $scope.countryForm.addresses.length; i ++) {
                                //Set the primary element based on the display index
                                if($scope.primaryElementIndex == null || highestDisplayIndex < $scope.countryForm.addresses[i].displayIndex) {
                                    $scope.primaryElementIndex = i;
                                    highestDisplayIndex = $scope.countryForm.addresses[i].displayIndex;
                                }
                            }
                        } else {
                            $scope.defaultVisibility = $scope.countryForm.visibility.visibility;                    
                        }     
                        $scope.$apply();                
                    }
                }).fail(function(e){
                    // something bad is happening!
                    console.log("error fetching external identifiers");
                    logAjaxError(e);
                });
            };

            $scope.openEditModal = function() {
                
                if(emailVerified === true || configuration.showModalManualEditVerificationEnabled == false){
                    $scope.bulkEditShow = false;
                    
                    $.colorbox({
                        html: $compile($('#edit-country').html())($scope),
                        scrolling: true,
                        onLoad: function() {
                            $('#cboxClose').remove();
                            if ($scope.countryForm.addresses.length == 0){                  
                                $scope.addNewModal();
                            } else {
                                if ($scope.countryForm.addresses.length == 1){
                                    if($scope.countryForm.addresses[0].source == null){
                                        $scope.countryForm.addresses[0].source = $scope.orcidId;
                                        $scope.countryForm.addresses[0].sourceName = "";
                                    }
                                }
                                $scope.updateDisplayIndex();
                            }                
                        },
             
                        width: utilsService.formColorBoxResize(),
                        onComplete: function() {      
                        },
                        onClosed: function() {
                            $scope.getCountryForm();
                        }            
                    });
                    $.colorbox.resize();
                }else{
                    showEmailVerificationModal();
                }
            };

            $scope.setBulkGroupPrivacy = function(priv) {
                for (var idx in $scope.countryForm.addresses){
                    $scope.countryForm.addresses[idx].visibility.visibility = priv;        
                }
            };

            $scope.setCountryForm = function(){
                $scope.countryForm.visibility = null;
                $.ajax({
                    contentType: 'application/json;charset=UTF-8',
                    data:  angular.toJson($scope.countryForm),
                    dataType: 'json',
                    type: 'POST',
                    url: getBaseUri() + '/account/countryForm.json',
                    success: function(data) {
                        $scope.countryForm = data;
                        if ($scope.countryForm.errors.length == 0){
                            $.colorbox.close();
                            $scope.getCountryForm();
                        }else{
                            console.log($scope.countryForm.errors);
                        }
                        
                        $scope.$apply();
                    }
                }).fail(function() {
                    // something bad is happening!
                    console.log("CountryCtrl.serverValidate() error");
                });
            };
            
            $scope.setPrivacy = function(priv, $event) {
                $event.preventDefault();
                $scope.defaultVisibility = priv;
            };
            
            $scope.setPrivacyModal = function(priv, $event, country) {        
                var countries = $scope.countryForm.addresses;        
                var len = countries.length;   

                $event.preventDefault();

                while (len--) {
                    if (countries[len] == country){            
                        countries[len].visibility.visibility = priv;
                        $scope.countryForm.addresses = countries;
                    }
                }
            };
            
            $scope.swapDown = function(index){
                var temp = null;
                var tempDisplayIndex = null;
                if (index < $scope.countryForm.addresses.length - 1) {
                    temp = $scope.countryForm.addresses[index];
                    tempDisplayIndex = $scope.countryForm.addresses[index]['displayIndex'];
                    temp['displayIndex'] = $scope.countryForm.addresses[index + 1]['displayIndex']
                    
                    $scope.countryForm.addresses[index] = $scope.countryForm.addresses[index + 1];
                    $scope.countryForm.addresses[index]['displayIndex'] = tempDisplayIndex;
                    $scope.countryForm.addresses[index + 1] = temp;
                }
            };

            $scope.swapUp = function(index){
                var temp = null;
                var tempDisplayIndex = null;
                if (index > 0) {
                    temp = $scope.countryForm.addresses[index];
                    tempDisplayIndex = $scope.countryForm.addresses[index]['displayIndex'];
                    temp['displayIndex'] = $scope.countryForm.addresses[index - 1]['displayIndex']
                    
                    $scope.countryForm.addresses[index] = $scope.countryForm.addresses[index - 1];
                    $scope.countryForm.addresses[index]['displayIndex'] = tempDisplayIndex;
                    $scope.countryForm.addresses[index - 1] = temp;
                }
            };

            $scope.updateDisplayIndex = function():any {
                let displayIndex: any;
                let countryFormAddressesLength: any;
                let idx: any;
                
                for ( idx in $scope.countryForm.addresses ){
                    countryFormAddressesLength = $scope.countryForm.addresses.length;
                    displayIndex = countryFormAddressesLength - idx;
                    $scope.countryForm.addresses[idx]['displayIndex'] = displayIndex;
                }
            };

            $scope.getCountryForm();

        }
    ]
);

// This is the Angular 2 part of the module
@NgModule({})
export class CountryCtrlNg2Module {}