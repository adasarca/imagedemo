@ECHO OFF
aws dynamodb --endpoint-url http://localhost:4566 delete-table --table-name Post
aws dynamodb --endpoint-url http://localhost:4566 delete-table --table-name UserCredentials
aws dynamodb --endpoint-url http://localhost:4566 delete-table --table-name User
aws dynamodb --endpoint-url http://localhost:4566 delete-table --table-name UniqueField