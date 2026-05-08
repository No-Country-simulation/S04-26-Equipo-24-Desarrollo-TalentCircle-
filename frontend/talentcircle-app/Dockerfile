# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY .. .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:stable-alpine
# Copiamos el build de Vite (carpeta dist) a la ruta de Nginx
COPY --from=build /app/dist /usr/share/nginx/html
# Exponemos el puerto 80
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]