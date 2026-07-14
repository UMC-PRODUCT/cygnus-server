package com.umc.product.project.adapter.in.web.fixture;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("project-policy-catalog-mutation")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectPolicyCatalogMutationController {

    @GetMapping("/catalog-mutation-fixture")
    public void unexpectedSurface() {
    }
}
