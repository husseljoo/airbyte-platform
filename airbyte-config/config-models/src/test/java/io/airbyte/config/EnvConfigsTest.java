/*
 * Copyright (c) 2020-2024 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.airbyte.commons.envvar.EnvVar;
import io.airbyte.commons.version.AirbyteVersion;
import io.airbyte.config.Configs.DeploymentMode;
import io.airbyte.config.Configs.WorkerEnvironment;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.NullAssignment")
class EnvConfigsTest {

  private Map<String, String> envMap;
  private EnvConfigs config;
  private static final String ABC = "abc";
  private static final String DEV = "dev";
  private static final String ABCDEF = "abc/def";
  private static final String ROOT = "root";
  private static final String KEY = "key";
  private static final String ONE = "one";
  private static final String TWO = "two";
  private static final String ONE_EQ_TWO = "one=two";
  private static final String NOTHING = "nothing";
  private static final String SOMETHING = "something";
  private static final String AIRBYTE = "airbyte";
  private static final String SERVER = "server";
  private static final String AIRB_SERV_SOME_NOTHING = "airbyte=server,something=nothing";
  private static final String ENV_STRING = "key=k,,;$%&^#";
  private static final String NODE_SELECTORS = ",,,";

  @BeforeEach
  void setUp() {
    envMap = new HashMap<>();
    config = new EnvConfigs(envMap);
  }

  @Test
  void ensureGetEnvBehavior() {
    assertNull(System.getenv("MY_RANDOM_VAR_1234"));
  }

  @Test
  void testAirbyteRole() {
    envMap.put(EnvVar.AIRBYTE_ROLE.name(), null);
    assertNull(config.getAirbyteRole());

    envMap.put(EnvVar.AIRBYTE_ROLE.name(), DEV);
    assertEquals(DEV, config.getAirbyteRole());
  }

  @Test
  void testAirbyteVersion() {
    envMap.put(EnvVar.AIRBYTE_VERSION.name(), null);
    assertThrows(IllegalArgumentException.class, () -> config.getAirbyteVersion());

    envMap.put(EnvVar.AIRBYTE_VERSION.name(), DEV);
    assertEquals(new AirbyteVersion(DEV), config.getAirbyteVersion());
  }

  @Test
  void testWorkspaceRoot() {
    envMap.put(EnvVar.WORKSPACE_ROOT.name(), null);
    assertThrows(IllegalArgumentException.class, () -> config.getWorkspaceRoot());

    envMap.put(EnvVar.WORKSPACE_ROOT.name(), ABCDEF);
    assertEquals(Paths.get(ABCDEF), config.getWorkspaceRoot());
  }

  @Test
  void testLocalRoot() {
    envMap.put(EnvVar.LOCAL_ROOT.name(), null);
    assertThrows(IllegalArgumentException.class, () -> config.getLocalRoot());

    envMap.put(EnvVar.LOCAL_ROOT.name(), ABCDEF);
    assertEquals(Paths.get(ABCDEF), config.getLocalRoot());
  }

  @Test
  void testConfigRoot() {
    envMap.put(EnvVar.CONFIG_ROOT.name(), null);
    assertThrows(IllegalArgumentException.class, () -> config.getConfigRoot());

    envMap.put(EnvVar.CONFIG_ROOT.name(), "a/b");
    assertEquals(Paths.get("a/b"), config.getConfigRoot());
  }

  @Test
  void testGetDatabaseUser() {
    envMap.put(EnvVar.DATABASE_USER.name(), null);
    assertThrows(IllegalArgumentException.class, () -> config.getDatabaseUser());

    envMap.put(EnvVar.DATABASE_USER.name(), "user");
    assertEquals("user", config.getDatabaseUser());
  }

  @Test
  void testGetDatabasePassword() {
    envMap.put(EnvVar.DATABASE_PASSWORD.name(), null);
    assertThrows(IllegalArgumentException.class, () -> config.getDatabasePassword());

    envMap.put(EnvVar.DATABASE_PASSWORD.name(), "password");
    assertEquals("password", config.getDatabasePassword());
  }

  @Test
  void testGetDatabaseUrl() {
    envMap.put(EnvVar.DATABASE_URL.name(), null);
    assertThrows(IllegalArgumentException.class, () -> config.getDatabaseUrl());

    envMap.put(EnvVar.DATABASE_URL.name(), "url");
    assertEquals("url", config.getDatabaseUrl());
  }

  @Test
  void testGetWorkspaceDockerMount() {
    envMap.put(EnvVar.WORKSPACE_DOCKER_MOUNT.name(), null);
    envMap.put(EnvVar.WORKSPACE_ROOT.name(), ABCDEF);
    assertEquals(ABCDEF, config.getWorkspaceDockerMount());

    envMap.put(EnvVar.WORKSPACE_DOCKER_MOUNT.name(), ROOT);
    envMap.put(EnvVar.WORKSPACE_ROOT.name(), ABCDEF);
    assertEquals(ROOT, config.getWorkspaceDockerMount());

    envMap.put(EnvVar.WORKSPACE_DOCKER_MOUNT.name(), null);
    envMap.put(EnvVar.WORKSPACE_ROOT.name(), null);
    assertThrows(IllegalArgumentException.class, () -> config.getWorkspaceDockerMount());
  }

  @Test
  void testGetLocalDockerMount() {
    envMap.put(EnvVar.LOCAL_DOCKER_MOUNT.name(), null);
    envMap.put(EnvVar.LOCAL_ROOT.name(), ABCDEF);
    assertEquals(ABCDEF, config.getLocalDockerMount());

    envMap.put(EnvVar.LOCAL_DOCKER_MOUNT.name(), ROOT);
    envMap.put(EnvVar.LOCAL_ROOT.name(), ABCDEF);
    assertEquals(ROOT, config.getLocalDockerMount());

    envMap.put(EnvVar.LOCAL_DOCKER_MOUNT.name(), null);
    envMap.put(EnvVar.LOCAL_ROOT.name(), null);
    assertThrows(IllegalArgumentException.class, () -> config.getLocalDockerMount());
  }

  @Test
  void testDockerNetwork() {
    envMap.put(EnvVar.DOCKER_NETWORK.name(), null);
    assertEquals("host", config.getDockerNetwork());

    envMap.put(EnvVar.DOCKER_NETWORK.name(), ABC);
    assertEquals(ABC, config.getDockerNetwork());
  }

  @Test
  void testDeploymentMode() {
    envMap.put(EnvVar.DEPLOYMENT_MODE.name(), null);
    assertEquals(Configs.DeploymentMode.OSS, config.getDeploymentMode());

    envMap.put(EnvVar.DEPLOYMENT_MODE.name(), "CLOUD");
    assertEquals(Configs.DeploymentMode.CLOUD, config.getDeploymentMode());

    envMap.put(EnvVar.DEPLOYMENT_MODE.name(), "oss");
    assertEquals(Configs.DeploymentMode.OSS, config.getDeploymentMode());

    envMap.put(EnvVar.DEPLOYMENT_MODE.name(), "OSS");
    assertEquals(Configs.DeploymentMode.OSS, config.getDeploymentMode());
  }

  @Test
  void testworkerKubeTolerations() {
    final String airbyteServer = "airbyte-server";
    final String noSchedule = "NoSchedule";

    envMap.put(EnvVar.JOB_KUBE_TOLERATIONS.name(), null);
    assertEquals(config.getJobKubeTolerations(), List.of());

    envMap.put(EnvVar.JOB_KUBE_TOLERATIONS.name(), ";;;");
    assertEquals(config.getJobKubeTolerations(), List.of());

    envMap.put(EnvVar.JOB_KUBE_TOLERATIONS.name(), "key=k,value=v;");
    assertEquals(config.getJobKubeTolerations(), List.of());

    envMap.put(EnvVar.JOB_KUBE_TOLERATIONS.name(), "key=airbyte-server,operator=Exists,effect=NoSchedule");
    assertEquals(config.getJobKubeTolerations(), List.of(new TolerationPOJO(airbyteServer, noSchedule, null, "Exists")));

    envMap.put(EnvVar.JOB_KUBE_TOLERATIONS.name(), "key=airbyte-server,operator=Equals,value=true,effect=NoSchedule");
    assertEquals(config.getJobKubeTolerations(), List.of(new TolerationPOJO(airbyteServer, noSchedule, "true", "Equals")));

    envMap.put(EnvVar.JOB_KUBE_TOLERATIONS.name(),
        "key=airbyte-server,operator=Exists,effect=NoSchedule;key=airbyte-server,operator=Equals,value=true,effect=NoSchedule");
    assertEquals(config.getJobKubeTolerations(), List.of(
        new TolerationPOJO(airbyteServer, noSchedule, null, "Exists"),
        new TolerationPOJO(airbyteServer, noSchedule, "true", "Equals")));
  }

  @Test
  void testSplitKVPairsFromEnvString() {
    String input = "key1=value1,key2=value2";
    Map<String, String> map = config.splitKVPairsFromEnvString(input);
    assertNotNull(map);
    assertEquals(2, map.size());
    assertEquals(map, Map.of("key1", "value1", "key2", "value2"));

    input = ENV_STRING;
    map = config.splitKVPairsFromEnvString(input);
    assertNotNull(map);
    assertEquals(map, Map.of(KEY, "k"));

    input = null;
    map = config.splitKVPairsFromEnvString(input);
    assertNull(map);

    input = " key1= value1,  key2 =    value2";
    map = config.splitKVPairsFromEnvString(input);
    assertNotNull(map);
    assertEquals(map, Map.of("key1", "value1", "key2", "value2"));

    input = "key1:value1,key2:value2";
    map = config.splitKVPairsFromEnvString(input);
    assertNull(map);
  }

  @Test
  void testJobKubeNodeSelectors() {
    envMap.put(EnvVar.JOB_KUBE_NODE_SELECTORS.name(), null);
    assertNull(config.getJobKubeNodeSelectors());

    envMap.put(EnvVar.JOB_KUBE_NODE_SELECTORS.name(), NODE_SELECTORS);
    assertNull(config.getJobKubeNodeSelectors());

    envMap.put(EnvVar.JOB_KUBE_NODE_SELECTORS.name(), ENV_STRING);
    assertEquals(config.getJobKubeNodeSelectors(), Map.of(KEY, "k"));

    envMap.put(EnvVar.JOB_KUBE_NODE_SELECTORS.name(), ONE_EQ_TWO);
    assertEquals(config.getJobKubeNodeSelectors(), Map.of(ONE, TWO));

    envMap.put(EnvVar.JOB_KUBE_NODE_SELECTORS.name(), AIRB_SERV_SOME_NOTHING);
    assertEquals(config.getJobKubeNodeSelectors(), Map.of(AIRBYTE, SERVER, SOMETHING, NOTHING));
  }

  @Test
  void testPublishMetrics() {
    envMap.put(EnvVar.PUBLISH_METRICS.name(), "true");
    assertTrue(config.getPublishMetrics());

    envMap.put(EnvVar.PUBLISH_METRICS.name(), "false");
    assertFalse(config.getPublishMetrics());

    envMap.put(EnvVar.PUBLISH_METRICS.name(), null);
    assertFalse(config.getPublishMetrics());

    envMap.put(EnvVar.PUBLISH_METRICS.name(), "");
    assertFalse(config.getPublishMetrics());
  }

  @Test
  @DisplayName("Should parse constant tags")
  void testDDConstantTags() {
    assertEquals(List.of(), config.getDDConstantTags());

    envMap.put(EnvVar.DD_CONSTANT_TAGS.name(), " ");
    assertEquals(List.of(), config.getDDConstantTags());

    envMap.put(EnvVar.DD_CONSTANT_TAGS.name(), "airbyte_instance:dev,k8s-cluster:eks-dev");
    final List<String> expected = List.of("airbyte_instance:dev", "k8s-cluster:eks-dev");
    assertEquals(expected, config.getDDConstantTags());
    assertEquals(2, config.getDDConstantTags().size());
  }

  @Test
  void testSharedJobEnvMapRetrieval() {
    envMap.put(EnvVar.AIRBYTE_VERSION.name(), DEV);
    envMap.put(EnvVar.WORKER_ENVIRONMENT.name(), WorkerEnvironment.KUBERNETES.name());
    final Map<String, String> expected = Map.of("AIRBYTE_VERSION", DEV,
        "AIRBYTE_ROLE", "",
        "DEPLOYMENT_MODE", "OSS",
        "WORKER_ENVIRONMENT", "KUBERNETES");
    assertEquals(expected, config.getJobDefaultEnvMap());
  }

  @Test
  void testAllJobEnvMapRetrieval() {
    envMap.put(EnvVar.AIRBYTE_VERSION.name(), DEV);
    envMap.put(EnvVar.AIRBYTE_ROLE.name(), "UNIT_TEST");
    envMap.put(EnvVar.JOB_DEFAULT_ENV_.name() + "ENV1", "VAL1");
    envMap.put(EnvVar.JOB_DEFAULT_ENV_.name() + "ENV2", "VAL\"2WithQuotesand$ymbols");
    envMap.put(EnvVar.DEPLOYMENT_MODE.name(), DeploymentMode.CLOUD.name());

    final Map<String, String> expected = Map.of("ENV1", "VAL1",
        "ENV2", "VAL\"2WithQuotesand$ymbols",
        "AIRBYTE_VERSION", DEV,
        "AIRBYTE_ROLE", "UNIT_TEST",
        "DEPLOYMENT_MODE", "CLOUD",
        "WORKER_ENVIRONMENT", "DOCKER");
    assertEquals(expected, config.getJobDefaultEnvMap());
  }

}
