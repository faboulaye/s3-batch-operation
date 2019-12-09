package com.app.aws.batchoperation;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3control.AWSS3Control;
import com.amazonaws.services.s3control.AWSS3ControlClient;
import com.amazonaws.services.s3control.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Optional;
import java.util.UUID;

import static com.amazonaws.regions.Regions.EU_WEST_1;

@Service
public class BatchOperationService {

    private static final Logger log = LoggerFactory.getLogger(BatchOperationService.class);

    @Value("${app.aws.accountId:}")
    private String accountId;

    @Value("${app.aws.batch-operation.role:}")
    private String batchOperationRole;

    @Value("${app.aws.batch-operation.bucket.report:}")
    private String batchOperationReportBucket;

    @Value("${app.aws.batch-operation.bucket.source:}")
    private String batchOperationSourceBucket;

    @Value("${app.aws.batch-operation.bucket.target:}")
    private String batchOperationTargetBucket;

    private AmazonS3 s3Client;
    private AWSS3Control s3ControlClient;

    @PostConstruct
    public void init() {
        try {
            s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(EU_WEST_1)
                    .build();
            s3ControlClient = AWSS3ControlClient.builder()
                    .withCredentials(new ProfileCredentialsProvider())
                    .withRegion(EU_WEST_1)
                    .build();
        } catch (SdkClientException e) {
            log.error("Failed to start aws client");
            throw new AmazonServiceException("Failed to start aws client", e);
        }
    }

    public Optional<CreateJobResult> executeBatch() {
        try {
            CreateJobResult result = s3ControlClient.createJob(new CreateJobRequest()
                    .withAccountId(accountId)
                    .withOperation(createJobOperation())
                    .withManifest(createJobManifest())
                    .withReport(createJobReport())
                    .withPriority(42)
                    .withRoleArn(batchOperationRole)
                    .withClientRequestToken(UUID.randomUUID().toString())
                    .withDescription("Copy Batch Operation")
                    .withConfirmationRequired(false));
            log.info("Start batch operations with id {} successfully", result.getJobId());
            return Optional.ofNullable(result);
        } catch (AmazonServiceException e) {
            log.error("Failed to process with aws services", e);
        } catch (SdkClientException e) {
            log.error("Failed to process with aws client ", e);
        }
        return Optional.empty();
    }

    public String getJobReport(String jobId) {
        return null;
    }

    private JobOperation createJobOperation() {
        JobOperation jobOperation = new JobOperation()
                .withS3PutObjectCopy(new S3CopyObjectOperation().withTargetResource(batchOperationTargetBucket));
        return jobOperation;
    }

    private JobManifest createJobManifest() {
        String eTag = uploadManifest();
        if(StringUtils.isEmpty(eTag)) {
            throw new AmazonServiceException("Failed too upload manifest file");
        }
        JobManifest manifest = new JobManifest()
                .withSpec(new JobManifestSpec()
                        .withFormat("S3BatchOperations_CSV_20180820")
                        .withFields(new String[]{
                                "Bucket", "Key"
                        }))
                .withLocation(new JobManifestLocation()
                        .withObjectArn(batchOperationSourceBucket + "/manifest.csv")
                        .withETag(uploadManifest()));
        return manifest;
    }

    private JobReport createJobReport() {
        JobReport jobReport = new JobReport()
                .withBucket(batchOperationReportBucket)
                .withPrefix("reports")
                .withFormat("Report_CSV_20180820")
                .withEnabled(true)
                .withReportScope("AllTasks");
        return jobReport;
    }

    private String uploadManifest() {
        try {
            File manifest = new File("src/main/resources/data/manifest.csv");
            if(!manifest.isFile()) {
                throw new AmazonServiceException("Failed to find file");
            }
            PutObjectRequest request = new PutObjectRequest("batch-source-operation",
                    "manifest.csv", manifest);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/csv");
            metadata.addUserMetadata("x-amz-meta-title", "manifest file");
            request.setMetadata(metadata);
            return s3Client.putObject(request).getETag();
        } catch (AmazonServiceException e) {
            log.error("Failed to process with aws services", e);
        } catch (SdkClientException e) {
            log.error("Failed to process with aws client ", e);
        }
        return null;
    }

}
