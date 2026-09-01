package com.offerhub.gamification.security;

import com.offerhub.gamification.exception.ApiException;
import com.offerhub.gamification.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/** Lets a controller method declare a CallerIdentity parameter and get it filled in. */
@Component
public class CallerIdentityArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CallerIdentity.class.equals(parameter.getParameterType());
    }

    @Override
    public CallerIdentity resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer container,
                                          NativeWebRequest request,
                                          WebDataBinderFactory binderFactory) {

        String userId = request.getHeader(USER_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        // The gateway sets both after validating the token. Missing means the request
        // reached this service directly, bypassing authentication.
        if (userId == null || role == null) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Caller identity is missing");
        }

        try {
            return new CallerIdentity(UUID.fromString(userId), Role.valueOf(role));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Caller identity is malformed");
        }
    }
}
