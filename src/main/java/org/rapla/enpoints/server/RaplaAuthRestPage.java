package org.rapla.enpoints.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.rapla.ConnectInfo;
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

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

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
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML})
    @Operation(
        summary = "Login with credentials",
        description = "Authenticate user with username and password. Supports two flows:\n\n" +
                     "1. **JSON API Login**: Send JSON with username/password in body, returns JWT token\n\n" +
                     "2. **Form-based Web Login**: Send form parameters (username, password, url), returns HTML page with token in URL fragment and cookie\n\n" +
                     "Content negotiation via Accept header: application/json → JSON response, text/plain → token string only"
    )
    @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = LoginTokens.class)))
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public Response login(
            @RequestBody(description = "Login credentials (JSON body)", required = false, content = @Content(schema = @Schema(implementation = LoginCredentials.class))) LoginCredentials credentials,
            @Parameter(description = "Username (form parameter)") @FormParam("username") String user,
            @Parameter(description = "Password (form parameter)") @FormParam("password") String password,
            @Parameter(description = "Connect as (form parameter)") @FormParam("connectAs") String connectAs,
            @Parameter(description = "Target URL to redirect to after login (form parameter)") @QueryParam("url") String url,
            @Context HttpHeaders headers,
            @Context HttpServletResponse response) throws Exception
    {
        // Determine which flow to use
        if (credentials != null && (user == null && password == null)) {
            // JSON API flow
            final LoginTokens tokens = create(credentials);
            System.out.println("JSON API login successful: " + tokens.getAccessToken());
            // Check Accept header for content negotiation
            String acceptHeader = headers.getHeaderString("Accept");
            if (acceptHeader != null && acceptHeader.contains(MediaType.TEXT_PLAIN)) {
                return Response.ok(tokens.getAccessToken(), MediaType.TEXT_PLAIN).build();
            }
            
            // Default: return full LoginTokens object
            return Response.ok(tokens).build();
        } else if (user != null && password != null) {
            // Form-based web flow
            System.out.println("Form-based web login initiated for user: " + user);
            return loginFormBased(url, user, password, connectAs, response);
        } else {
            throw new RaplaSecurityException("Invalid login request: provide either JSON body or form parameters");
        }
    }

    private Response loginFormBased(String url, String user, String password, String connectAs, HttpServletResponse response) throws Exception
    {
        final String targetUrl = url != null ? Tools.createXssSafeString(url) : "../apiTest.html";
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
                final LoginTokens token = create(new LoginCredentials(user, password, connectAs));
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
                return Response.ok().build();
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
        
        response.setContentType("text/html");
        createPage(url, user, errorMessage, response);
        return Response.ok().build();
    }

    @GET
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

    private LoginTokens create(LoginCredentials credentials) throws Exception
    {
        return dummy(credentials);
    }

}
