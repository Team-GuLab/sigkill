package com.gulab.sigkillserver.common;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();
}