package org.tkit.onecx.workspace.api.legacy.rs.v1;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.tkit.quarkus.security.test.SecurityTestUtils.getKeycloakClientToken;

import java.util.List;
import java.util.Map;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwx.JsonWebStructure;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tkit.onecx.workspace.api.legacy.AbstractTest;
import org.tkit.onecx.workspace.api.legacy.rs.v1.controllers.MenuItemRestController;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import gen.org.tkit.onecx.workspace.api.rs.legacy.v1.model.GetMenuItemsRequestDTOV1;
import gen.org.tkit.onecx.workspace.api.rs.legacy.v1.model.ProblemDetailResponseDTOV1;
import gen.org.tkit.onecx.workspace.api.rs.legacy.v1.model.UserWorkspaceMenuStructureDTOV1;
import gen.org.tkit.onecx.workspace.user.client.model.UserWorkspaceMenuItem;
import gen.org.tkit.onecx.workspace.user.client.model.UserWorkspaceMenuRequest;
import gen.org.tkit.onecx.workspace.user.client.model.UserWorkspaceMenuStructure;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(MenuItemRestController.class)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ws-legacy:read" })
class UserMenuRestControllerTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(UserMenuRestControllerTest.class);
    @InjectMockServerClient
    MockServerClient mockServerClient;

    private static final ObjectReader OBJECT_READER = new ObjectMapper().readerFor(UserWorkspaceMenuRequest.class);

    @Test
    void getUserMenuAccessTokenTest() {

        final String accessToken = keycloakClient.getAccessToken(ADMIN);
        String workspaceName = "testWorkspace";

        UserWorkspaceMenuItem child = new UserWorkspaceMenuItem().name("mainMenuChild").key("MAIN_MENU_CHILD").position(2)
                .url("/child");
        UserWorkspaceMenuItem menuItem = new UserWorkspaceMenuItem().key("MAIN_MENU").name("mainMenu").position(1)
                .url("/menuItem1").children(List.of(child));
        UserWorkspaceMenuStructure response = new UserWorkspaceMenuStructure().workspaceName(workspaceName)
                .menu(List.of(menuItem));

        // create mock rest endpoint
        mockServerClient
                .when(request().withPath("/internal/user/" + workspaceName + "/menu")
                        .withBody(JsonBody.json(new UserWorkspaceMenuRequest().token(accessToken)
                                .menuKeys(List.of("main-menu"))))
                        .withMethod(HttpMethod.POST))
                .withId("mock")
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(response)));

        GetMenuItemsRequestDTOV1 requestDTO = new GetMenuItemsRequestDTOV1()
                .workspaceName(workspaceName).token(accessToken).menuKeys(List.of("main-menu"));

        var output = given()
                .when()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .header(APM_HEADER_PARAM, keycloakClient.getAccessToken(USER))
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("accessToken")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(UserWorkspaceMenuStructureDTOV1.class);

        Assertions.assertEquals(output.getMenu().size(), response.getMenu().size());

        mockServerClient.clear("mock");
    }

    @Test
    void getUserMenuAccessTokenWrongWorkspaceIdTest() {
        final String accessToken = keycloakClient.getAccessToken(ADMIN);
        String workspaceName = "notFound";

        // create mock rest endpoint
        mockServerClient
                .when(request().withPath("/internal/user/" + workspaceName + "/menu")
                        .withBody(
                                JsonBody.json(new UserWorkspaceMenuRequest().token(accessToken).menuKeys(List.of("main-menu"))))
                        .withMethod(HttpMethod.POST))
                .withId("mock")
                .respond(httpRequest -> response().withStatusCode(Response.Status.NOT_FOUND.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON));

        GetMenuItemsRequestDTOV1 requestDTO = new GetMenuItemsRequestDTOV1().token(accessToken).workspaceName(workspaceName)
                .menuKeys(List.of("main-menu"));
        given()
                .when()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .header(APM_HEADER_PARAM, keycloakClient.getAccessToken(USER))
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("accessToken")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());

        mockServerClient.clear("mock");
    }

    @Test
    void getUserMenuAccessTokenMissingWorkspaceNameTest() {
        GetMenuItemsRequestDTOV1 requestDTO = new GetMenuItemsRequestDTOV1();
        requestDTO.menuKeys(List.of("main-menu"));
        var output = given()
                .when()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .header(APM_HEADER_PARAM, keycloakClient.getAccessToken(USER))
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("accessToken")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(ProblemDetailResponseDTOV1.class);

        Assertions.assertNotNull(output);
    }

    @Test
    void getUserMenuIdTokenTest() {

        final String idToken = createIdToken("user", List.of("role1", "role2"));
        String workspaceName = "testWorkspace";

        UserWorkspaceMenuItem child = new UserWorkspaceMenuItem().name("mainMenuChild").key("MAIN_MENU_CHILD").position(2)
                .url("/child");
        UserWorkspaceMenuItem menuItem = new UserWorkspaceMenuItem().key("MAIN_MENU").name("mainMenu").position(1)
                .url("/menuItem1").children(List.of(child));
        UserWorkspaceMenuStructure response = new UserWorkspaceMenuStructure().workspaceName(workspaceName)
                .menu(List.of(menuItem));

        // create mock rest endpoint
        mockServerClient
                .when(request().withPath("/internal/user/" + workspaceName + "/menu")
                        .withMethod(HttpMethod.POST))
                .withId("mock")
                .respond(httpRequest -> {

                    var result = response().withContentType(MediaType.APPLICATION_JSON);

                    var check = validateHttpRequest(httpRequest);
                    if (check) {
                        result.withStatusCode(Response.Status.OK.getStatusCode())
                                .withBody(JsonBody.json(response));
                    } else {
                        result.withStatusCode(Response.Status.NOT_FOUND.getStatusCode());
                    }
                    return result;
                });

        GetMenuItemsRequestDTOV1 requestDTO = new GetMenuItemsRequestDTOV1()
                .workspaceName(workspaceName).token(idToken).menuKeys(List.of("main-menu"));

        var output = given()
                .when()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .header(APM_HEADER_PARAM, keycloakClient.getAccessToken(USER))
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("idToken")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(UserWorkspaceMenuStructureDTOV1.class);

        Assertions.assertEquals(output.getMenu().size(), response.getMenu().size());

        mockServerClient.clear("mock");
    }

    private boolean validateHttpRequest(HttpRequest httpRequest) {
        try {
            UserWorkspaceMenuRequest menuRequest = OBJECT_READER.readValue(httpRequest.getBodyAsJsonOrXmlString());
            var jws = (JsonWebSignature) JsonWebStructure.fromCompactSerialization(menuRequest.getToken());
            var jwtClaims = JwtClaims.parse(jws.getUnverifiedPayload());

            var realmRoles = (Map<String, Object>) jwtClaims.getClaimsMap().get("realm_access");
            var roles = realmRoles.get("roles");

            Assertions.assertNotNull(roles);
            Assertions.assertInstanceOf(List.class, roles);
            var list = (List<?>) roles;
            Assertions.assertEquals(2, list.size());
            Assertions.assertTrue(list.contains("role1"));
            Assertions.assertTrue(list.contains("role2"));
        } catch (Exception e) {
            log.error("Error validate request", e);
            return false;
        }
        return true;
    }

    @Test
    void getUserMenuAccessNoTokenTest() {

        String workspaceName = "testWorkspace";

        GetMenuItemsRequestDTOV1 requestDTO = new GetMenuItemsRequestDTOV1()
                .workspaceName(workspaceName).menuKeys(List.of("main-menu"));

        given()
                .when()
                .auth().oauth2(getKeycloakClientToken("testClient"))
                .header(APM_HEADER_PARAM, keycloakClient.getAccessToken(USER))
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("accessToken")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .contentType(APPLICATION_JSON);

        mockServerClient.clear("mock");
    }
}
