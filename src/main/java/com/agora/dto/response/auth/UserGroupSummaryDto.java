package com.agora.dto.response.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserGroupSummaryDto {

    private final UUID id;
    private final String name;
    @JsonProperty("isPreset")
    private final boolean isPreset;
}
