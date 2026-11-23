package com.itzetech.dpdms.config;

import com.itzetech.dpdms.common.PasswordPolicy;
import com.itzetech.dpdms.user.Role;
import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import com.itzetech.dpdms.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** On a brand-new database there is nobody to add the first administrator, so one is created from configuration. */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserAccountRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    AdminBootstrap(UserAccountRepository users, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.ADMIN)) {
            return;
        }
        AppProperties.BootstrapAdmin a = props.bootstrapAdmin();
        PasswordPolicy.require(a.password());
        UserAccount admin = new UserAccount(a.email().trim().toLowerCase(), a.fullName(), encoder.encode(a.password()), Role.ADMIN, UserStatus.INVITED);
        admin.setMustChangePassword(true);
        users.save(admin);
        log.warn("No administrator existed, so one was created: {}. Sign in with the password from BOOTSTRAP_ADMIN_PASSWORD "
                + "and choose a new one straight away.", admin.getEmail());
    }
}
