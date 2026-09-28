output "ecr_repository_url" {
  value = aws_ecr_repository.this.repository_url
}

output "ecr_repository_name" {
  value = aws_ecr_repository.this.name
}

output "ssm_parameter_ecr_url" {
  value = aws_ssm_parameter.ecr_url.name
}

output "db_endpoint" {
  value = aws_db_instance.this.address
}

output "db_jdbc_url" {
  value = aws_ssm_parameter.db_url.value
}

output "ssm_parameters_db" {
  value = {
    url      = aws_ssm_parameter.db_url.name
    endpoint = aws_ssm_parameter.db_endpoint.name
    porta    = aws_ssm_parameter.db_port.name
    banco    = aws_ssm_parameter.db_name.name
    usuario  = aws_ssm_parameter.db_username.name
    senha    = aws_ssm_parameter.db_password.name
  }
}
