DocumentNewAttributesView = BaseView.extend({
	template_el: "#document-new-attributes-tpl",
	initialize: function () {
		_.bindAll(this,
			"template", "render");
	},
	render: function () {
		var jsonModel = this.model ? this.model.toJSON() : {};
		$(this.el).html(this.template({
			model: jsonModel
		}));
		return this;
	},
});
