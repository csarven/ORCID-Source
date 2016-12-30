angular.module('orcidApp').controller('SocialNetworksCtrl',['$scope',function ($scope){
    $scope.twitter=false;

    $scope.checkTwitterStatus = function(){
        $.ajax({
            url: getBaseUri() + '/manage/twitter/check-twitter-status',
            type: 'GET',
            contentType: 'application/json;charset=UTF-8',
            dataType: 'text',
            success: function(data) {
                if(data == "true")
                    $scope.twitter = true;
                else
                    $scope.twitter = false;
                $scope.$apply();
            }
        }).fail(function(){
            console.log("Unable to fetch user twitter status");
        });
    };

    $scope.updateTwitter = function() {
        if($scope.twitter == true) {
            $.ajax({
                url: getBaseUri() + '/manage/twitter',
                type: 'POST',
                contentType: 'application/json;charset=UTF-8',
                dataType: 'text',
                success: function(data) {
                    window.location = data;
                }
            }).fail(function() {
                console.log("Unable to enable twitter");
            });
        } else {
            $.ajax({
                url: getBaseUri() + '/manage/disable-twitter',
                type: 'POST',
                contentType: 'application/json;charset=UTF-8',
                dataType: 'text',
                success: function(data) {
                    if(data == "true"){
                        $scope.twitter = false;
                    } else {
                        $scope.twitter = true;
                    }

                    $scope.$apply();
                }
            }).fail(function() {
                console.log("Unable to disable twitter");
            });
        }
    };

    //init
    $scope.checkTwitterStatus();
}]);