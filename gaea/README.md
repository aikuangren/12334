# Gaea 部署说明

当前配置按 Gaea skill 的「模式 F：纯前端 SPA/H5 + Nginx rootless」生成。

## 部署形态

```text
Gaea 容器
  |
  v
Nginx 8080
  |
  v
src/main/resources/static 中的 H5 静态资源
```

当前 H5 已改为轻量功能验证方案：

- 不启动 Java 服务。
- 不启动 Node.js 服务。
- 不连接 MySQL。
- 不配置 `/api/` 反向代理；`/api/` 请求会直接返回 404。
- 卡池规则从 `/assets/h5/config/card-pool-v1.json` 读取。
- 卡册、每日次数和抽卡历史记录在浏览器本地存储中。

## 生成文件

```text
gaea/Dockerfile
gaea/entrypoint.sh
gaea/nginx/nginx.conf
gaea/nginx/default.conf
```

## 环境变量

当前 Gaea 镜像无必填业务环境变量。`BACKEND_URL` 已移除，避免在无后端服务时把 `/api/` 请求代理到不存在的默认服务。

## 访问地址

容器内监听端口：

```text
8080
```

主要页面：

```text
/h5-c.html
/
```

健康检查：

```text
/nginx-health
```

## 本地构建示例

```bash
docker build -f gaea/Dockerfile -t card-draw-h5:gaea .
docker run --rm -p 8080:8080 card-draw-h5:gaea
```

访问：

```text
http://localhost:8080/h5-c.html
```

## 注意事项

- Dockerfile 使用公司内部镜像 `hub.51talk.biz/library/nginx:1.28.3-alpine3.23`。
- 容器使用 `USER gaea` rootless 方式启动。
- Nginx 日志通过软链输出到 stdout/stderr。
- `default.conf` 不包含 `/api/` 反向代理；`/api/` 请求显式返回 404，H5 的抽卡接口由页面内本地适配逻辑处理。
- `default.conf` 中设置了 `absolute_redirect off` 和 `port_in_redirect off`，避免容器内端口污染跳转地址。
- `/assets/h5/config/card-pool-v1.json` 不做长期缓存，便于发版更新配置。
