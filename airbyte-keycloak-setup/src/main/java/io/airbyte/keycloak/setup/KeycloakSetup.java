/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.keycloak.setup;

import io.airbyte.commons.auth.config.AirbyteKeycloakConfiguration;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for setting up the Keycloak server. It initializes and configures the
 * server according to the provided specifications.
 */
@Singleton
@Slf4j
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class KeycloakSetup {

  private final HttpClient httpClient;
  private final KeycloakServer keycloakServer;
  private final AirbyteKeycloakConfiguration keycloakConfiguration;

  public KeycloakSetup(
                       final HttpClient httpClient,
                       final KeycloakServer keycloakServer,
                       final AirbyteKeycloakConfiguration keycloakConfiguration) {
    this.httpClient = httpClient;
    this.keycloakServer = keycloakServer;
    this.keycloakConfiguration = keycloakConfiguration;
  }

  public void run() {
    try {
      final String keycloakUrl = keycloakServer.getKeycloakServerUrl();
      final HttpResponse<String> response = httpClient.toBlocking()
          .exchange(HttpRequest.GET(keycloakUrl), String.class);

      log.info("Keycloak server response: {}", response.getStatus());
      log.info("Starting admin Keycloak client with url: {}", keycloakUrl);

      if (keycloakConfiguration.getResetRealm()) {
        keycloakServer.recreateAirbyteRealm();
      } else {
        keycloakServer.createAirbyteRealm();
      }
    } finally {
      keycloakServer.closeKeycloakAdminClient();
    }
  }

}
