# Generic charts template for Spring Boot + Istio

Generic helm-charts and examples for enterprise-level refined traffic governance and canary deployment based on springboot+istio

## 2. Building

```bash
git clone https://github.com/wl4g/springboot-istio-charts-template.git

cd springboot-istio-charts-template/springboot-demo/

mvn clean package -DskipTests -P build:tar:docker
```

## 3. Deploy on Docker

```bash
docker run --rm wl4g/springboot-demo:1.0.0
```

## 4. Deploy on Kubernetes

- Initial deploy

```bash
kubectl create ns demo

kubectl label ns demo istio-injection=enabled --overwrite

# By default, only the baseline will be deployed initially.
helm -n demo upgrade -i springboot-demo ./helm/app-stack
```

## 5. Testing

### 5.1 Gets Istio ingress information

```bash
export nodeIP=$(ip a | grep -E '^[0-9]+: (em|eno|enp|ens|eth|wlp)+[0-9]' -A2 | grep inet | awk -F ' ' '{print $2}' | cut -f1 -d/ | head -1)
export nodePort=$(kubectl -n istio-system get svc istio-ingressgateway -ojson | jq -r '.spec.ports[] | select (.name == "http2") | .nodePort')
```

### 5.2 Simulation normal user requests

```bash
for i in `seq 1 100`; do echo -n "response $i from app version is: "; \
curl -s -XPOST \
-H 'Host: springboot-demo.wl4g.io' \
-d '{"author":"James Wrong","sex":"Man"}' \
${nodeIP}:${nodePort}/demo/echo?name=jack | jq -r '.appversion' ; done
```

### 5.3 Redeploy, modify the percentage of load traffic of different versions of pods

```bash
helm -n demo upgrade --install demo-stack . --set="\
springboot-demo.image.baselineTag=1.0.0,\
springboot-demo.image.upgradeTag=1.0.1,\
springboot-demo.governance.istio.ingress.http.canary.baseline.weight=80,\
springboot-demo.governance.istio.ingress.http.canary.upgrade.weight=20"
```

### 5.4 Simulation Internal users requests

```bash
for i in `seq 1 100`; do echo -n "response $i from app version is: "; \
curl -s -XPOST \
-H 'Host: springboot-demo.wl4g.io' \
-H 'Cookie: "sid=abcdefg; _email=jack@wl4g.io; uid=abcd1234"' \
-d '{"author":"James Wrong","sex":"Man"}' \
${nodeIP}:${nodePort}/demo/echo?name=jack | jq -r '.appversion' ; done
```

### 5.5 Summary

- The 5.2 No matter how many times the request is made, the responsed result `appversion` is always `1.0.0`, because it is a simulated normal user.

- The 5.4 If the request is made 100 times, about 80% of the response result `appversion` is `1.0.0`, and 20% is `1.0.1`, because it is a request to simulate an internal experimental user.
