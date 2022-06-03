FROM public.ecr.aws/lambda/java:11

# Copy function code and runtime dependencies from Gradle layout
COPY build/classes/java/main ${LAMBDA_TASK_ROOT}
COPY build/resources/main ${LAMBDA_TASK_ROOT}
COPY build/dependencies/* ${LAMBDA_TASK_ROOT}/lib/

RUN chmod 644 $(find . -type f)
RUN chmod 755 $(find . -type d)
# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler::handleRequest" ]