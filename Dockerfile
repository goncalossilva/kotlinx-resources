# Use an official OpenJDK runtime as a parent image.
# 'jammy' is based on Ubuntu 22.04 LTS.
FROM --platform=linux/amd64 eclipse-temurin:17-jdk-jammy

# Set the working directory inside the container.
WORKDIR /app

# Set environment variables to ensure correct architecture detection
ENV KOTLIN_NATIVE_ARCHITECTURE=x86_64
ENV KOTLIN_NATIVE_TARGET=linux_x64

# Copy the project files into the container
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Run the Linux x64 tests
CMD ["./gradlew", "clean", "linuxX64Test"]
