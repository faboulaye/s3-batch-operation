#### Deploy aws batch operation template
`aws cloudformation create-stack --template-body file://batch-operation-template.yml --stack-name s3-batch-operation --capabilities CAPABILITY_IAM`
#### Update aws batch operation template
`aws cloudformation update-stack --template-body file://batch-operation-template.yml --stack-name s3-batch-operation --capabilities CAPABILITY_IAM`