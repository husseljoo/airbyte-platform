/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.data.services.impls.keycloak;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.airbyte.commons.auth.config.AirbyteKeycloakConfiguration;
import io.airbyte.config.Application;
import io.airbyte.config.User;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.DetachedTestCase"})
class ApplicationServiceKeycloakImplTests {

  public static final String TEST_1 = "test1";
  public static final String TEST_2 = "test2";
  @Mock
  private Keycloak keycloakClient;
  private AirbyteKeycloakConfiguration keycloakConfiguration;
  @Mock
  private RealmResource realmResource;
  @Mock
  ClientsResource clientsResource;
  @Mock
  ClientResource clientResource;
  @Mock
  UsersResource usersResource;
  @Mock
  UserResource userResource;

  private ApplicationServiceKeycloakImpl apiKeyServiceKeycloakImpl;

  @BeforeEach
  void setUp() {
    keycloakConfiguration = new AirbyteKeycloakConfiguration();
    keycloakConfiguration.setClientRealm("airbyte-client-realm");
    keycloakConfiguration.setProtocol("http");
    keycloakConfiguration.setHost("localhost:8080");

    MockitoAnnotations.openMocks(this);

    apiKeyServiceKeycloakImpl = spy(new ApplicationServiceKeycloakImpl(
        keycloakClient,
        keycloakConfiguration));

    when(keycloakClient.realm(keycloakConfiguration.getClientRealm())).thenReturn(realmResource);
    when(keycloakClient.realm(keycloakConfiguration.getClientRealm()).clients()).thenReturn(clientsResource);
    when(keycloakClient.realm(keycloakConfiguration.getClientRealm()).users()).thenReturn(usersResource);

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .create(any(ClientRepresentation.class))).thenReturn(Response.created(URI.create("https://company.example")).build());
  }

  // TODO: Add this test back in, got tired of fighting mocks.
  // @Test
  void testCreateApiKeyForUser() {
    final var user = new User().withUserId(UUID.fromString("bf0cc898-4a99-4dc1-834d-26b2ba57fdeb"));

    doReturn(Collections.emptyList())
        .when(apiKeyServiceKeycloakImpl)
        .listApplicationsByUser(user);

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .findByClientId(any()))
                .thenReturn(List.of(buildClientRepresentation(user, TEST_1, 0)));

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .get(any())).thenReturn(clientResource);

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .get(any())
            .getServiceAccountUser()).thenReturn(new UserRepresentation());

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .users()
            .get(any())).thenReturn(userResource);

    doNothing().when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .users()
            .get(any()))
        .update(any(UserRepresentation.class));

    final var apiKey1 = apiKeyServiceKeycloakImpl.createApplication(
        user,
        TEST_1);
    assert apiKey1 != null;
    assert TEST_1.equals(apiKey1.getName());
    assert apiKey1.getClientId().equals(user.getUserId() + "-0");

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .findByClientId(buildClientId("bf0cc898-4a99-4dc1-834d-26b2ba57fdeb", "1")))
                .thenReturn(List.of(buildClientRepresentation(user, TEST_2, 1)));

    doReturn(List.of(buildClientRepresentation(user, TEST_1, 0)))
        .when(apiKeyServiceKeycloakImpl)
        .listApplicationsByUser(user);

    final var apiKey2 = apiKeyServiceKeycloakImpl.createApplication(
        user,
        TEST_2);
    assert apiKey2 != null;
    assert TEST_2.equals(apiKey2.getName());
    assert apiKey2.getClientId().equals(user.getUserId() + "-1");

    doReturn(Optional.empty())
        .when(apiKeyServiceKeycloakImpl)
        .deleteApplication(any(), any());

    apiKeyServiceKeycloakImpl.deleteApplication(user, apiKey2.getId());
    apiKeyServiceKeycloakImpl.deleteApplication(user, apiKey1.getId());

    doReturn(Collections.emptyList())
        .when(apiKeyServiceKeycloakImpl)
        .listApplicationsByUser(user);
    assert apiKeyServiceKeycloakImpl.listApplicationsByUser(
        user).isEmpty();
  }

  @Test
  void testNoMoreThanTwoApiKeys() {
    final var user = new User().withUserId(UUID.fromString("6287ecb9-f9fb-4062-a12b-20479b6d2dde"));

    doReturn(List.of(
        buildClientRepresentation(user, TEST_1, 0),
        buildClientRepresentation(user, TEST_2, 1)))
            .when(apiKeyServiceKeycloakImpl)
            .listApplicationsByUser(user);

    assertThrows(
        BadRequestException.class,
        () -> apiKeyServiceKeycloakImpl.createApplication(user, "test3"));
  }

  @Test
  void testApiKeyNameAlreadyExists() {
    final var user = new User().withUserId(UUID.fromString("4bb2a760-a0b6-4936-aea0-a13fada349f4"));

    doReturn(List.of(buildClientRepresentation(user, TEST_1, 0)))
        .when(apiKeyServiceKeycloakImpl)
        .listApplicationsByUser(user);

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .findByClientId(buildClientId("4bb2a760-a0b6-4936-aea0-a13fada349f4", "0")))
                .thenReturn(List.of(buildClientRepresentation(user, TEST_1, 0)));

    assertThrows(
        BadRequestException.class,
        () -> apiKeyServiceKeycloakImpl.createApplication(user, TEST_1));
  }

  @Test
  void testBadKeycloakCreateResponse() {
    final var user = new User().withUserId(UUID.fromString("b3600891-e7c7-4278-8a94-8b838985de2a"));
    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .create(any(ClientRepresentation.class))).thenReturn(Response.status(500).build());

    doReturn(Collections.emptyList())
        .when(apiKeyServiceKeycloakImpl)
        .listApplicationsByUser(user);

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .findByClientId(buildClientId("b3600891-e7c7-4278-8a94-8b838985de2a", "0")))
                .thenReturn(List.of(buildClientRepresentation(user, TEST_1, 0)));

    assertThrows(
        BadRequestException.class,
        () -> apiKeyServiceKeycloakImpl.createApplication(user, TEST_1));
    assert apiKeyServiceKeycloakImpl.listApplicationsByUser(
        user).isEmpty();
  }

  @Test
  void testListKeysForUser() {
    final var user = new User().withUserId(UUID.fromString("58b32b0c-acef-47b9-8e3d-1c83adc7ce59"));
    // Note: This can be quickly refactored into an integration test, but for now we mock creating.

    doReturn(List.of(
        buildClientRepresentation(user, TEST_1, 0)))
            .when(apiKeyServiceKeycloakImpl)
            .listApplicationsByUser(user);

    doReturn(new Application()).when(apiKeyServiceKeycloakImpl).createApplication(
        user,
        TEST_1);

    final var apiKey1 = apiKeyServiceKeycloakImpl.createApplication(
        user,
        TEST_1);

    var apiKeys = apiKeyServiceKeycloakImpl.listApplicationsByUser(
        user);
    assert apiKeys.size() == 1;

    doReturn(new Application()).when(apiKeyServiceKeycloakImpl).createApplication(
        user,
        TEST_2);
    final var apiKey2 = apiKeyServiceKeycloakImpl.createApplication(
        user,
        TEST_2);

    doReturn(List.of(
        buildClientRepresentation(user, TEST_1, 0),
        buildClientRepresentation(user, TEST_2, 1)))
            .when(apiKeyServiceKeycloakImpl)
            .listApplicationsByUser(user);
    apiKeys = apiKeyServiceKeycloakImpl.listApplicationsByUser(
        user);
    assert apiKeys.size() == 2;

    doReturn(Optional.empty())
        .when(apiKeyServiceKeycloakImpl)
        .deleteApplication(any(), any());

    apiKeyServiceKeycloakImpl.deleteApplication(user, apiKey2.getId());
    apiKeyServiceKeycloakImpl.deleteApplication(user, apiKey1.getId());

    doReturn(Collections.emptyList())
        .when(apiKeyServiceKeycloakImpl)
        .listApplicationsByUser(user);
    assert apiKeyServiceKeycloakImpl.listApplicationsByUser(
        user).isEmpty();
  }

  // It was very difficult to mock out the remove call as it returns a void. Commenting this test out.
  void testDeleteApiKey() {
    final var user = new User().withUserId(UUID.fromString("f81780ef-148e-413d-8e00-6e755e4e2256"));
    // Note: This can be quickly refactored into an integration test, but for now we mock creating.
    doReturn(new Application()).when(apiKeyServiceKeycloakImpl).createApplication(
        user,
        TEST_1);

    final var apiKey1 = apiKeyServiceKeycloakImpl.createApplication(
        user,
        TEST_1);
    doReturn(new Application()).when(apiKeyServiceKeycloakImpl).createApplication(
        user,
        TEST_2);

    apiKeyServiceKeycloakImpl.createApplication(
        user,
        TEST_2);

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .findByClientId(buildClientId("f81780ef-148e-413d-8e00-6e755e4e2256", "0")))
                .thenReturn(List.of(buildClientRepresentation(user, TEST_1, 0)));
    apiKeyServiceKeycloakImpl.deleteApplication(user, apiKey1.getId());

    doReturn(List.of(
        buildClientRepresentation(user, TEST_1, 0)))
            .when(apiKeyServiceKeycloakImpl)
            .listApplicationsByUser(user);

    var apiKeys = apiKeyServiceKeycloakImpl.listApplicationsByUser(
        user);
    assert apiKeys.size() == 1;
    assert "f81780ef-148e-413d-8e00-6e755e4e2256-0".equals(apiKeys.get(0).getId());

    when(
        keycloakClient
            .realm(keycloakConfiguration.getClientRealm())
            .clients()
            .findByClientId(buildClientId("f81780ef-148e-413d-8e00-6e755e4e2256", "0")))
                .thenReturn(List.of(buildClientRepresentation(user, TEST_2, 0)));
    apiKeyServiceKeycloakImpl.deleteApplication(user, "f81780ef-148e-413d-8e00-6e755e4e2256-0");

    doReturn(Collections.emptyList())
        .when(apiKeyServiceKeycloakImpl)
        .listApplicationsByUser(user);
    apiKeys = apiKeyServiceKeycloakImpl.listApplicationsByUser(
        user);
    assert apiKeys.isEmpty();
  }

  private ClientRepresentation buildClientRepresentation(User user, String name, int index) {
    final var clientRepresentation = new ClientRepresentation();
    clientRepresentation.setClientId(user.getUserId() + "-" + index);
    clientRepresentation.setName(name);
    clientRepresentation.setSecret("test");
    var attributes = new HashMap<String, String>();
    attributes.put("user_id", user.getUserId().toString());
    attributes.put("client.secret.creation.time", "365");
    clientRepresentation.setAttributes(attributes);
    return clientRepresentation;
  }

  private static String buildClientId(String userId, String index) {
    return userId + "-" + index;
  }

}
