FROM node:22-alpine AS web
ARG VITE_PAYMENTS=fake
ARG VITE_STRIPE_PUBLISHABLE_KEY=
ENV VITE_PAYMENTS=$VITE_PAYMENTS VITE_STRIPE_PUBLISHABLE_KEY=$VITE_STRIPE_PUBLISHABLE_KEY
WORKDIR /src
RUN corepack enable
COPY frontend/package.json frontend/pnpm-lock.yaml ./
RUN --mount=type=cache,target=/root/.local/share/pnpm/store pnpm install --frozen-lockfile
COPY frontend/ ./
RUN pnpm build

FROM nginx:1.28-alpine
COPY --from=web /src/dist /usr/share/nginx/html
COPY deploy/compose/nginx/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY deploy/compose/nginx/security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY deploy/compose/nginx/allowlist.sh /docker-entrypoint.d/10-allowlist.sh
RUN chmod +x /docker-entrypoint.d/10-allowlist.sh
