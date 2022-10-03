#!/usr/bin/env bash

echo "########### Configuring profile ###########"

aws configure set aws_access_key_id test-key
aws configure set aws_secret_access_key test-secret
aws configure set region eu-west-1
aws configure set output json

echo "########### Creating S3 bucket ###########"

aws --endpoint-url http://localhost:4566 s3 mb s3://imagedemo-posts

echo "########### Creating DynamoDB tables ###########"

aws dynamodb --endpoint-url http://localhost:4566 create-table --cli-input-json file:///tmp/dynamodb-tables/UserCredentials.json
aws dynamodb --endpoint-url http://localhost:4566 create-table --cli-input-json file:///tmp/dynamodb-tables/User.json
aws dynamodb --endpoint-url http://localhost:4566 create-table --cli-input-json file:///tmp/dynamodb-tables/UniqueField.json
aws dynamodb --endpoint-url http://localhost:4566 create-table --cli-input-json file:///tmp/dynamodb-tables/Post.json
aws dynamodb --endpoint-url http://localhost:4566 update-time-to-live --table-name Post --time-to-live-specification "Enabled=true, AttributeName=ExpirationTime"
