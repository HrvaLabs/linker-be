package cc.hrva.ladica.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PushRequest(@NotNull @Size(max = 1000) List<@Valid LinkDto> links) {
}
