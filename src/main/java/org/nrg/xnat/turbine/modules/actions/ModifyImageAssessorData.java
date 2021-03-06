/*
 * web: org.nrg.xnat.turbine.modules.actions.ModifyImageAssessorData
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xft.XFTItem;

public class ModifyImageAssessorData extends ModifyItem {

	@Override
	public void postProcessing(XFTItem item, RunData data, Context context)
			throws Exception {
		super.postProcessing(item, data, context);
//		
//		XnatImageassessordata assessor= (XnatImageassessordata) BaseElement.GetGeneratedItem(item);
//
//		final PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, TurbineUtils.getUser(data), assessor.getImageSessionData().getItem(),"Added image assessment");
//    	EventMetaI c=wrk.buildEvent();
//        PersistentWorkflowUtils.save(wrk,c);
	}
	
}
