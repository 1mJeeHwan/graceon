# HTTPS front for the API (C9). The EC2 serves plain HTTP on :8080; a CloudFront distribution
# terminates TLS with its default *.cloudfront.net certificate and proxies to the instance, so the
# HTTPS Vercel frontends can call the API without mixed-content errors — no custom domain needed.
# An Elastic IP keeps the origin hostname stable across instance stop/start.

resource "aws_eip" "api" {
  domain   = "vpc"
  instance = aws_instance.api.id
  tags     = { Name = "${var.project}-api" }
}

# Origin Access Control lets CloudFront sign (SigV4) its requests to the private S3
# media bucket, so the bucket stays private (no public ACLs/policy) while the CDN reads it.
resource "aws_cloudfront_origin_access_control" "media" {
  name                              = "${var.project}-media"
  description                       = "OAC for the private media S3 bucket (HLS segments)"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
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

  # Private S3 media bucket, reached via OAC (no custom_origin_config for S3).
  origin {
    domain_name              = aws_s3_bucket.media.bucket_regional_domain_name
    origin_id                = "s3-media"
    origin_access_control_id = aws_cloudfront_origin_access_control.media.id
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

  # Encrypted HLS segments (hls/track-{id}/segNNN.ts) are served from the private S3
  # bucket. AES-encrypted .ts means public CDN caching is safe.
  ordered_cache_behavior {
    path_pattern           = "/hls/*"
    target_origin_id       = "s3-media"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    compress               = true
    # Managed policies: CachingOptimized + CORS-S3Origin
    # (forwards CORS/Range headers so the browser player can byte-range the segments).
    cache_policy_id          = "658327ea-f89d-4fab-a63d-7e88639e58f6"
    origin_request_policy_id = "88a5eaf4-2fd4-4709-b370-b4c650ea3fcf"
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

output "hls_segment_base_url" {
  description = "Base URL for encrypted HLS segments (backend HLS_SEGMENT_BASE_URL). Same CloudFront domain — /hls/* routes to S3, everything else to the API."
  value       = "https://${aws_cloudfront_distribution.api.domain_name}"
}
