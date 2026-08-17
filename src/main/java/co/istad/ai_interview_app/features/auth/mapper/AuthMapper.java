package co.istad.ai_interview_app.features.auth.mapper;

import co.istad.ai_interview_app.features.auth.dto.RegisterRequest;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    UserRepresentation toUserRepresentation(RegisterRequest registerRequest);
}
