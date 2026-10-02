data "aws_vpc" "plataforma" {
  filter {
    name   = "tag:Name"
    values = ["${var.project}-${var.ambiente}-vpc"]
  }
}

data "aws_subnets" "privadas" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.plataforma.id]
  }

  filter {
    name   = "tag:Name"
    values = ["${var.project}-${var.ambiente}-private-*"]
  }
}

resource "aws_db_subnet_group" "this" {
  name       = local.nome_completo
  subnet_ids = data.aws_subnets.privadas.ids
}

resource "aws_security_group" "banco" {
  name        = "${local.nome_completo}-db"
  description = "Acesso ao Postgres do catalogo, restrito a VPC da plataforma"
  vpc_id      = data.aws_vpc.plataforma.id

  ingress {
    description = "Postgres de dentro da VPC"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = [data.aws_vpc.plataforma.cidr_block]
  }
}

resource "aws_db_parameter_group" "this" {
  name   = local.nome_completo
  family = "postgres16"

  parameter {
    name         = "max_connections"
    value        = var.db_max_connections
    apply_method = "pending-reboot"
  }
}

resource "aws_db_instance" "this" {
  identifier     = local.nome_completo
  engine         = "postgres"
  engine_version = var.db_engine_version
  instance_class = var.db_instance_class

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  allocated_storage = var.db_allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.banco.id]
  publicly_accessible    = false
  parameter_group_name   = aws_db_parameter_group.this.name

  multi_az                = false
  backup_retention_period = 0
  skip_final_snapshot     = true
  deletion_protection     = false
  apply_immediately       = true

  lifecycle {
    precondition {
      condition     = var.db_pool_maximo * var.replicas_maximas + var.conexoes_reservadas <= var.db_max_connections
      error_message = "Orcamento de conexao estourado: pool ${var.db_pool_maximo} x ${var.replicas_maximas} replicas mais ${var.conexoes_reservadas} reservadas excede max_connections ${var.db_max_connections}."
    }
  }
}

resource "aws_ssm_parameter" "db_endpoint" {
  name  = "/${var.project}/${var.ambiente}/${var.nome}/db/endpoint"
  type  = "String"
  value = aws_db_instance.this.address
}

resource "aws_ssm_parameter" "db_port" {
  name  = "/${var.project}/${var.ambiente}/${var.nome}/db/port"
  type  = "String"
  value = tostring(aws_db_instance.this.port)
}

resource "aws_ssm_parameter" "db_name" {
  name  = "/${var.project}/${var.ambiente}/${var.nome}/db/name"
  type  = "String"
  value = aws_db_instance.this.db_name
}

resource "aws_ssm_parameter" "db_username" {
  name  = "/${var.project}/${var.ambiente}/${var.nome}/db/username"
  type  = "String"
  value = var.db_username
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/${var.project}/${var.ambiente}/${var.nome}/db/password"
  type  = "SecureString"
  value = var.db_password
}

locals {
  jdbc_url = "jdbc:postgresql://${aws_db_instance.this.endpoint}/${aws_db_instance.this.db_name}"
}

resource "aws_ssm_parameter" "db_url" {
  name  = "/${var.project}/${var.ambiente}/${var.nome}/db/url"
  type  = "String"
  value = local.jdbc_url
}
