variable "ambiente" {
  type = string

  validation {
    condition     = contains(["hml", "prd"], var.ambiente)
    error_message = "ambiente deve ser hml ou prd."
  }
}

variable "project" {
  type    = string
  default = "servicetrack"
}

variable "nome" {
  type    = string
  default = "catalogo"
}

variable "region" {
  type    = string
  default = "us-east-1"
}

variable "max_image_count" {
  type    = number
  default = 10
}

variable "untagged_expire_days" {
  type    = number
  default = 7
}

variable "db_engine_version" {
  description = "Versao do Postgres. Apenas a maior: a AWS retira versoes menores de circulacao e o RDS escolhe a menor disponivel no momento da criacao."
  type        = string
  default     = "16"
}

variable "db_instance_class" {
  type    = string
  default = "db.t3.micro"
}

variable "db_allocated_storage" {
  type    = number
  default = 20
}

variable "db_name" {
  type    = string
  default = "st_cat"
}

variable "db_username" {
  description = "Usuario mestre do banco. Vem do secret ST_CAT_DB_USER da esteira; sem default de proposito."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9_]{2,62}$", var.db_username))
    error_message = "db_username deve comecar por letra minuscula e usar apenas minusculas, digitos e sublinhado."
  }
}

variable "db_password" {
  description = "Senha do usuario mestre. Vem do secret ST_CAT_DB_PASSWORD da esteira; sem default de proposito."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.db_password) >= 16 && length(var.db_password) <= 128
    error_message = "db_password deve ter de 16 a 128 caracteres."
  }

  validation {
    condition     = !can(regex("[/@\"' ]", var.db_password))
    error_message = "O RDS recusa barra, arroba, aspas simples, aspas duplas e espaco na senha do usuario mestre."
  }
}

variable "db_max_connections" {
  type    = number
  default = 60
}

variable "db_pool_maximo" {
  description = "Teto do pool de cada replica da aplicacao, o mesmo valor de ST_CAT_DB_POOL_MAX."
  type        = number
  default     = 10
}

variable "replicas_maximas" {
  description = "Teto de replicas do HPA da aplicacao. Subir o HPA sem subir o orcamento faz o plan falhar."
  type        = number
  default     = 4
}

variable "conexoes_reservadas" {
  description = "Conexoes fora do pool: psql do ritual de ambiente, manutencao, superusuario."
  type        = number
  default     = 10
}
