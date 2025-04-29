package org.tkit.onecx.workspace.api.legacy.rs.v1.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.workspace.api.legacy.domain.config.ApiLegacyConfig;
import org.tkit.onecx.workspace.api.legacy.domain.service.TokenService;
import org.tkit.onecx.workspace.api.legacy.rs.v1.mappers.ExceptionMapper;
import org.tkit.onecx.workspace.api.legacy.rs.v1.mappers.UserMenuMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.workspace.api.rs.legacy.v1.MenuItemApiV1;
import gen.org.tkit.onecx.workspace.api.rs.legacy.v1.model.GetMenuItemsRequestDTOV1;
import gen.org.tkit.onecx.workspace.api.rs.legacy.v1.model.ProblemDetailResponseDTOV1;
import gen.org.tkit.onecx.workspace.user.client.api.UserMenuInternalApi;
import gen.org.tkit.onecx.workspace.user.client.model.UserWorkspaceMenuRequest;
import gen.org.tkit.onecx.workspace.user.client.model.UserWorkspaceMenuStructure;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class MenuItemRestController implements MenuItemApiV1 {

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    UserMenuMapper mapper;

    @Inject
    @RestClient
    UserMenuInternalApi userMenuClient;

    @Inject
    TokenService tokenService;

    @Inject
    ApiLegacyConfig config;

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTOV1> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public Response restException(ClientWebApplicationException ex) {
        return exceptionMapper.clientException(ex);
    }

    @Override
    public Response getMenuItemsAccessToken(GetMenuItemsRequestDTOV1 getMenuItemsRequestDTOV1) {
        UserWorkspaceMenuRequest request = mapper.map(getMenuItemsRequestDTOV1);
        var workspaceName = workspaceName(getMenuItemsRequestDTOV1);
        try (Response response = userMenuClient.getUserMenu(workspaceName, request)) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(UserWorkspaceMenuStructure.class))).build();
        }
    }

    @Override
    public Response getMenuItemsIdToken(GetMenuItemsRequestDTOV1 getMenuItemsRequestDTOV1) {
        UserWorkspaceMenuRequest request = mapper.map(getMenuItemsRequestDTOV1);
        var token = tokenService.convertIdToken(request.getToken());
        request.setToken(token);
        var workspaceName = workspaceName(getMenuItemsRequestDTOV1);
        try (Response response = userMenuClient.getUserMenu(workspaceName, request)) {
            return Response.status(response.getStatus())
                    .entity(mapper.map(response.readEntity(UserWorkspaceMenuStructure.class))).build();
        }
    }

    private String workspaceName(GetMenuItemsRequestDTOV1 dto) {
        var workspace = config.workspace().mapping().get(dto.getWorkspaceName());
        if (workspace != null) {
            return workspace;
        }
        return dto.getWorkspaceName();
    }
}
