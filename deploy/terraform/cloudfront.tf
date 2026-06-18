# HTTPS front for the API (C9). The EC2 serves plain HTTP on :8080; a CloudFront distribution
# terminates TLS with its default *.cloudfront.net certificate and proxies to the instance, so the
# HTTPS Vercel frontends can call the API without mixed-content errors — no custom domain needed.
# An Elastic IP keeps the origin hostname stable across instance stop/start.

resource "aws_eip" "api" {
  domain   = "vpc"
  instance = aws_instance.api.id
  tags     = { Name = "${var.project}-api" }
}

resource "aws_cloudfront_distribution" "api" {
  enabled         = true
  comment         = "${var.project} API HTTPS front"
  http_version    = "http2"
  price_class     = "PriceClass_200"

  origin {
    domain_name = aws_eip.api.public_dns
    origin_id   = "ec2-api"
    custom_origin_config {
      http_port              = 8080
      https_port             = 443
      origin_protocol_policy = "http-only" # CloudFront → origin over HTTP:8080
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    target_origin_id       = "ec2-api"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods         = ["GET", "HEAD"]
    # Managed policies: CachingDisabled + AllViewerExceptHostHeader → transparent API proxy
    # (forwards auth headers/cookies/query/body; passes the origin its own Host).
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad"
    origin_request_policy_id = "b689b0a8-53d0-40ab-baf2-68738e2966ac"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }
}

output "cloudfront_url" {
  description = "HTTPS base URL for the API (use as the frontends' API base)."
  value       = "https://${aws_cloudfront_distribution.api.domain_name}"
}

output "api_eip" {
  description = "Stable public IP of the API instance."
  value       = aws_eip.api.public_ip
}
