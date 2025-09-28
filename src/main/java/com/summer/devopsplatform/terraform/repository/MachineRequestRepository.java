package com.summer.devopsplatform.terraform.repository;

import com.summer.devopsplatform.terraform.entity.MachineRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineRequestRepository extends JpaRepository<MachineRequest, Long> {
    // 根据环境名称查询
    List<MachineRequest> findByEnvironment(String environment);

    // 根据成功状态查询
    List<MachineRequest> findBySuccess(Boolean success);

    // 根据环境名称和成功状态查询
    List<MachineRequest> findByEnvironmentAndSuccess(String environment, Boolean success);
}
