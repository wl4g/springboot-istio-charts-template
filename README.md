# Generic charts template for Spring Boot + Istio

Generic helm-charts and examples for enterprise-level refined traffic governance and canary deployment based on springboot+istio

## 2. Features

- One-step support for istio-based dual-version (`baseline`/`upgrade`) canary (grayscale) deploy, percentage traffic load supported.

- Automatically calculate the number of Pods replicas based on the traffic percentage.

- The based on istio's traffic governance capabilities, it supports the necessary functions of distributed microservices such as `canary routing` , `request limiting`, `circuit breaker`, `fault injection`, and `request response filtering` etc.

- Automatically add the response header `x-app-version`, the standard microservice interface is friendly to gray service diagnosis.

## 3. Building

```bash
git clone https://github.com/wl4g/springboot-istio-charts-template.git

cd springboot-istio-charts-template/springboot-demo/

mvn clean package -DskipTests -P build:tar:docker
```

## 4. Deploy on Docker

```bash
docker run --rm wl4g/springboot-demo:1.0.0
```

## 5. Deploy on Kubernetes

- Initial deploy

```bash
kubectl create ns demo

kubectl label ns demo istio-injection=enabled --overwrite

# By default, only the baseline will be deployed initially.
helm -n demo upgrade -i demo ./helm/app-stack
```

## 6. Testing

### 6.1 Gets Istio ingress information

```bash
export nodeIP=$(ip a | grep -E '^[0-9]+: (em|eno|enp|ens|eth|wlp)+[0-9]' -A2 | grep inet | awk -F ' ' '{print $2}' | cut -f1 -d/ | head -1)
export nodePort=$(kubectl -n istio-system get svc istio-ingressgateway -ojson | jq -r '.spec.ports[] | select (.name == "http2") | .nodePort')
```

### 6.2 Simulation normal user requests (production version)

```bash
for i in `seq 1 100`; do echo -n "response $i from app version is: "; \
curl -s -XPOST \
-H 'Host: springboot-demo.wl4g.io' \
-d '{"author":"James Wrong","sex":"Man"}' \
${nodeIP}:${nodePort}/demo/echo?name=jack | jq -r '.appversion' ; done
```

### 6.3 Redeploy, modify the percentage of load traffic of different versions of pods

```bash
helm -n demo upgrade --install demo-stack . --set="\
springboot-demo.image.baselineTag=1.0.0,\
springboot-demo.image.upgradeTag=1.0.1,\
springboot-demo.governance.istio.ingress.http.canary.baseline.weight=80,\
springboot-demo.governance.istio.ingress.http.canary.upgrade.weight=20"
```

### 6.4 Simulation Internal users requests (gray version)

- Requests Example

```bash
for i in `seq 1 100`; do echo -n "response $i from app version is: "; \
curl -s -XPOST \
-H 'Host: springboot-demo.wl4g.io' \
-H 'Cookie: "sid=abcdefg; _email=jack@wl4g.io; uid=abcd1234"' \
-d '{"author":"James Wrong","sex":"Man"}' \
${nodeIP}:${nodePort}/demo/echo?name=jack | jq -r '.appversion' ; done
```

- Response Example

```log
POST /demo/echo?name=jack HTTP/1.1
Host: springboot-demo.wl4g.io
User-Agent: curl/7.68.0
Accept: */*
Cookie: "sid=abcdefg; _email=jack@wl4g.io; uid=abcd1234"
Content-Length: 36
Content-Type: application/x-www-form-urlencoded

HTTP/1.1 200 OK
content-type: application/json
content-length: 888
date: Mon, 15 Aug 2022 02:28:51 GMT
x-envoy-upstream-service-time: 7
server: istio-envoy
x-app-version: 1.0.1

1.0.1
```

- Notice: The custom additional response header: **`x-app-version`** is the version of the backend application processing the current request.

### 6.5 Summary

- The 6.2 No matter how many times the request is made, the responsed result `appversion` is always `1.0.0`, because it is a simulated normal user.

- The 6.4 If the request is made 100 times, about 80% of the response result `appversion` is `1.0.0`, and 20% is `1.0.1`, because it is a request to simulate an internal experimental user.
