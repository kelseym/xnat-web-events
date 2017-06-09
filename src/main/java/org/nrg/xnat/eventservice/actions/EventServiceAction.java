package org.nrg.xnat.eventservice.actions;

import java.util.List;

@Deprecated
public interface EventServiceAction {
    String getName();
    String getDisplayName();
    String getDescription();
    List<String> getEvents();
    List<String> getAttributeKeys();
    Boolean getEnabled();
    void setEnabled(Boolean enabled);
}
