/**
 * Time Filter Feature Controller
 * Manages time filter presets, creation, toggling, and deletion.
 */
angular.module('tweklApp')
.controller('TimeFilterController', ['$scope', 'TimeFilterGetAllService', 'TimeFilterCreateService', 'TimeFilterToggleService', 'TimeFilterDeleteService', 'ToastService',
function($scope, TimeFilterGetAllService, TimeFilterCreateService, TimeFilterToggleService, TimeFilterDeleteService, ToastService) {

    $scope.timePresets = [];
    $scope.newPreset = {
        presetKey: '',
        labelEn: '',
        labelAr: '',
        labelKu: '',
        durationValue: 1,
        durationUnit: 'DAYS'
    };

    /**
     * Load all presets
     */
    $scope.loadPresets = function() {
        TimeFilterGetAllService.execute().then(function(res) {
            $scope.timePresets = res.data || [];
        }, function() {
            ToastService.error('Failed to load time filter presets');
        });
    };

    /**
     * Create new preset
     */
    $scope.createPreset = function() {
        if (!$scope.newPreset.labelEn) {
            ToastService.error('Please provide a label');
            return;
        }
        $scope.newPreset.presetKey = 'PRESET_' + Date.now();

        TimeFilterCreateService.execute($scope.newPreset).then(function() {
            ToastService.success('Time filter preset created!');
            $scope.newPreset = { presetKey: '', labelEn: '', labelAr: '', labelKu: '', durationValue: 1, durationUnit: 'DAYS' };
            $scope.loadPresets();
        }, function(err) {
            ToastService.error('Failed to create time preset');
        });
    };

    /**
     * Toggle preset active state
     */
    $scope.togglePreset = function(preset) {
        TimeFilterToggleService.execute(preset.id).then(function() {
            preset.active = !preset.active;
            ToastService.success('Preset updated');
        }, function() {
            ToastService.error('Failed to toggle preset');
        });
    };

    /**
     * Delete preset
     */
    $scope.deletePreset = function(id) {
        TimeFilterDeleteService.execute(id).then(function() {
            ToastService.success('Preset deleted');
            $scope.loadPresets();
        }, function() {
            ToastService.error('Failed to delete preset');
        });
    };

    $scope.loadPresets();
}]);
