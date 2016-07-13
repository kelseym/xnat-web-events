/*!
 * Functions for creating XNAT tab UI elements
 */

var XNAT = getObject(XNAT || {});

(function(factory){
    if (typeof define === 'function' && define.amd) {
        define(factory);
    }
    else if (typeof exports === 'object') {
        module.exports = factory();
    }
    else {
        return factory();
    }
}(function(){

    var ui, tab, tabs, page,
        urlHashValue = getUrlHashValue('#',':');
    
    XNAT.ui = ui =
        getObject(XNAT.ui || {});

    XNAT.ui.tab = XNAT.tab = tab =
        getObject(XNAT.ui.tab || XNAT.tab || {});

    XNAT.ui.tabs = XNAT.tabs = tabs =
        getObject(XNAT.ui.tabs || XNAT.tabs || {});

    XNAT.page = page =
        getObject(XNAT.page || {});


    // ==================================================
    // SET UP ONE TAB GROUP
    // add a single tab group to the groups
    tab.group = function(obj){
        var id = toDashed(obj.id || obj.name);
        if (!id) return; // a tab group MUST have an id
        return spawn('ul.nav.tab-group', { id: id }, [
            ['li.label', (obj.label || obj.title || obj.text || 'Tab Group')]
        ]);
    };
    // ==================================================


    // ==================================================
    // SET UP TAB GROUPS
    tab.groups = function(obj){
        var groups = [];
        $.each(obj, function(name, label){
            groups.push(tab.group({
                id: toDashed(name),
                label: label
            }));
        });
        // console.log(groups);
        return groups;
    };
    // ==================================================


    // save the id of the active tab
    tab.active = '';


    // ==================================================
    // SELECT A TAB
    tab.select = tab.activate = function(name, container){
        container = container || tabs.container || 'body';
        $$(container).find('li.tab[data-tab="' + name + '"]').trigger('click');
    };
    // ==================================================


    // ==================================================
    // CREATE A SINGLE TAB
    tab.init = function _tab(obj){

        var $group, groupId, tabId, tabIdHash, _flipper, _pane;

        obj = cloneObject(obj);
        obj.config = cloneObject(obj.config);

        tabId = toDashed(obj.id || obj.name || randomID('t', false));

        _flipper = spawn('li.tab', {
            data: { tab: tabId }
        }, [
            ['a', {
                title: obj.label,
                // href: '#'+obj.config.id,
                href: '#' + tabId,
                html: obj.label
            }]
        ]);

        // setup the footer for the whole tab pane
        function paneFooter(){
            return spawn('footer.footer', [
                ['button', {
                    type: 'button',
                    html: 'Save All',
                    classes: 'save-all btn btn-primary pull-right'
                }]
            ]);
        }
        tab.paneFooter = paneFooter;

        obj.config.data =
            extend(true, {}, obj.config.data, {
                name: obj.name||'',
                tab: tabId
            });

        _pane = spawn('div.tab-pane', obj.config);

        // if 'active' is explicitly set, use the tabId value
        obj.active = (obj.active) ? tabId : '';

        // set active tab on page load if tabId matches url hash
        if (urlHashValue && urlHashValue === tabId) {
            tabIdHash = tabId;
        }

        if ((tabIdHash||obj.active) === tabId) {
            //$(_flipper).addClass('active');
            //$(_pane).addClass('active');
            tabs.active = tab.active = tabId;
        }

        groupId = toDashed(obj.group||'other');

        // un-hide the group that this tab is in
        // (groups are hidden until there is a tab for them)
        $group = $('#' + groupId + '.tab-group');

        $group.show();
        
        // add all the flippers
        $group.append(_flipper);

        function onRender(){
            console.log('tab: ' + tabId)
        }

        function get(){
            return _pane;
        }

        return {
            // contents: obj.tabs||obj.contents||obj.content||'',
            flipper: _flipper,
            pane: _pane,
            element: _pane,
            spawned: _pane,
            onRender: onRender,
            get: get
        }
    };
    // ==================================================

    
    // ==================================================
    // MAIN FUNCTION
    tabs.init = function tabsInit(obj){

        var layout, container, $container, 
            navTabs, tabContent;

        // set container and layout before spawning:
        // XNAT.tabs.container = 'div.foo';
        container = obj.container || tabs.container || 'div.xnat-tab-container';

        layout = obj.layout || tabs.layout || 'left';

        navTabs = spawn('div.xnat-nav-tabs');
        tabContent = spawn('div.xnat-tab-content');

        if (layout === 'left') {
            navTabs.className += ' side pull-left';
            tabContent.className += ' side pull-right';
        }

        $container = $$(container).hide();

        $container.append(navTabs);
        $container.append(tabContent);

        // set up the group elements
        $(navTabs).append(tab.groups(obj.meta.tabGroups));

        // bind tab click events
        $container.on('click', 'li.tab', function(e){
            e.preventDefault();
            var clicked = $(this).data('tab');
            // de-activate all tabs and panes
            $container.find('[data-tab]').removeClass('active');
            // activate the clicked tab and pane
            $container.find('[data-tab="' + clicked + '"]').addClass('active');
            // set the url hash
            //var baseUrl = window.location.href.split('#')[0];
            window.location.replace('#' + clicked);
        });

        function onRender($element){
            $container.find('li.tab, div.tab-pane').removeClass('active');
            $container.find('[data-tab="' + tabs.active + '"]').addClass('active');
            // console.log($element);
            // $container.find('li.tab.active').last().trigger('click');
        }

        function get(){
            return tabContent;
        }

        return {
            // contents: obj.tabs||obj.contents||obj.content||'',
            element: tabContent,
            spawned: tabContent,
            onRender: onRender,
            get: get
        };

    };
    // ==================================================

    // activate tab indicated in url hash
    // $(function(){
    //     if (window.location.hash) {
    //         tab.activate(getUrlHashValue())
    //     }
    // });
    
    tabs.tab = tab;

    return tabs;

}));

