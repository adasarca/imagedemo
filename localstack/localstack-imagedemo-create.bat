@ECHO OFF
aws dynamodb --endpoint-url http://localhost:4566 create-table --cli-input-json file://imagedemo/src/main/resources/dynamodb/UserCredentials.json
aws dynamodb --endpoint-url http://localhost:4566 create-table --cli-input-json file://imagedemo/src/main/resources/dynamodb/User.json
aws dynamodb --endpoint-url http://localhost:4566 create-table --cli-input-json file://imagedemo/src/main/resources/dynamodb/UniqueField.json
aws dynamodb --endpoint-url http://localhost:4566 create-table --cli-input-json file://imagedemo/src/main/resources/dynamodb/Post.json
aws dynamodb --endpoint-url http://localhost:4566 update-time-to-live --table-name Post --time-to-live-specification "Enabled=true, AttributeName=ExpirationTime"