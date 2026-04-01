package org.rapla.enpoints.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.rapla.RaplaResources;
import org.rapla.components.util.Tools;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.facade.RaplaFacade;
import org.rapla.framework.RaplaException;
import org.rapla.framework.internal.AbstractRaplaLocale;
import org.rapla.logger.Logger;
import org.rapla.server.internal.RaplaAuthentificationService;
import org.rapla.server.internal.TokenHandler;
import org.rapla.storage.RaplaSecurityException;
import org.rapla.storage.dbrm.LoginCredentials;
import org.rapla.storage.dbrm.LoginTokens;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("login")
@Tag(name = "Authentication", description = "User login and authentication")
public class RaplaAuthRestPage
{

    public static final String LOGIN_COOKIE = "raplaLoginToken";
    @Inject
    RaplaAuthentificationService authentificationService;
    @Inject
    RaplaResources i18n;
    @Inject
    RaplaFacade facade;
    @Inject
    Logger logger;
    @Inject
    TokenHandler tokenHandler;

    @Inject
    public RaplaAuthRestPage()
    {
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    @Operation(
        summary = "Authentication with credentials",
        description = "Authenticate user with username and password. \r\n" +
                        "Send JSON with username/password in body, returns JWT token\r\n" +
                        "Content negotiation via Accept header: application/json → JSON response, text/plain → token string only",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginTokens.class))),
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
        }
    )
    public Response create(LoginCredentials credentials, @Context HttpHeaders headers) throws Exception {
        final LoginTokens tokens = dummy(credentials);
        // Check Accept header for content negotiation
        String acceptHeader = headers.getHeaderString("Accept");
        if (acceptHeader != null && acceptHeader.contains(MediaType.TEXT_PLAIN)) {
            return Response.ok(tokens.getAccessToken(), MediaType.TEXT_PLAIN).build();
        }
        
        // Default: return full LoginTokens object
        return Response.ok(tokens).build();
    }

    @POST
    @Path("form")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Operation(
        summary = "Form-based Login (Web)",
        description = "Classic login via HTML form. Sets a cookie and redirects if necessary.",
        responses = {
            @ApiResponse(responseCode = "302", description = "Redirect to target URL after successful login"),
            @ApiResponse(responseCode = "200", description = "Login page with error message on failed attempt")
        }
    )
    public void createFormBased(
        @Parameter(description = "Optional target URL for redirect") @QueryParam("url") String url, 
        @Parameter(description = "Username") @FormParam("username") String user, 
        @Parameter(description = "Password") @FormParam("password") String password,
        @Parameter(description = "Connect as (assume identity)") @FormParam("connectAs") String connectAs, 
        @Context HttpServletResponse response) throws Exception {
        final String targetUrl = url != null ? Tools.createXssSafeString(url) : "../../apiTest.html";
        URI uri = new URI(targetUrl);
        if (uri.isAbsolute()) {
            throw new RaplaSecurityException("Absolute target urls are not allowed at this point.");
        }
        final String errorMessage;
        if (user != null)
        {
            try
            {
                if (connectAs == null)
                {
                    final String[] split = user.split(" su ");
                    if (split.length > 1) {
                        user = split[0];
                        connectAs = split[1];
                    }
                }
                final LoginTokens token = dummy(new LoginCredentials(user, password, connectAs));
                final int i = targetUrl.indexOf("#");
                String newUrl;
                final String accessToken = token.getAccessToken();
                if (i >= 0)
                {
                    boolean last = i == targetUrl.length() - 1;
                    newUrl = targetUrl + (last ? "" : "&") + LOGIN_COOKIE + "=" + accessToken;
                }
                else
                {
                    newUrl = targetUrl + "#" + LOGIN_COOKIE + "=" + accessToken;
                }
                newUrl += "&valid_until=" + token.getValidUntil().getTime();
                final Cookie cookie = new Cookie(LOGIN_COOKIE, token.toString());
                cookie.setPath("/");
                response.addCookie(cookie);
                response.sendRedirect(newUrl);
                final PrintWriter writer = response.getWriter();
                writer.println(accessToken);
                writer.close();
                return;
            }
            catch (Exception e)
            {
                errorMessage = e.getMessage();
            }
        }
        else
        {
            errorMessage = null;
        }
        createPage(url, user, errorMessage, response);
    }

    
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Operation(
        summary = "Get HTML page",
        description = "Retrieves HTML content for a given URL. If no URL is provided, returns the default login page."
    )
    @ApiResponse(
        responseCode = "200",
        description = "HTML page content retrieved successfully",
        content = @Content(mediaType = "text/html")
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid URL parameter"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error while retrieving HTML content"
    )
    public void getHtml(@QueryParam("url") String url, @Context HttpServletResponse response) throws IOException
    {
        createPage(url, null, null, response);
    }

    protected void createPage(String url, final String user, final String errorMessage, HttpServletResponse response) throws IOException
    {
        final String userName = Tools.createXssSafeString(user);

        response.setContentType("text/html; charset=ISO-8859-1");
        PrintWriter out = response.getWriter();
        String linkPrefix = "../";//request.getPathTranslated() != null ? "../" : "";

        out.println("<html>");
        out.println("  <head>");
        // add the link to the stylesheet for this page within the <head> tag
        out.println("    <link REL=\"stylesheet\" href=\"" + linkPrefix + "login.css\" type=\"text/css\">");
        // tell the html page where its favourite icon is stored
        out.println("    <link REL=\"shortcut icon\" type=\"image/x-icon\" href=\"" + linkPrefix + "images/favicon.ico\">");
        out.println("    <title>");
        String title = null;
        final String defaultTitle = i18n.getString("rapla.title");
        try
        {
            final Preferences systemPreferences = facade.getSystemPreferences();
            title = systemPreferences.getEntryAsString(AbstractRaplaLocale.TITLE, defaultTitle);
        }
        catch (RaplaException e)
        {
            title = defaultTitle;
        }

        out.println(title);
        out.println("    </title>");
        out.println("  </head>");
        out.println("  <body>");
        out.println("    <form method=\"post\">");
        if (url != null)
        {
            out.println("       <input type=\"hidden\" name=\"url\" value=\"" + Tools.createXssSafeString(url) + "\"/>");
        }
        out.println("           <div class=\"loginOuterPanel\">");
        out.println("               <div class=\"loginInputPanel\">");
        final String userNameValue = userName != null ? userName : "";
        out.println("                   <input name=\"username\" type=\"text\" value=\"" + userNameValue + "\">");
        out.println("                   <input name=\"password\"type=\"password\" >");
        out.println("               </div>");
        out.println("               <div class=\"loginCommandPanel\">");
        out.println("                   <button type=\"submit\" class=\"sendButton\">login</button>");
        out.println("               </div>");
        out.println("           </div>");
        if (errorMessage != null)
        {
            out.println("       <div class=\"errorMessage\">" + errorMessage + "</div>");
        }
        out.println("  </body>");
        out.println("</html>");
        out.close();
    }

    private LoginTokens dummy(LoginCredentials credentials) throws RaplaException
    {
        User user = null;
        try
        {
            user = authentificationService.authenticate(credentials.getUsername(), credentials.getPassword(), credentials.getConnectAs(), logger);
        }
        catch(Exception e)
        {
            logger.error(e.getMessage());
            final String loginErrorMessage = i18n.getString("error.login");
            throw new RaplaSecurityException(loginErrorMessage);
        }
        final LoginTokens loginTokens = tokenHandler.generateAccessToken(user);
        if (loginTokens.isValid())
        {
            return loginTokens;
        }
        final String loginErrorMessage = i18n.getString("error.login");
        throw new RaplaSecurityException(loginErrorMessage);
    }
}
