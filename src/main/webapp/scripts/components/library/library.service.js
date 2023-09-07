(function () {
    'use strict';

    angular.module('numaHopApp').factory('Library', function ($resource) {
        return $resource(
            'api_int/libraries/:login',
            {},
            {
                query: {
                    method: 'GET',
                    isArray: true,
                },
                get: {
                    method: 'GET',
                    transformResponse: function (data) {
                        data = angular.fromJson(data);
                        return data;
                    },
                },
            }
        );
    });
})();
