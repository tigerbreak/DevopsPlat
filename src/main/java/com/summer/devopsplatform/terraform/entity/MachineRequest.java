package com.summer.devopsplatform.terraform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "machine_request")
public class MachineRequest {
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(nullable = false)
   private String environment;

   @Column(name = "instance_type", nullable = false)
   private String instanceType;

   @Column(name = "aws_region", nullable = false)
   private String awsRegion;

   @Column(name = "request_time", nullable = false)
   private LocalDateTime requestTime;

   @Column(name = "success")
   private Boolean success;
   @Lob
   @Column(name = "message", length = 1000)
   private String message;

   @Column(name = "instance_id")
   private String instanceId;

   @Column(name = "public_ip")
   private String publicIp;

   @Column(name = "completion_time")
   private LocalDateTime completionTime;

   // 构造函数
   public MachineRequest() {
       this.requestTime = LocalDateTime.now();
   }

   public MachineRequest(String environment, String instanceType, String awsRegion) {
       this();
       this.environment = environment;
       this.instanceType = instanceType;
       this.awsRegion = awsRegion;
   }
}
