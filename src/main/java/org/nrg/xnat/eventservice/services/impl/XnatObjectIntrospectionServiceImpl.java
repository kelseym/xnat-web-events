package org.nrg.xnat.eventservice.services.impl;

import org.nrg.xdat.model.XnatExperimentdataI;
import org.nrg.xnat.eventservice.daos.XnatObjectIntrospectionDao;
import org.nrg.xnat.eventservice.services.XnatObjectIntrospectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class XnatObjectIntrospectionServiceImpl implements XnatObjectIntrospectionService {

    private static final Logger log = LoggerFactory.getLogger(XnatObjectIntrospectionService.class);
    private XnatObjectIntrospectionDao dao;

    @Autowired
    public XnatObjectIntrospectionServiceImpl(XnatObjectIntrospectionDao dao) {
        this.dao = dao;
    }


    @Override
    public Boolean isModified(XnatExperimentdataI experiment) {
        try{
            return dao.isExperimentModified(experiment.getId());
        }catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }
}
