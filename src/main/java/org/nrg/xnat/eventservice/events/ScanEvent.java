package org.nrg.xnat.eventservice.events;

import org.nrg.xdat.om.XnatImagescandata;

@Deprecated
public interface ScanEvent {
     XnatImagescandata getScan();

}
