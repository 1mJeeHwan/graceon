variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "ap-northeast-2" # Seoul
}

variable "aws_profile" {
  description = "AWS CLI profile to deploy with. Use the new account's profile, not the default."
  type        = string
  default     = "streamhub"
}

variable "project" {
  description = "Resource name prefix."
  type        = string
  default     = "streamhub"
}

variable "s3_bucket_name" {
  description = "Globally-unique S3 bucket name for media uploads."
  type        = string
}

variable "instance_type" {
  # The 2025 AWS "free account plan" restricts launches to its free-tier-eligible types
  # (t3.micro / t4g.micro / t3.small …) — the older t2.micro is rejected there. t3.micro works
  # with the AL2023 x86_64 AMI. (On a legacy 12-month free-tier account, t2.micro is the free one.)
  description = "EC2 instance type. New free-tier accounts: t3.micro (t2.micro is rejected)."
  type        = string
  default     = "t3.micro"
}

variable "db_instance_class" {
  description = "RDS instance class (free tier: db.t3.micro / db.t2.micro)."
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "Initial database name."
  type        = string
  default     = "streamhub"
}

variable "db_username" {
  description = "RDS master username."
  type        = string
  default     = "streamhub"
}

variable "db_password" {
  description = "RDS master password (8+ chars). Provide via tfvars or TF_VAR_db_password."
  type        = string
  sensitive   = true
}

variable "jwt_secret" {
  description = "JWT signing secret (long random string). Provide via tfvars or TF_VAR_jwt_secret."
  type        = string
  sensitive   = true
}

variable "kakao_rest_key" {
  description = "Kakao REST API key for live church discovery + geocoding (church.geocode.kakao-rest-key). Provide via tfvars or TF_VAR_kakao_rest_key."
  type        = string
  sensitive   = true
}

variable "chat_llm_api_key" {
  description = "Gemini API key for the LLM chatbot (chat.llm.api-key). Empty = rule fallback. Provide via tfvars or TF_VAR_chat_llm_api_key."
  type        = string
  sensitive   = true
  default     = ""
}

variable "ssh_public_key" {
  description = "SSH public key contents for EC2 access (e.g. file(\"~/.ssh/id_ed25519.pub\"))."
  type        = string
}

variable "ssh_ingress_cidr" {
  description = "MUST be set in terraform.tfvars to your IP/32; SSM Session Manager is the preferred access path."
  type        = string
  default     = "127.0.0.1/32"
}

variable "protect_database" {
  description = "Production DB teardown protection. true = deletion_protection + final snapshot (hard to destroy); false (default) = demo-friendly teardown. Encryption and automated backups apply regardless."
  type        = bool
  default     = false
}
