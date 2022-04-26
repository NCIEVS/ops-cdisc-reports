package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaHandler implements RequestHandler<LambdaRequest, String> {
  @Override
  public String handleRequest(LambdaRequest input, Context context) {

    return "{'status':'success'}";
  }
}
