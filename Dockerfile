# the base image that contains OpenJDK 11
FROM openjdk:11
# Add the fatjar in the image
COPY build/libs/fibo-docker-modules-1.0.jar /
# Default command
CMD ["java","-jar","fibo-docker-modules-1.0.jar"]
