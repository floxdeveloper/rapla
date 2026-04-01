package org.rapla.enpoints.server;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rapla.entities.Entity;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentMapping;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.internal.AppointmentImpl;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.entities.storage.ReferenceInfo;
import org.rapla.facade.RaplaFacade;
import org.rapla.facade.internal.CalendarModelImpl;
import org.rapla.framework.RaplaException;
import org.rapla.rest.PATCH;
import org.rapla.scheduler.Promise;
import org.rapla.server.PromiseWait;
import org.rapla.server.RemoteSession;
import org.rapla.server.internal.SecurityManager;
import org.rapla.storage.CachableStorageOperator;
import org.rapla.storage.PermissionController;
import org.rapla.storage.RaplaSecurityException;
import org.rapla.storage.StorageOperator;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Path("events")
@Tag(name = "Events", description = "Manage reservations/events")
public class RaplaEventsRestPage
{
    @Inject RaplaFacade facade;
    @Inject RemoteSession session;
    @Inject SecurityManager securityManager;
    private final HttpServletRequest request;
    @Inject CachableStorageOperator operator;
    @Inject PromiseWait promiseWait;

    @Inject public RaplaEventsRestPage(@Context HttpServletRequest request)
    {
        this.request = request;
    }

    private Collection<String> CLASSIFICATION_TYPES = Arrays.asList(DynamicTypeAnnotations.VALUE_CLASSIFICATION_TYPE_RESERVATION);

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Operation(
        summary = "List events/reservations",
        description = "Get a list of events/reservations filtered by date range, resources, owners and event types"
    )
    @ApiResponse(responseCode = "200", description = "List of events", content = @Content(schema = @Schema(implementation = ReservationImpl.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public List<ReservationImpl> list(
            @Parameter(description = "Start date (ISO 8601 format)", required = false) @QueryParam("start") Date start,
            @Parameter(description = "End date (ISO 8601 format)", required = false) @QueryParam("end") Date end,
            @Parameter(description = "List of resource IDs to filter by", required = false) @QueryParam("resources") List<String> resources,
            @Parameter(description = "List of owner user IDs to filter by", required = false) @QueryParam("owners") List<String> ownersId,
            @Parameter(description = "List of event type keys to filter by", required = false) @QueryParam("eventTypes") Collection<String> eventTypes,
            @Parameter(description = "Attribute filter as JSON map", required = false) @QueryParam("attributeFilter") Map<String, String> simpleFilter) throws Exception
    {
        final User user = session.checkAndGetUser(request);
        Collection<Allocatable> allocatables = new ArrayList<>();
        for (String id : resources)
        {
            Allocatable allocatable = facade.resolve(new ReferenceInfo<Allocatable>(id, Allocatable.class));
            allocatables.add(allocatable);
        }

        Collection<User> owners = new ArrayList<>();
        if ( ownersId != null ) {
            for (String id : ownersId) {
                User owner = facade.resolve(new ReferenceInfo<User>(id, User.class));
                owners.add(owner);
            }
        }

        final ClassificationFilter[] filters = RaplaResourcesRestPage.getClassificationFilter(facade, simpleFilter, CLASSIFICATION_TYPES, eventTypes);
        final Map<String, String> annotationQuery = null;
        final User owner = null;
        final Promise<AppointmentMapping> promise = operator
                .queryAppointments(owner, allocatables, owners, start, end, filters, annotationQuery, false);
        final AppointmentMapping appMap = promiseWait.waitForWithRaplaException(promise, 20000);
        final List<ReservationImpl> result = new ArrayList<>();
        final Collection<Reservation> reservations = appMap.getAllReservations();
        PermissionController permissionController = facade.getPermissionController();
        for (Reservation r : reservations)
        {
            if (permissionController.canRead(r, user))
            {
                result.add((ReservationImpl) r);
            }
        }
        return result;
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Operation(summary = "Get event by ID", description = "Retrieve a specific event/reservation by its ID")
    @ApiResponse(responseCode = "200", description = "Event found", content = @Content(schema = @Schema(implementation = ReservationImpl.class)))
    @ApiResponse(responseCode = "404", description = "Event not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ReservationImpl get(@Parameter(description = "Event/Reservation ID", required = true) @PathParam("id") String id)
            throws RaplaException
    {
        final User user = session.checkAndGetUser(request);
        final StorageOperator operator = facade.getOperator();
        ReservationImpl event = (ReservationImpl) operator.resolve(id, Reservation.class);
        securityManager.checkRead(user, event);
        return event;
    }

    @PATCH
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Operation(summary = "Partially update event", description = "Update specific fields of an event/reservation")
    @ApiResponse(responseCode = "200", description = "Event updated", content = @Content(schema = @Schema(implementation = ReservationImpl.class)))
    @ApiResponse(responseCode = "404", description = "Event not found")
    @ApiResponse(responseCode = "403", description = "Forbidden - no write permissions")
    public ReservationImpl patch(@Parameter(description = "Event/Reservation ID", required = true) @PathParam("id") String id,
                                @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Partial event update", required = true) ReservationImpl event) throws RaplaException

    {
        final User user = session.checkAndGetUser(request);
        setResolver(event);
        securityManager.checkWritePermissions(user, event);
        facade.store(event);
        ReservationImpl result = facade.getPersistent(event);
        return result;
    }

    protected void setResolver(ReservationImpl event)
    {
        final StorageOperator operator = facade.getOperator();
        event.setResolver(operator);
    }
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Operation(summary = "Update event", description = "Update a complete event/reservation")
    @ApiResponse(responseCode = "200", description = "Event updated", content = @Content(schema = @Schema(implementation = ReservationImpl.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden - no write permissions")
    public ReservationImpl update(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Event object to update", required = true) ReservationImpl event) throws RaplaException
    {
        final User user = session.checkAndGetUser(request);
        setResolver(event);
        securityManager.checkWritePermissions(user, event);
        facade.store(event);
        ReservationImpl result = facade.getPersistent(event);
        return result;
    }

    @DELETE 
    @Path("{id}") 
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Operation(summary = "Delete event", description = "Delete an event/reservation by ID")
    @ApiResponse(responseCode = "200", description = "Event deleted")
    @ApiResponse(responseCode = "404", description = "Event not found")
    @ApiResponse(responseCode = "403", description = "Forbidden - no delete permissions")
    public boolean delete(@Parameter(description = "Event/Reservation ID", required = true) @PathParam("id") String id) throws RaplaException
    {
        final User user = session.checkAndGetUser(request);
        final Reservation event = facade.tryResolve(new ReferenceInfo<Reservation>(id, Reservation.class));
        if ( event == null)
        {
            return false;
        }
        securityManager.checkDeletePermissions(user, event);
        facade.remove(event);
        return true;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Operation(summary = "Create event", description = "Create a new event/reservation")
    @ApiResponse(responseCode = "201", description = "Event created", content = @Content(schema = @Schema(implementation = ReservationImpl.class)))
    @ApiResponse(responseCode = "400", description = "Invalid event data")
    @ApiResponse(responseCode = "403", description = "Forbidden - no create permissions")
    public ReservationImpl create(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "New event object", required = true) ReservationImpl event) throws RaplaException
    {
        final User user = session.checkAndGetUser(request);
        setResolver( event);
        if (!facade.getPermissionController().canCreate(event.getClassification().getType(), user))
        {
            throw new RaplaSecurityException("User " + user + " can't modify event " + event);
        }
        if (event.getId() != null)
        {
            throw new RaplaException("Id has to be null for new events");
        }
        ReferenceInfo<Reservation> eventId = operator.createIdentifier(Reservation.class, 1).get(0);
        event.setId(eventId.getId());
        Appointment[] appointments = event.getAppointments();
        List<ReferenceInfo<Appointment>> appointmentIds = operator.createIdentifier(Appointment.class, appointments.length);
        for (int i = 0; i < appointments.length; i++)
        {
            AppointmentImpl app = (AppointmentImpl) appointments[i];
            String id = appointmentIds.get(i).getId();
            app.setId(id);
        }
        event.setOwner(user);
        facade.storeAndRemove(new Entity[] { event }, Entity.ENTITY_ARRAY, user);
        ReservationImpl result = facade.getPersistent(event);
        return result;
    }

}
