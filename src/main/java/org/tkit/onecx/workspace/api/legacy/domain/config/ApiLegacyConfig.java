package org.tkit.onecx.workspace.api.legacy.domain.config;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Shell path mapping configuration
 */
@ConfigDocFilename("onecx-workspace-api-legacy.adoc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "onecx.workspace.api.legacy")
public interface ApiLegacyConfig {

    /**
     * Token configuration
     */
    @WithName("token")
    TokenConfig token();

    /**
     * Token configuration
     */
    interface TokenConfig {

        /**
         * access token roles path
         */
        @WithName("access.roles.path")
        @WithDefault("realm_access/roles")
        String accessTokenRolesPath();

        /**
         * access token roles path
         */
        @WithName("id.roles")
        @WithDefault("realm_roles")
        String idTokenRoles();

    }

    /**
     * path mapping configuration
     */
    @WithName("shell-mapping")
    ShellMappingConfig shellMapping();

    interface ShellMappingConfig {

        /**
         * Enable or disable shell mapping
         */
        @WithDefault("false")
        @WithName("enabled")
        boolean enabled();

        /**
         * Prefix to be used for paths
         */
        @WithDefault("ui/")
        @WithName("prefix")
        String prefix();
    }
}
