# 纯前端 H5 轻量验证版：Nginx rootless 静态资源部署
FROM hub.51talk.biz/library/nginx:1.28.3-alpine3.23

RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.ustc.edu.cn/g' /etc/apk/repositories

ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

RUN addgroup -g 1001 gaea && adduser -u 1001 -G gaea -D gaea

COPY --chown=gaea:gaea src/main/resources/static /usr/share/nginx/html
COPY gaea/nginx/nginx.conf /etc/nginx/nginx.conf
COPY gaea/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --chmod=755 gaea/entrypoint.sh /entrypoint.sh

RUN mkdir -p /var/cache/nginx /tmp/nginx \
    && ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log \
    && chown -R gaea:gaea /usr/share/nginx/html /var/cache/nginx /tmp/nginx

USER gaea

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s \
  CMD wget -qO- http://127.0.0.1:8080/nginx-health || exit 1

ENTRYPOINT ["/entrypoint.sh"]
