package org.nrg.xnat.eventservice.services.impl;

import org.nrg.xdat.model.XnatExperimentdataI;
import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.model.XnatSubjectdataI;
import org.nrg.xnat.eventservice.services.XnatObjectIntrospectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class XnatObjectIntrospectionServiceImpl implements XnatObjectIntrospectionService {

    private static final Logger log = LoggerFactory.getLogger(XnatObjectIntrospectionService.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public XnatObjectIntrospectionServiceImpl(final NamedParameterJdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }


    @Override
    public Boolean isModified(XnatExperimentdataI experiment) {
        try{
            List<Map<String, Object>> result = jdbcTemplate.queryForList(QUERY_IS_EXPERIMENT_MODIFIED,
                                                                        new MapSqlParameterSource("experimentId", experiment.getId()));
            if (!result.isEmpty() && result.get(0).containsKey("modified") && result.get(0).get("modified").equals(1)) return true;
            else return false;
        }catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    public Boolean hasResource(XnatExperimentdataI experiment) {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(QUERY_EXPERIMENTDATA_RESOURCE,
                                                                        new MapSqlParameterSource("experimentId", experiment.getId()));
            if (result == null || result.isEmpty()) return false;
            else return true;
        } catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    public Boolean hasHistory(XnatSubjectdataI subject) {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(QUERY_SUBJECTDATA_HISTORY,
                    new MapSqlParameterSource("subjectId", subject.getId()));
            if (result == null || result.isEmpty()) return false;
            else return true;
        } catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Boolean hasResource(XnatSubjectdataI subject) {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(QUERY_SUBJECTDATA_RESOURCE,
                    new MapSqlParameterSource("subjectId", subject.getId()));
            if (result == null || result.isEmpty()) return false;
            else return true;
        } catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Integer getResourceCount(XnatProjectdataI project) {
        Integer result = jdbcTemplate.queryForObject(COUNT_PROJECTDATA_RESOURCES,
                new MapSqlParameterSource("projectId", project.getId()), Integer.class);
        return result;
    }


    private static final String QUERY_IS_EXPERIMENT_MODIFIED =  "SELECT xnat_experimentdata_meta_data.modified AS modified FROM xnat_experimentdata_meta_data WHERE meta_data_id IN " +
                                                                    "(SELECT experimentdata_info FROM xnat_experimentData WHERE id = :experimentId)";

    private static final String QUERY_EXPERIMENTDATA_RESOURCE = "SELECT * FROM xnat_experimentdata_resource WHERE xnat_experimentdata_id = :experimentId";

    private static final String QUERY_SUBJECTDATA_HISTORY = "SELECT * FROM xnat_subjectdata_history WHERE subjectdata_info IN (SELECT subjectdata_info FROM xnat_subjectdata WHERE id = :subjectId)";

    private static final String QUERY_SUBJECTDATA_RESOURCE = "SELECT * FROM xnat_subjectdata_resource WHERE xnat_subjectdata_id = :subjectId";

    private static final String COUNT_PROJECTDATA_RESOURCES = "SELECT COUNT(*) FROM xnat_projectdata_meta_data WHERE meta_data_id IN (SELECT projectdata_info FROM xnat_projectdata WHERE id = :projectId)";
}
