/*
 * web: org.nrg.xnat.turbine.modules.screens.XDATScreen_brief_xnat_imageSessionData
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/**
 * 
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.bean.XnatImagesessiondataBean;



/**
 * @author tolsen01
 *
 */
public class XDATScreen_brief_xnat_imageSessionData extends PrearchiveSessionScreen {
	/* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void finalProcessing(XnatImagesessiondataBean session, RunData data, Context context) throws Exception{

        
    }
}
