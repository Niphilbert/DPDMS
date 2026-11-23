package com.itzetech.dpdms.project;

import com.itzetech.dpdms.user.ClientCompany;
import com.itzetech.dpdms.user.ClientCompanyRepository;
import com.itzetech.dpdms.user.UserAccount;
import com.itzetech.dpdms.user.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Looks up display names for ids so responses can show people and companies instead of bare ids. */
@Component
public class Names {

    private final UserAccountRepository users;
    private final ClientCompanyRepository companies;

    public Names(UserAccountRepository users, ClientCompanyRepository companies) {
        this.users = users;
        this.companies = companies;
    }

    public Map<UUID, String> users(Collection<UUID> ids) {
        Map<UUID, String> out = new HashMap<>();
        Collection<UUID> wanted = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (!wanted.isEmpty()) {
            for (UserAccount u : users.findAllById(wanted)) {
                out.put(u.getId(), u.getFullName());
            }
        }
        return out;
    }

    public String user(UUID id) {
        return id == null ? null : users.findById(id).map(UserAccount::getFullName).orElse("Unknown");
    }

    public Map<UUID, String> companies(Collection<UUID> ids) {
        Map<UUID, String> out = new HashMap<>();
        Collection<UUID> wanted = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (!wanted.isEmpty()) {
            for (ClientCompany c : companies.findAllById(wanted)) {
                out.put(c.getId(), c.getName());
            }
        }
        return out;
    }

    public String company(UUID id) {
        return id == null ? null : companies.findById(id).map(ClientCompany::getName).orElse("Unknown");
    }
}
