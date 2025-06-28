# Use Node.js runtime
FROM node:18-alpine

# Set the working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy application files
COPY . .

# Create necessary directories
RUN mkdir -p logs uploads uploads/thumbnails

# Expose the port (Render will map this to $PORT)
EXPOSE 3000

# Start the server
CMD ["npm", "start"] 