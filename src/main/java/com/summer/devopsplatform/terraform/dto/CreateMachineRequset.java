package com.summer.devopsplatform.terraform.dto;

import lombok.Data;

@Data
public class CreateMachineRequset {
    private String environment; //环境名称 如dev test
    private String instanceType; //实例类型 如t2.micro
    private String awsRegion; //AWS区域。如us-west-2
}
