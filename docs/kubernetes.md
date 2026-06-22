# StreamHub on Kubernetes (로컬 kind/minikube)

> `docker-compose` 스택(MySQL·Redis·MinIO·LocalStack·API)을 **K8s 매니페스트**로 옮긴 구성.
> 매니페스트는 `deploy/k8s/`. 프론트(Vercel)는 클러스터 밖이라 대상 아님.
> 핵심 트릭: API에 `SPRING_PROFILES_ACTIVE=docker`를 주면 `application-docker.yml`의 호스트명
> (`mysql`/`redis`/`minio`/`localstack`)이 **그대로 K8s Service 이름과 일치** → DB/Redis/MinIO/SQS
> 배선이 추가 설정 없이 동작한다.

---

## 1. 구조 (무엇이 무엇으로 매핑되나)

```
namespace: streamhub
├─ ConfigMap streamhub-config   (SPRING_PROFILES_ACTIVE=docker, EVENTLOG_TRANSPORT, CORS …)
├─ Secret    streamhub-secret   (JWT_SECRET, CHAT_LLM_API_KEY, KAKAO …)
├─ StatefulSet mysql  + Service mysql(headless) + PVC(volumeClaimTemplates)   ← 상태저장
├─ Deployment  redis  + Service redis                                          ← 캐시(무상태)
├─ Deployment  minio  + Service minio + PVC minio-data + Job minio-init        ← 오브젝트 스토리지
├─ Deployment  localstack + Service localstack                                 ← SQS 에뮬
└─ Deployment  streamhub-api + Service streamhub-api (+ initContainer wait-for-mysql)
```

| compose | K8s | 비고 |
|---|---|---|
| `services.mysql` + volume | **StatefulSet** + headless Service + PVC | 상태저장의 정석(안정 ID·전용 볼륨) |
| `services.redis` | Deployment + Service | 무상태 → PVC 없음 |
| `services.minio` + `minio-init` | Deployment + PVC + **Job** | Job = 일회성 버킷 생성 |
| `services.localstack` | Deployment + Service | SQS 큐는 API가 기동 시 생성 |
| `./mvnw` 또는 api 컨테이너 | **Deployment** + Service | env는 ConfigMap/Secret에서 주입 |
| `depends_on` | **initContainer**(`wait-for-mysql`) | K8s엔 depends_on 없음 → 대기 컨테이너로 순서 보장 |

## 2. 원리 (리소스가 하는 일)

- **Namespace** — 리소스 격리 단위. 모든 리소스가 `streamhub` 안에.
- **Deployment** — 무상태 Pod의 원하는 개수(replicas)·롤링업데이트·자가복구 관리. (redis/minio/localstack/api)
- **StatefulSet** — 상태저장용. Pod가 안정적 이름(`mysql-0`)과 **각자의 PVC**(volumeClaimTemplates)를 가져 재시작·재스케줄에도 데이터 유지. DB엔 Deployment보다 이게 맞다.
- **Service** — Pod는 죽고 살며 IP가 바뀌므로, 변하지 않는 **가상 DNS 이름**을 준다. `ClusterIP`(내부), `headless`(clusterIP: None → Pod로 직접 DNS, StatefulSet용).
- **PVC(PersistentVolumeClaim)** — "이만큼의 영속 디스크 줘" 요청. 클러스터의 StorageClass가 실제 볼륨을 붙인다. MySQL·MinIO 데이터가 여기 산다.
- **ConfigMap / Secret** — 설정/민감값을 이미지에서 분리. `envFrom`으로 컨테이너 env에 주입. Secret은 base64(암호화는 아님 — RBAC로 보호).
- **Job** — 끝나면 종료되는 일회성 작업(여기선 MinIO 버킷 생성).
- **initContainer** — 메인 컨테이너 전에 실행. `wait-for-mysql`이 `nc -z mysql 3306` 성공할 때까지 대기 → API가 DB 부팅 중 크래시루프 도는 걸 방지.
- **probe** — `readinessProbe`(트래픽 받을 준비됐나, Service 라우팅 제어), `livenessProbe`(죽었으면 재시작). API는 TCP 8080으로 점검.

## 3. 설정 방법 (로컬 kind로 띄우기)

### 3.1 준비물
- `kubectl`, 그리고 **kind**(Docker 안에 K8s) 또는 **minikube**. 여기선 kind 기준.
- Docker 런타임(Colima 등) 실행 중.
- 설치(macOS/Homebrew): `brew install kubectl kind`
  - (대안) Colima 내장 K8s: `colima start --kubernetes` — 단 로컬 이미지 적재 방식이 달라(k3s/containerd) 본 가이드의 `kind load`와는 경로가 다르다.

### 3.2 단계
```bash
cd ~/Documents/MyToys/streamhub-admin

# 1) kind 클러스터 생성
kind create cluster --name streamhub

# 2) API 이미지 빌드 후 kind에 적재 (레지스트리 푸시 불필요)
docker build -t streamhub-api:latest streamhub-api
kind load docker-image streamhub-api:latest --name streamhub

# 3) Secret 값 채우기: deploy/k8s/10-config-secret.yaml 의 JWT_SECRET 등을 실제 값으로
#    (또는: kubectl -n streamhub create secret generic streamhub-secret --from-literal=JWT_SECRET=$(openssl rand -base64 48) ...)

# 4) 매니페스트 적용 (번호 순)
kubectl apply -f deploy/k8s/

# 5) 상태 확인 — 전부 Running/Ready 될 때까지 (api는 mysql 기동 후 시작)
kubectl -n streamhub get pods -w

# 6) API 접속 (ClusterIP라 포트포워딩)
kubectl -n streamhub port-forward svc/streamhub-api 8080:8080
#   → http://localhost:8080/v3/api-docs , /pub/v1/home
```

> minikube면 2)는 `minikube image load streamhub-api:latest`, 외부 접속은 `minikube service -n streamhub streamhub-api`.

### 3.3 env 전환 포인트
| 위치 | 값 | 의미 |
|---|---|---|
| ConfigMap `SPRING_PROFILES_ACTIVE` | `docker` | application-docker.yml 사용(서비스 호스트명 일치) |
| ConfigMap `EVENTLOG_TRANSPORT` | `sqs` \| `kafka` | 감사로그 전송수단([[eventlog-kafka]] 참고). kafka면 Kafka StatefulSet/Service도 추가 |
| Secret `JWT_SECRET` | 필수 | 세션 JWT 서명 |
| Secret `CHAT_LLM_API_KEY`/`KAKAO` | 선택 | 비우면 mock/seed |

## 4. 검증 체크리스트
- [ ] `kubectl -n streamhub get pods` — mysql/redis/minio/localstack/api 모두 `Running`, api `READY 1/1`
- [ ] `kubectl -n streamhub logs deploy/streamhub-api` — `Started StreamhubApiApplication`
- [ ] `kubectl -n streamhub get pvc` — mysql/minio PVC `Bound`
- [ ] port-forward 후 `/pub/v1/home` 200
- [ ] `kubectl -n streamhub get job minio-init` — `Complete`
- [ ] (정리) `kubectl delete -f deploy/k8s/` / `kind delete cluster --name streamhub`

## 5. 향후 개선 (면접 어필)
- **Ingress + ingress-nginx**로 도메인 노출(포트포워딩 대신)
- **HPA**(HorizontalPodAutoscaler)로 api 부하 기반 오토스케일
- **Helm 차트**로 패키징(values로 env 관리)
- **Kustomize** overlay로 local/prod 분리
- MySQL을 **관리형(RDS)**로 빼고 앱만 K8s에(상태/무상태 분리)
- 리소스 `requests/limits` 튜닝 + `PodDisruptionBudget`
