package com.itzetech.dpdms.user;

import com.itzetech.dpdms.project.ProjectService;
import com.itzetech.dpdms.security.AuthUser;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** A short list of client companies for pick lists and filters. Management only. */
@RestController
class CompanyController {

    record CompanyDto(UUID id, String name) {
    }

    private final ClientCompanyRepository companies;

    CompanyController(ClientCompanyRepository companies) {
        this.companies = companies;
    }

    @GetMapping("/api/companies")
    List<CompanyDto> list(@AuthenticationPrincipal AuthUser me) {
        ProjectService.requireManagement(me);
        return companies.findAll(Sort.by("name")).stream().map(c -> new CompanyDto(c.getId(), c.getName())).toList();
    }
}
