package com.panicattheconsole.userservice.users;

import org.openapitools.api.UsersApi;
import org.springframework.web.bind.annotation.RestController;

/**
 * User profile and directory endpoints ({@code /users/me}, {@code /users}).
 * Uses generated {@link UsersApi} defaults (501) until persistence lands in a follow-up PR.
 */
@RestController
class UsersController implements UsersApi {}
