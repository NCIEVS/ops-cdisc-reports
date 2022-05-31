package gov.nih.nci.evs.cdisc.report.aws;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class LambdaHandlerTest {

    @Mock
    private Context mockContext;

    private LambdaHandler lambdaHandler = new LambdaHandler();

    @Test
    public void test(){
    }
}
