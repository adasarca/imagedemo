#!/usr/bin/env bash

echo "########### Configuring profile ###########"

aws configure set aws_access_key_id test-key
aws configure set aws_secret_access_key test-secret
aws configure set region eu-west-1
aws configure set output json

echo "########### Deploying Cloud Formation Infrastructure ###########"

#aws --endpoint-url http://localhost:4566 cloudformation deploy --stack-name cfn-quickstart-stack --template-file /tmp/cloud-formation/cloud-formation.yml