/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.server.apis.publicapi.mappers

import io.airbyte.api.model.generated.WorkspaceRead
import io.airbyte.api.model.generated.WorkspaceReadList
import io.airbyte.public_api.model.generated.WorkspacesResponse
import io.airbyte.server.apis.publicapi.constants.INCLUDE_DELETED
import io.airbyte.server.apis.publicapi.constants.WORKSPACES_PATH
import io.airbyte.server.apis.publicapi.constants.WORKSPACE_IDS
import io.airbyte.server.apis.publicapi.helpers.removePublicApiPathPrefix
import io.airbyte.server.apis.publicapi.mappers.WorkspaceResponseMapper.from
import java.util.UUID

/**
 * Maps config API WorkspaceReadList to WorkspacesResponse.
 */
object WorkspacesResponseMapper {
  /**
   * Converts a WorkspaceReadList object from the config api to a WorkspacesResponse object.
   *
   * @param workspaceReadList Output of a workspace list from config api
   * @param workspaceIds workspaceIds we wanted to list
   * @param includeDeleted did we include deleted workspaces or not?
   * @param limit Number of responses to be outputted
   * @param offset Offset of the pagination
   * @param apiHost Host url e.g. api.airbyte.com
   * @return SourcesResponse List of SourceResponse along with a next and previous https requests
   */
  fun from(
    workspaceReadList: WorkspaceReadList,
    workspaceIds: List<UUID>,
    includeDeleted: Boolean,
    limit: Int,
    offset: Int,
    apiHost: String,
  ): WorkspacesResponse {
    val uriBuilder =
      io.airbyte.server.apis.publicapi.mappers.PaginationMapper.getBuilder(apiHost, removePublicApiPathPrefix(WORKSPACES_PATH))
        .queryParam(WORKSPACE_IDS, io.airbyte.server.apis.publicapi.mappers.PaginationMapper.uuidListToQueryString(workspaceIds))
        .queryParam(INCLUDE_DELETED, includeDeleted)
    val workspacesResponse = WorkspacesResponse()
    workspacesResponse.next =
      io.airbyte.server.apis.publicapi.mappers.PaginationMapper.getNextUrl(workspaceReadList.workspaces, limit, offset, uriBuilder)
    workspacesResponse.previous = io.airbyte.server.apis.publicapi.mappers.PaginationMapper.getPreviousUrl(limit, offset, uriBuilder)
    workspacesResponse.data =
      workspaceReadList.workspaces.stream()
        .map { obj: WorkspaceRead? -> from(obj!!) }
        .toList()
    return workspacesResponse
  }
}
