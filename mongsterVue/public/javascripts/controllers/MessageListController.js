var mongsterVueApp = angular.module('mongsterVueApp', ['ui.bootstrap']);
mongsterVueApp.controller('MessageListController', ['$scope','$http', function($scope, $http) {
  
  $scope.messages = [];

  $scope.setMessages = function(messages) {
    $scope.messages = messages;
  };

  $scope.updateList = function() {
    $http.get('/messages.json').success(function(data) {
      $scope.messages = data.messages;
    });
  };

  setInterval(function() {
    $scope.updateList();
    $scope.$apply();
  }, 30 * 1000); // update every 30 seconds

  $scope.updateList();

  $scope.clear = function() {
    $http.post('/clear').success(function(data) {
      console.log("Cleared");
      $scope.updateList();
      $scope.$apply();
    });
  };

}]);

