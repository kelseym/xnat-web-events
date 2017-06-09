package org.nrg.xnat.eventservice.events;


import org.nrg.xdat.om.XnatImagesessiondata;

@Deprecated
public interface SessionEvent {

     XnatImagesessiondata getSession();
}
