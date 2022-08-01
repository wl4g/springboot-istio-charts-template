# Demo httpserver project for Spring Boot

## Building

```bash
git clone https://github.com/wl4g/demo-httpserver.git
cd demo-httpserver
mvn clean package -DskipTests -P build:tar:docker
```

## Run

```bash
docker run --rm wl4g/demo-httpserver:1.0.0
```

## Testing

```bash
curl -skv -XPOST -d '{"author":"James Wrong","sex":"Man"}' localhost:8080/demo/echo?name=jack
```
