package org.nrg.xnat.eventservice.listeners;


import reactor.bus.Event;
import reactor.fn.Consumer;

public interface EventServiceListener<T> extends Consumer<Event<T>> {
    String getEventType();
}
