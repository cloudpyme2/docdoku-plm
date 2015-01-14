/*global define,App*/
define([
    'common-objects/common/singleton_decorator',
    'common-objects/views/base',
    'views/template_content_list',
    'text!templates/nav/template_nav.html',
    '../../../product-management/router'
], function (singletonDecorator, BaseView, TemplateContentListView, template, Router) {
    'use strict';
    var TemplateNavView = BaseView.extend({

        template: template,
        el: '#template-nav',

        initialize: function () {
            BaseView.prototype.initialize.apply(this, arguments);
            this.render();
        },
        setActive: function () {
            if (App.$documentManagementMenu) {
                App.$documentManagementMenu.find('.active').removeClass('active');
            }
            this.$el.find('.nav-list-entry').first().addClass('active');
        },
        showContent: function () {
            this.setActive();
            this.addSubView(
                new TemplateContentListView()
            ).render();
        }
    });
    TemplateNavView = singletonDecorator(TemplateNavView);
    return TemplateNavView;
});
