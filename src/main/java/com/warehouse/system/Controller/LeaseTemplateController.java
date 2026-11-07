package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Lease.Request.CreateLeaseTemplateRequest;
import com.warehouse.system.DTO.Lease.Request.UpdateLeaseTemplateRequest;
import com.warehouse.system.DTO.Lease.Response.LeaseTemplateResponse;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Service.Lease.LeaseTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lease-templates")
@RequiredArgsConstructor
public class LeaseTemplateController {

    private final LeaseTemplateService templateService;

    @GetMapping("/default")
    public LeaseTemplateResponse getDefault() {
        return LeaseTemplateResponse.from(templateService.getPlatformDefaultTemplate());
    }

    @GetMapping("/mine")
        public List<LeaseTemplateResponse> mine(@AuthenticationPrincipal UserModel user) {
        return templateService.listMine(user.getId())
                .stream().map(LeaseTemplateResponse::from).toList();
    }

    @GetMapping("/{id}")
    public LeaseTemplateResponse get(@PathVariable UUID id, @AuthenticationPrincipal UserModel user) {
        return LeaseTemplateResponse.from(templateService.getTemplateFor(user.getId(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaseTemplateResponse create(@Valid @RequestBody CreateLeaseTemplateRequest body,
                                        @AuthenticationPrincipal UserModel user) {
        return LeaseTemplateResponse.from(templateService.createTemplate(user.getId(), body));
    }

    @PutMapping("/{id}")
    public LeaseTemplateResponse update(@PathVariable UUID id,
                                        @Valid @RequestBody UpdateLeaseTemplateRequest body,
                                        @AuthenticationPrincipal UserModel user) {
        return LeaseTemplateResponse.from(templateService.updateTemplate(user.getId(), id, body));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID id, @AuthenticationPrincipal UserModel user) {
        templateService.deactivateTemplate(user.getId(), id);
    }
}
