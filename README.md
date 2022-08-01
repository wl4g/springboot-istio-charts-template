# Demo httpserver project for Spring Boot

## 1. Building

```bash
git clone https://github.com/wl4g/springboot-istio-canary-demo.git
cd springboot-istio-canary-demo
mvn clean package -DskipTests -P build:tar:docker
```

## 2. Deploy on Docker

```bash
docker run --rm wl4g/springboot-istio-canary-demo:1.0.0
```

## 3. Deploy on Kubernetes

- Initial deploy

```bash
cd springboot-istio-canary-demo/deploy/helm

kubectl ns create demo

kubectl label ns demo istio-injection=enabled --overwrite

# By default, only the baseline will be deployed initially.
helm -n demo upgrade --install demo-stack .
```

## 4. Testing

### 4.1 Gets Istio ingress information

```bash
export nodeIp=$(ip a | grep -E '^[0-9]+: (em|eno|enp|ens|eth|wlp)+[0-9]' -A2 | grep inet | awk -F ' ' '{print $2}' | cut -f1 -d/ | head -1)
export istioInNodePort=$(kubectl -n istio-system get svc istio-ingressgateway -ojson | jq -r '.spec.ports[] | select (.name == "http2") | .nodePort')
```

### 4.2 Simulation normal user requests.

```bash
for i in `seq 1 100`; do echo -n "$i Result: "; \
curl -sk -XPOST \
-H 'Host: springboot-istio-canary-demo.wl4g.io' \
-d '{"author":"James Wrong","sex":"Man"}' \
${nodeIp}:${istioInNodePort}/demo/echo?name=jack | jq -r '.appversion' ; done
```

### 4.3 Redeploy, modify the percentage of load traffic of different versions of pods.

```bash
helm -n demo upgrade --install demo-stack . --set="\
    springboot-istio-canary-demo.image.baselineTag=1.0.0,\
    springboot-istio-canary-demo.image.upgradeTag=1.0.1,\
    springboot-istio-canary-demo.governance.istio.ingress.http.canary.baseline.weight=80,\
    springboot-istio-canary-demo.governance.istio.ingress.http.canary.upgrade.weight=20"
```

### 4.4 Simulation Internal users requests.

```bash
for i in `seq 1 100`; do echo -n "$i Result: "; \
curl -sk -XPOST \
-H 'Host: springboot-istio-canary-demo.wl4g.io' \
-H 'Cookie: "sid=abcdefg; _email=jack@wl4g.io; uid=abcd1234"' \
-d '{"author":"James Wrong","sex":"Man"}' \
${nodeIp}:${istioInNodePort}/demo/echo?name=jack | jq -r '.appversion' ; done
```

### 4.5 Summary

- The 4.2 No matter how many times the request is made, the responsed result `appversion` is always `1.0.0`, because it is a simulated normal user.

- The 4.4 If the request is made 100 times, about 80% of the response result `appversion` is `1.0.0`, and 20% is `1.0.1`, because it is a request to simulate an internal experimental user.
