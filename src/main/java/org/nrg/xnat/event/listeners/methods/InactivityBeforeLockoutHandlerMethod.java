package org.nrg.xnat.event.listeners.methods;

import com.google.common.collect.ImmutableList;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xnat.security.DisableInactiveUsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Component
public class InactivityBeforeLockoutHandlerMethod extends AbstractSiteConfigPreferenceHandlerMethod {
    @Autowired
    public InactivityBeforeLockoutHandlerMethod(final SiteConfigPreferences preferences, final ThreadPoolTaskScheduler scheduler) {
        _preferences = preferences;
        _scheduler = scheduler;
    }

    @Override
    public List<String> getHandledPreferences() {
        return PREFERENCES;
    }

    @Override
    public void handlePreferences(final Map<String, String> values) {
        if (!Collections.disjoint(PREFERENCES, values.keySet())) {
            updateInactivityBeforeLockout();
        }
    }

    @Override
    public void handlePreference(final String preference, final String value) {
        if (PREFERENCES.contains(preference)) {
            updateInactivityBeforeLockout();
        }
    }

    private void updateInactivityBeforeLockout() {
        _scheduler.getScheduledThreadPoolExecutor().setRemoveOnCancelPolicy(true);
        for (ScheduledFuture temp : _scheduledInactivityBeforeLockout) {
            temp.cancel(false);
        }
        _scheduledInactivityBeforeLockout.clear();
        try {
            _scheduledInactivityBeforeLockout.add(_scheduler.schedule(new DisableInactiveUsers((new Long(SiteConfigPreferences.convertPGIntervalToSeconds(_preferences.getInactivityBeforeLockout()))).intValue(), (new Long(SiteConfigPreferences.convertPGIntervalToSeconds(_preferences.getMaxFailedLoginsLockoutDuration()))).intValue()), new CronTrigger(_preferences.getInactivityBeforeLockoutSchedule())));
        } catch (SQLException ignored) {
            // Do nothing: the SQL exception here is superfluous.
        }
    }

    private static final List<String> PREFERENCES = ImmutableList.copyOf(Arrays.asList("inactivityBeforeLockout", "inactivityBeforeLockoutSchedule"));

    private final ArrayList<ScheduledFuture> _scheduledInactivityBeforeLockout = new ArrayList<>();

    private final SiteConfigPreferences   _preferences;
    private final ThreadPoolTaskScheduler _scheduler;
}
