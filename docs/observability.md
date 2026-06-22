# 관측성 (Observability) — Actuator · Micrometer · Prometheus · Grafana

> 앱이 "지금 잘 돌고 있나"를 **메트릭으로 관측**하는 구성. Spring Boot Actuator가 메트릭을 만들고,
> Micrometer가 Prometheus 포맷으로 노출(`/actuator/prometheus`), Prometheus가 주기적으로 수집,
> Grafana가 대시보드로 시각화한다.

---

## 1. 구조

```
[Spring Boot API]                         [수집]                 [시각화]
Actuator + Micrometer ──/actuator/prometheus──▶ Prometheus ──(PromQL)──▶ Grafana
 (JVM·HTTP·DataSource·                            :9090(스크랩 15s)        :3030(대시보드)
  Kafka·캐시 등 메트릭)
```

- **Actuator** (`spring-boot-starter-actuator`) — 앱 내부 상태를 엔드포인트로 노출: `/actuator/health`(헬스), `/actuator/metrics`, `/actuator/prometheus`(스크랩용).
- **Micrometer** (`micrometer-registry-prometheus`) — 메트릭 파사드. JVM(힙/GC/스레드), HTTP 요청(`http_server_requests`), HikariCP 커넥션풀, 로그 카운트 등을 자동 계측해 Prometheus 텍스트 포맷으로 변환.
- **Prometheus** — 시계열 DB + 스크래퍼. `prometheus.yml`의 타깃을 주기적으로 긁어 저장. PromQL로 질의.
- **Grafana** — Prometheus를 데이터소스로 대시보드 렌더. 본 구성은 데이터소스를 자동 프로비저닝.

## 2. 원리 (핵심)

- **Pull 모델.** 앱이 보내는 게 아니라 **Prometheus가 끌어간다(scrape)**. 앱은 `/actuator/prometheus`만 열어두면 됨. 그래서 앱은 모니터링 인프라를 몰라도 된다(느슨한 결합).
- **메트릭 종류** — Counter(증가만, 요청수), Gauge(증감, 힙 사용량), Timer/Histogram(지연시간 분포: `http_server_requests_seconds`). p95/p99 지연은 히스토그램에서 PromQL `histogram_quantile`로 계산.
- **라벨(label).** 메트릭에 `uri`, `status`, `method`, `application` 같은 차원이 붙어, "엔드포인트별 5xx 비율" 같은 슬라이싱이 된다. (우리는 `management.metrics.tags.application=streamhub-api`로 앱 라벨 부여)
- **헬스 프로브 분리.** `management.endpoint.health.probes.enabled=true`로 `/actuator/health/liveness`·`/readiness`가 생겨 **K8s liveness/readiness 프로브**에 그대로 쓴다(60-api.yaml 적용됨).
- **보안.** `/actuator/health`·`/actuator/prometheus`만 `SecurityConfig.PUBLIC_PATHS`로 공개(메트릭엔 민감정보 없음). 운영에선 보통 별도 management 포트로 분리한다.

## 3. 설정 방법 (로컬)

```bash
cd ~/Documents/MyToys/streamhub-admin

# 1) Prometheus + Grafana 기동 (docker-compose에 추가됨)
docker compose up -d prometheus grafana

# 2) 백엔드 실행 (호스트, ./mvnw) — Actuator가 자동으로 메트릭 노출
cd streamhub-api && ./mvnw spring-boot:run

# 3) 확인
curl -s localhost:8080/actuator/health        # {"status":"UP"}
curl -s localhost:8080/actuator/prometheus | head   # # HELP jvm_... 형태 메트릭
open http://localhost:9090                     # Prometheus — Status>Targets에서 streamhub-api UP
open http://localhost:3030                     # Grafana (anon Admin) — Prometheus 데이터소스 연결됨
```

Grafana에서 대시보드 Import → ID **4701**(JVM Micrometer) 또는 **11378**(Spring Boot Statistics) → 데이터소스 Prometheus 선택하면 JVM·HTTP 지표가 바로 보인다.

### 스크랩 타깃 주의
- 백엔드가 **호스트(`./mvnw`)** 면: `deploy/observability/prometheus.yml`의 타깃 `host.docker.internal:8080` (그대로).
- 백엔드가 **컨테이너 안**이면: 타깃을 `streamhub-api:8080`으로 바꾼다.

## 4. 쿠버네티스에서
- `60-api.yaml`에 `prometheus.io/scrape` 어노테이션 + actuator 헬스 프로브를 넣어뒀다.
- 풀스택은 **kube-prometheus-stack**(Helm) 또는 **ServiceMonitor**(Prometheus Operator)로 자동 수집하게 한다(무거우니 별도 트랙).

## 5. 무엇을 보나 (대표 PromQL)
- 요청량: `sum(rate(http_server_requests_seconds_count[1m]))`
- p95 지연: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))`
- 5xx 비율: `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))`
- 힙: `jvm_memory_used_bytes{area="heap"}`
- DB 커넥션: `hikaricp_connections_active`

## 6. 향후 개선 (면접 어필)
- **분산 트레이싱**: Micrometer Tracing + OTLP → **Tempo/Jaeger**, Grafana에서 trace-to-metric 연계
- **로그 집계**: Loki(+promtail) → Grafana 단일 창에서 메트릭·로그·트레이스
- **알림(Alerting)**: Prometheus Alertmanager 룰(5xx 급증, p99 임계)
- **SLO 대시보드** + 에러버짓
- 비즈니스 메트릭 커스텀(`@Timed`, `Counter`)으로 "주문 생성율" 등 도메인 지표
