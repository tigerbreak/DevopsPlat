package com.summer.devopsplatform.terraform.controller;

import com.summer.devopsplatform.terraform.dto.CreateMachineRequset;
import com.summer.devopsplatform.terraform.dto.TerraformResult;
import com.summer.devopsplatform.terraform.service.TerraformService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/machine")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MachineController {
    private final TerraformService terraformService;

    public MachineController(TerraformService terraformService) {
        this.terraformService = terraformService;
    }

    @PostMapping
    public TerraformResult createMachine(@RequestBody CreateMachineRequset request){
        // 简单的参数校验
        if (request.getEnvironment() == null || request.getEnvironment().isEmpty()) {
            return new TerraformResult(false, "环境名称不能为空");
        }

        //调用Terraform创建机器
        return terraformService.createMachine(request.getEnvironment(), request.getInstanceType(), request.getAwsRegion());


    }
}
