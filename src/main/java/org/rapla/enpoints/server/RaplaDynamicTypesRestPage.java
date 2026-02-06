package org.rapla.enpoints.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rapla.entities.User;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.internal.DynamicTypeImpl;
import org.rapla.facade.RaplaFacade;
import org.rapla.framework.RaplaException;
import org.rapla.server.RemoteSession;
import org.rapla.storage.PermissionController;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.List;

@Path("dynamictypes")
@Tag(name = "Dynamic Types", description = "Get available classification types")
public class RaplaDynamicTypesRestPage
{
    @Inject
    RemoteSession session;
    @Inject
    RaplaFacade facade;
    private final HttpServletRequest request;
    @Inject
    public RaplaDynamicTypesRestPage(@Context HttpServletRequest request)
    {
        this.request = request;
    }

    @GET
    @Operation(
        summary = "List dynamic types",
        description = "Get a list of available dynamic types filtered by classification type (resource, person, or reservation)"
    )
    @ApiResponse(responseCode = "200", description = "List of dynamic types", content = @Content(schema = @Schema(implementation = DynamicTypeImpl.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public List<DynamicTypeImpl> list(@Parameter(description = "Classification type filter: resource, person, or reservation", required = false) @QueryParam("classificationType") String classificationType) throws RaplaException
    {
        final User user = session.checkAndGetUser(request);
        DynamicType[] types = facade.getDynamicTypes(classificationType);
        List<DynamicTypeImpl> result = new ArrayList<>();
        final PermissionController controller  =   facade.getPermissionController();
        for (DynamicType type : types)
        {
            if ( controller.canRead( type, user))
            {
                result.add((DynamicTypeImpl) type);
            }
        }
        return result;
    }

}
