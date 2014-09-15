/*global define*/
define(
    [
        'backbone',
        "mustache",
        "text!templates/blocker.html"
    ], function (Backbone, Mustache, template) {

        var BlockerView = Backbone.View.extend({

            tagName: "div",

            id: "blocker",

            render: function () {
                this.$el.html(Mustache.render(template, {i18n: APP_CONFIG.i18n}));
                return this;
            }

        });

        return BlockerView;

    });