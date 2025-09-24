package com.summer.devopsplatform.terraform.dto;

import lombok.Data;

@Data
public class TerraformResult {
    private boolean success;
    private String message;
    private String instanceId;
    private String publicip;

    public TerraformResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
