define(    [
    "text!templates/marker_create_modal.html",
    "i18n!localization/nls/product-structure-strings"
],

    function (template, i18n) {

        var MarkerCreateModalView = Backbone.View.extend({

            events: {
                "click .save-marker" : "saveMarker",
                "hidden #creationMarkersModal": "onHidden"
            },

            template: Mustache.compile(template),

            initialize: function() {
                _.bindAll(this);
            },

            render: function() {
                this.$el.html(this.template({i18n: i18n}));
                this.$modal = this.$("#creationMarkersModal");
                this.$markersModalInputName = this.$('input');
                this.$markersModalInputDescription = this.$('textarea');

                return this;
            },

            saveMarker: function() {
                this.model.createMarker(this.$markersModalInputName.val(), this.$markersModalInputDescription.val(), this.options.intersectPoint.x, this.options.intersectPoint.y, this.options.intersectPoint.z);
                this.closeModal();
            },

            openModal: function() {
                this.$modal.modal('show');
                this.$markersModalInputName.focus();
            },

            closeModal: function() {
                this.$modal.modal('hide');
            },

            onHidden: function() {
                this.remove();
            }

        });

        return MarkerCreateModalView;
    }

);