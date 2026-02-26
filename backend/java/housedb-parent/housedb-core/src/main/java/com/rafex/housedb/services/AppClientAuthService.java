package com.rafex.housedb.services;

import java.util.List;
import java.util.UUID;

public interface AppClientAuthService {

    AuthResult authenticate(String clientId, char[] clientSecret) throws Exception;

    CreateClientResult createClient(String clientId, String name, char[] clientSecret, List<String> roles)
            throws Exception;

    record AuthResult(boolean ok, UUID appClientId, String clientId, List<String> roles, String code) {
        public static AuthResult ok(final UUID appClientId, final String clientId, final List<String> roles) {
            return new AuthResult(true, appClientId, clientId, roles, null);
        }

        public static AuthResult bad(final String code) {
            return new AuthResult(false, null, null, List.of(), code);
        }
    }

    record CreateClientResult(boolean ok, UUID appClientId, String clientId, String name, List<String> roles,
            String code) {
        public static CreateClientResult ok(final UUID appClientId, final String clientId, final String name,
                final List<String> roles) {
            return new CreateClientResult(true, appClientId, clientId, name, roles, null);
        }

        public static CreateClientResult bad(final String code) {
            return new CreateClientResult(false, null, null, null, List.of(), code);
        }
    }
}
