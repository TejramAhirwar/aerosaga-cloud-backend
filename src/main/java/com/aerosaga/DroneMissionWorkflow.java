
package com.aerosaga;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.QueryMethod;

@WorkflowInterface
public interface DroneMissionWorkflow {

    @WorkflowMethod
    void executeMission(String droneId, String Destination);

    @QueryMethod
    String getMissionStatus();
}
