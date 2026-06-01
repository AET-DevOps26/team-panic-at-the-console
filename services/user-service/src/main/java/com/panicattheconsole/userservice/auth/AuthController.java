package com.panicattheconsole.userservice.auth;

import org.openapitools.api.AuthApi;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints ({@code /auth/register}, {@code /auth/login}, {@code /auth/logout}).
 * Uses generated {@link AuthApi} defaults (501) until auth logic lands in a follow-up PR.
 */
@RestController
class AuthController implements AuthApi {}
