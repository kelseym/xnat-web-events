package org.nrg.xnat.eventservice.services;

import org.nrg.xdat.model.XnatExperimentdataI;

public interface XnatObjectIntrospectionService {

    Boolean isModified(XnatExperimentdataI experiment);

}
