/*
 * web: PrearchiveDetails_fileDialog.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *  
 * Released under the Simplified BSD.
 */

XNAT.app.fileDialog = XNAT.app.fileDialog || {};

XNAT.app.fileDialog.loadScan = function( url, title ){

    xmodal.closeAll(); // get rid of any lingering modals

    xmodal.loading.open();

    var modalOpts={};
    modalOpts.width = '80%';
    // modalOpts.height = '80%';
    modalOpts.maxBtn = true;
    modalOpts.nuke = true;
    modalOpts.buttons = [
        {
            label: 'Close',
            close: true,
            isDefault: true
        }
    ];
    modalOpts.beforeShow = function(){
        xmodal.loading.close();
    };

    var getData = $.ajax({
        type: 'GET',
        url: url,
        cache: false ,
        dataType: 'html'
    });

    getData.done(function(data){
        modalOpts.title = title;
        modalOpts.content = data;
        XNAT.ui.dialog.open(modalOpts);
    });

    getData.fail(function(jqXHR, textStatus, errorThrown){
        modalOpts.title = 'Error - ' + title;
        modalOpts.content = 'Error: ' + textStatus;
        XNAT.ui.dialog.open(modalOpts);
    });

};
