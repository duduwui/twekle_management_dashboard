/**
 * Common Utility Service: Internationalization (i18n)
 * Manages active language state ('en', 'ar', 'ku') and static dictionary lookups.
 */
app.factory('I18nService', [function() {
    var currentLanguage = 'en';

    var translations = {
        en: {
            app_title: 'Twekl Management',
            admin_management: 'Admin Management',
            users_permissions: 'Users & Permissions',
            followups: 'Follow-ups',
            time_filters: 'Time Filters',
            reports: 'Reports',
            save: 'Save',
            cancel: 'Cancel',
            delete: 'Delete',
            confirm: 'Confirm'
        },
        ar: {
            app_title: 'إدارة توكل',
            admin_management: 'إدارة المسؤولين',
            users_permissions: 'المستخدمين والصلاحيات',
            followups: 'المتابعات',
            time_filters: 'فلاتر الوقت',
            reports: 'التقارير',
            save: 'حفظ',
            cancel: 'إلغاء',
            delete: 'حذف',
            confirm: 'تأكيد'
        },
        ku: {
            app_title: 'بەڕێوەبردنی توکل',
            admin_management: 'بەڕێوەبردنی ئەدمینەکان',
            users_permissions: 'بەکارهێنەران و دەسەڵاتەکان',
            followups: 'بەدواداچوونەکان',
            time_filters: 'فیلتەرەکانی کات',
            reports: 'ڕاپۆرتەکان',
            save: 'پاشەکەوت',
            cancel: 'هەڵوەشاندنەوە',
            delete: 'سڕینەوە',
            confirm: 'پشتڕاستکردنەوە'
        }
    };

    return {
        getLanguage: function() { return currentLanguage; },
        setLanguage: function(lang) {
            if (translations[lang]) {
                currentLanguage = lang;
                document.documentElement.setAttribute('dir', (lang === 'ar' || lang === 'ku') ? 'rtl' : 'ltr');
                document.documentElement.setAttribute('lang', lang);
            }
        },
        t: function(key) {
            return (translations[currentLanguage] && translations[currentLanguage][key]) || key;
        }
    };
}]);
