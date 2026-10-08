package com.warehouse.system.DTO.Result;

import com.warehouse.system.DTO.Response.LoginResponse;
import com.warehouse.system.DTO.TokenPair;

public record AuthResult(LoginResponse body, TokenPair tokenPair) { }