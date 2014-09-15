/*global casper,__utils__,workspaceUrl,folderCreationName*/
'use strict';
casper.test.begin('Folder creation tests suite',1, function folderCreationTestsSuite(){

    /**
     * Open document management URL
     * */
    casper.open(documentManagementUrl);

    /**
     * Open folder nav
     */
    casper.then(function waitForFolderNavLink(){
        this.waitForSelector('#folder-nav > .nav-list-entry > a',function clickFolderNavLink() {
            this.click('#folder-nav > .nav-list-entry > a');
        });
    });

    /**
     * Open folder creation modal
     */
    casper.then(function clickOnFolderCreationLink(){
        this.click('#folder-nav > div.nav-list-entry > div.btn-group > ul.dropdown-menu > li.new-folder > a');
        this.waitForSelector('#new-folder-form',function openFolderCreationModal(){
            this.sendKeys('#new-folder-form input', folderCreationName, {reset:true});
            this.click('button[form=new-folder-form]');
        });
    });

    /**
     *  Check if folder has been created
     * */
    casper.then(function checkIfFolderHasBeenCreated(){
        this.waitForSelector('a[href="#'+workspace+'/folders/'+folderCreationName+'"]',function(){
            this.test.assert(true,'Folder has been created');
        });
    });

    casper.run(function allDone() {
        this.test.done();
    });
});