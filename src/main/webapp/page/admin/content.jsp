<%@ page session="true" contentType="text/html" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="pg" tagdir="/WEB-INF/tags/page" %>

<pg:restricted>

    <link rel="stylesheet" type="text/css" href="${sessionScope.siteRoot}/page/admin/style.css">

    <div id="page-body">
        <div class="pad">

            <div id="admin-page">
                <header id="content-header">
                    <h2 class="pull-left">Site Administration</h2>
                    <div class="clearfix"></div>
                </header>

                <!-- Admin tab container -->
                <div id="admin-config-tabs" class="content-tabs xnat-tab-container">

                    <div class="xnat-nav-tabs side pull-left">
                        <!-- ================== -->
                        <!-- Admin tab flippers -->
                        <!-- ================== -->
                    </div>

                    <div class="xnat-tab-content side pull-right">
                        <!-- ================== -->
                        <!-- Admin tab panes    -->
                        <!-- ================== -->
                    </div>

                </div>

                <c:import url="/xapi/siteConfig" var="siteConfig"/>

                <script>
                    (function(){

                        XNAT.data = extend({}, XNAT.data, {
                            siteConfig: ${siteConfig}
                        });
                        // get rid of the 'targetSource' property
                        delete XNAT.data.siteConfig.targetSource;


                        // var jsonUrl = XNAT.url.rootUrl('/page/admin/data/config/site-admin-sample-new.yaml');
                        var jsonUrl = XNAT.url.rootUrl('/xapi/spawner/resolve/siteAdmin/adminPage');

                        $.get({
                            url: jsonUrl,
                            // dataType: 'text',
                            success: function(data){

                                if (typeof data === 'string') {
                                    data = YAML.parse(data);
                                }

                                // console.log(JSON.stringify(data, ' ', 1));

                                var adminTabs = XNAT.spawner.spawn(data);
                                adminTabs.render('#admin-config-tabs > .xnat-tab-content');
                                XNAT.app.adminTabs = adminTabs;

                                // xmodal.loading.closeAll();

                            }
                        });

                    })();
                </script>

            </div>

        </div>
    </div>
    <!-- /#page-body -->

    <div id="xnat-scripts">
        <script>
            //        $(window).load(function(){
            //            // any values that start with '@|' will be set to
            //            // the value of the element with matching selector
            //            $('[value^="@|"]').each(function(){
            //                var selector = $(this).val().split('@|')[1];
            //                var value = $$(selector).val();
            //                $(this).val(value).dataAttr('value',value);
            //            });
            //        });
        </script>
    </div>

</pg:restricted>
