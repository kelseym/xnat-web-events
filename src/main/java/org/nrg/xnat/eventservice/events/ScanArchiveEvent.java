package org.nrg.xnat.eventservice.events;


import org.nrg.framework.event.XnatEventServiceEvent;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xnat.eventservice.listeners.EventServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@XnatEventServiceEvent(
                name="ScanArchiveEvent",
                displayName = "Scan Archived",
                description="Session Archive Event",
                object = "Scan",
                operation = "Archived")
public class ScanArchiveEvent extends CombinedEventServiceEvent<ScanArchiveEvent, XnatImagescandata>  {
    private static final Logger log = LoggerFactory.getLogger(ScanArchiveEvent.class);

    public ScanArchiveEvent(){};

    public ScanArchiveEvent(final XnatImagescandata payload, final Integer eventUserId) {
        super(payload, eventUserId);
    }

    @Override
    public String getDisplayName() {
        return "Scan Archived";
    }

    @Override
    public String getDescription() {
        return "Session Archive Event";
    }

    @Override
    public String getPayloadXnatType() {
        return "xnat:imageScanData";
    }

    @Override
    public Boolean isPayloadXsiType() {
        return true;
    }

    @Override
    public EventServiceListener getInstance() {
        return new ScanArchiveEvent();
    }
}