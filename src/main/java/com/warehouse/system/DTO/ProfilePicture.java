package com.warehouse.system.DTO;

import org.springframework.core.io.Resource;

public record ProfilePicture(Resource resource, String contentType) {}

