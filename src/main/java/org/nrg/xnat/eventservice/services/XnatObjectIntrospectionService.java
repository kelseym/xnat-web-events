package org.nrg.xnat.eventservice.services;

import org.nrg.xdat.model.XnatExperimentdataI;
import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.model.XnatSubjectdataI;
import org.nrg.xdat.om.XnatExperimentdata;

import java.util.List;

public interface XnatObjectIntrospectionService {

    Boolean isModified(XnatExperimentdataI experiment);

    Boolean hasResource(XnatExperimentdataI experiment);

    Boolean hasHistory(XnatSubjectdataI subject);

    Boolean hasResource(XnatSubjectdataI subject);

    Boolean storedInDatabase(XnatSubjectdataI subject);

    Boolean storedInDatabase(XnatExperimentdata experiment);

    List<Integer> getScanIds(XnatExperimentdata experiment);

    Integer getResourceCount(XnatProjectdataI project);

}
