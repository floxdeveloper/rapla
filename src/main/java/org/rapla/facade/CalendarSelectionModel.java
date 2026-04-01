package org.rapla.facade;

import org.rapla.components.util.TimeInterval;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.planningstatus.PlanningStatusFilter;
import org.rapla.plugin.planningstatus.PlanningStatusPlugin;
import org.rapla.scheduler.Promise;

import java.util.Collection;
import java.util.Date;
import java.util.function.Predicate;

public interface CalendarSelectionModel extends CalendarModel{
    String getTitle();

    String getAnnotation(String annotationName);

	void setTitle(String title);

	void setViewId(String viewId);

	String getViewId();
		
	void setSelectedObjects(Collection<? extends Object> selectedObjects);

    void setOption( String name, String string );

    void setReservationFilter(ClassificationFilter[] array);

    void setAllocatableFilter(ClassificationFilter[] filters);

    void resetExports();
    Promise<Void> save(final String filename);

    void load(final String filename) throws RaplaException;

    default void loadRegardingPlanningStatus(final String filename, Preferences systemPreferences) throws RaplaException {
        boolean planningStatusEnabled = systemPreferences.getEntryAsBoolean(PlanningStatusPlugin.ENABLED, PlanningStatusPlugin.ENABLE_BY_DEFAULT);
        load(filename);
        if ( planningStatusEnabled )
        {
            setAppointmentFilter(PlanningStatusFilter.createFromCalendarModel( this));
        }
    }

    CalendarSelectionModel clone();
	
	void setMarkedIntervals(Collection<TimeInterval> timeIntervals,  boolean timeEnabled);
	/** calls setMarkedIntervals with a single interval from start to end*/
	void markInterval(Date start, Date end);

	void setMarkedAllocatables(Collection<Allocatable> allocatable);

    boolean isMarkedIntervalTimeEnabled();

    void setAppointmentFilter(Predicate<Appointment> appointmentFilter);
}