# Używamy lekkiego obrazu Node.js
FROM node:18-alpine

# Tworzymy folder roboczy w kontenerze
WORKDIR /usr/src/app

# Kopiujemy pliki z zależnościami i instalujemy je
COPY package*.json ./
RUN npm install

# Kopiujemy resztę kodu (index.js)
COPY . .

# Wystawiamy port 3000
EXPOSE 3000

# Komenda uruchamiająca serwer
CMD ["node", "index.js"]
