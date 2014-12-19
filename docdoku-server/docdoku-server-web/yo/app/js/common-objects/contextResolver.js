/*global _,$,define,App*/
define([
    'common-objects/models/user',
    'common-objects/models/workspace',
    'common-objects/views/forbidden'
], function (User, Workspace, ForbiddenView) {
    'use strict';
    var ContextResolver = function () {
    };

    ContextResolver.prototype.resolve = function (success) {
        $.getJSON('../server.properties.json').success(function (properties) {
            App.config.contextPath = properties.contextRoot;
            User.whoami(App.config.workspaceId,
                function (user, groups) {
                    Workspace.getWorkspaces(function (workspaces) {
                        App.config.login = user.login;
                        App.config.userName = user.name;
                        App.config.groups = groups;
                        App.config.workspaces = workspaces;
                        App.config.workspaceAdmin = _.select(App.config.workspaces.administratedWorkspaces, function (workspace) {
                            return workspace.id === App.config.workspaceId;
                        }).length === 1;
                        window.localStorage.setItem('locale', user.language || 'en');
                        success();
                    });
                }, function () {
                    var forbiddenView = new ForbiddenView().render();
                    $('body').append(forbiddenView.$el);
                    forbiddenView.openModal();
                }
            );
        });
    };

    return new ContextResolver();
});
