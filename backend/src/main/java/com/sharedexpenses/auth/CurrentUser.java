package com.sharedexpenses.auth;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** Access to the user authenticated for the current HTTP request (set by {@link AuthInterceptor}). */
public final class CurrentUser {

    static final String ATTRIBUTE = CurrentUser.class.getName() + ".userId";

    private CurrentUser() {
    }

    public static Long id() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object id = attributes == null ? null : attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (id == null) {
            throw new UnauthorizedException("You must be logged in");
        }
        return (Long) id;
    }
}
