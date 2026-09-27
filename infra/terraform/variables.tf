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
  type    = string
  default = "16.4"
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
  type    = string
  default = "st_cat_user"
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
