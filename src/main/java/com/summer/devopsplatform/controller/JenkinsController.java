package com.summer.devopsplatform.controller;

import com.summer.devopsplatform.service.JenkinsService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/jenkins")
@Slf4j
public class JenkinsController {
    private final JenkinsService jenkinsService;

    public JenkinsController(JenkinsService jenkinsService) {
        this.jenkinsService = jenkinsService;
    }

    @PostMapping("/trigger/{jobName}")
    public String triggerJob(@PathVariable String jobName){
        log.info("trigger job: {}", jobName);
        return jenkinsService.triggerJob(jobName);
    }
}
