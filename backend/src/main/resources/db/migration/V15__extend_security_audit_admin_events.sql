alter table security_audit_event
    drop constraint ck_security_audit_event_type;

alter table security_audit_event
    add constraint ck_security_audit_event_type
        check (event_type in (
            'LOGIN_SUCCESS',
            'LOGIN_FAILURE',
            'LOGIN_LOCKED',
            'LOGIN_DISABLED',
            'LOGOUT',
            'ADMIN_USER_DETAIL_VIEW',
            'ADMIN_USER_STATUS_CHANGE',
            'ADMIN_USER_PASSWORD_RESET'
        ));
