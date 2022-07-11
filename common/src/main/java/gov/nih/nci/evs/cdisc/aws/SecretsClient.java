package gov.nih.nci.evs.cdisc.aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class SecretsClient {

  /**
   * Gets secrets stored in AWS secrets manager
   * @param secretName
   * @return plain text secret
   */
  public static String getSecret(String secretName) {
    SecretsManagerClient client =
        SecretsManagerClient.builder().region(Region.of(System.getenv("AWS_REGION"))).build();
    GetSecretValueRequest getSecretValueRequest =
        GetSecretValueRequest.builder().secretId(secretName).build();
    try {
      GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
      return getSecretValueResponse.secretString();
    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Error occurred when getting secret %s", secretName), e);
    }
  }
}
