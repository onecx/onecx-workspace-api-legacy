package org.tkit.onecx.workspace.api.legacy.rs.v1;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.workspace.api.legacy.AbstractTest;
import org.tkit.onecx.workspace.api.legacy.domain.config.ApiLegacyConfig;
import org.tkit.onecx.workspace.api.legacy.rs.v1.controllers.MenuItemRestController;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;

import gen.org.tkit.onecx.workspace.api.rs.legacy.v1.model.GetMenuItemsRequestDTOV1;
import gen.org.tkit.onecx.workspace.api.rs.legacy.v1.model.UserWorkspaceMenuStructureDTOV1;
import gen.org.tkit.onecx.workspace.user.client.model.UserWorkspaceMenuItem;
import gen.org.tkit.onecx.workspace.user.client.model.UserWorkspaceMenuRequest;
import gen.org.tkit.onecx.workspace.user.client.model.UserWorkspaceMenuStructure;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.Mock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.config.SmallRyeConfig;

@QuarkusTest
@TestHTTPEndpoint(MenuItemRestController.class)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ws-legacy:read" })
class UserMenuRestControllerConfigTest extends AbstractTest {

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @InjectMock
    ApiLegacyConfig pathConfig;

    @Inject
    Config config;

    public static class ConfigProducer {

        @Inject
        Config config;

        @Produces
        @ApplicationScoped
        @Mock
        ApiLegacyConfig config() {
            return config.unwrap(SmallRyeConfig.class).getConfigMapping(ApiLegacyConfig.class);
        }
    }

    @BeforeEach
    void beforeEach() {
        var tmp = config.unwrap(SmallRyeConfig.class).getConfigMapping(ApiLegacyConfig.class);
        Mockito.when(pathConfig.token()).thenReturn(tmp.token());
        Mockito.when(pathConfig.workspaces()).thenReturn(tmp.workspaces());
        Mockito.when(pathConfig.shellMapping()).thenReturn(new ApiLegacyConfig.ShellMappingConfig() {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public String prefix() {
                return tmp.shellMapping().prefix();
            }
        });
    }

    @Test
    void getUserMenuWithModifiedPrefixTest() {
        final String accessToken = keycloakClient.getAccessToken(ADMIN);
        String workspaceName = "testWorkspace";

        UserWorkspaceMenuRequest request = new UserWorkspaceMenuRequest().token(accessToken).menuKeys(List.of("main-menu"));

        UserWorkspaceMenuItem menuItemInternal = new UserWorkspaceMenuItem().key("MAIN_MENU_INTERNAL").name("mainMenuInternal")
                .position(1).url("/menuItem1").external(false);
        UserWorkspaceMenuItem menuItemExternal = new UserWorkspaceMenuItem().key("MAIN_MENU_EXTERNAL").name("mainMenuExternal")
                .position(1).url("/menuItem2").external(true);
        UserWorkspaceMenuStructure response = new UserWorkspaceMenuStructure().workspaceName(workspaceName)
                .menu(List.of(menuItemInternal, menuItemExternal));

        // create mock rest endpoint
        mockServerClient
                .when(request().withPath("/internal/user/" + workspaceName + "/menu")
                        .withBody(JsonBody.json(request))
                        .withMethod(HttpMethod.POST))
                .withId("mock")
                .respond(httpRequest -> response().withStatusCode(Response.Status.OK.getStatusCode())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(JsonBody.json(response)));

        GetMenuItemsRequestDTOV1 requestDTO = new GetMenuItemsRequestDTOV1()
                .workspaceName(workspaceName).token(accessToken).menuKeys(List.of("main-menu"));
        var output = given()
                .when()
                .auth().oauth2(keycloakClient.getClientAccessToken("testClient"))
                .header(APM_HEADER_PARAM, keycloakClient.getAccessToken(USER))
                .contentType(APPLICATION_JSON)
                .body(requestDTO)
                .post("accessToken")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .contentType(APPLICATION_JSON)
                .extract().as(UserWorkspaceMenuStructureDTOV1.class);

        Assertions.assertEquals("customPrefix/menuItem1", output.getMenu().get(0).getUrl());
        Assertions.assertEquals("/menuItem2", output.getMenu().get(1).getUrl());
        mockServerClient.clear("mock");
    }

}
