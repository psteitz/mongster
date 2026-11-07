angular.module('mongsterVueApp').controller('MessageListController', ['$scope', '$http', '$interval', function($scope, $http, $interval) {

  $scope.messages = [];

  $scope.setMessages = function(messages) {
    $scope.messages = messages;
  };

  $scope.updateList = function() {
    return $http.get('/messages.json').then(function(response) {
      $scope.messages = response.data.messages;
    });
  };

  var timer = $interval($scope.updateList, 30 * 1000); // update every 30 seconds
  $scope.$on('$destroy', function() {
    $interval.cancel(timer);
  });

  $scope.updateList();

  $scope.clear = function() {
    return $http.post('/clear').then(function() {
      console.log("Cleared");
      return $scope.updateList();
    });
  };

}]);
