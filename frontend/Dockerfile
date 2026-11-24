# Production image: build the static app, serve it with nginx and strict security headers.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
COPY . .
RUN npm run build

FROM nginx:1.28-alpine
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=build /app/dist /usr/share/nginx/html
# The API the web app proxies to (same origin, so no CORS is needed).
ENV API_UPSTREAM=http://api:8000
EXPOSE 80
