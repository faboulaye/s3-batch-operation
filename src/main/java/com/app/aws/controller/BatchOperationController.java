package com.app.aws.controller;

import com.amazonaws.services.s3control.model.CreateJobResult;
import com.app.aws.batchoperation.BatchOperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/batch")
public class BatchOperationController {

    private static final Logger log = LoggerFactory.getLogger(BatchOperationController.class);

    @Autowired
    private BatchOperationService service;

    @GetMapping
    public String runBatch() {
        log.info("Request for starting batch job operation...");
        Optional<CreateJobResult> result = service.executeBatch();
        if (result.isPresent()) {
            return String.format("Started Job with id %s successfully", result.get().getJobId());
        } else {
            return "Failed to start job";
        }
    }
}
