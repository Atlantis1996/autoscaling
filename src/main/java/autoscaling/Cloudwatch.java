package autoscaling;


import com.amazonaws.services.cloudwatch.AmazonCloudWatch;

import static autoscaling.AutoScale.configuration;

import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;

import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.cloudwatch.model.Dimension;
/**
 * CloudWatch resources.
 */
public final class Cloudwatch {

    /**
     * Sixty seconds.
     */
    private static final Integer ALARM_PERIOD
            = configuration.getInt("alarm_period");
    /**
     * CPU Lower Threshold.
     */
    private static final Double CPU_LOWER_THRESHOLD
            = configuration.getDouble("cpu_lower_threshold");
    /**
     * CPU Upper Threshold.
     */
    private static final Double CPU_UPPER_THRESHOLD
            = configuration.getDouble("cpu_upper_threshold");
    /**
     * Alarm Evaluation Period out.
     */
    public static final int ALARM_EVALUATION_PERIODS_SCALE_OUT
            = configuration.getInt("alarm_evaluation_periods_scale_out");
    /**
     * Alarm Evaluation Period in.
     */
    public static final int ALARM_EVALUATION_PERIODS_SCALE_IN
            = configuration.getInt("alarm_evaluation_periods_scale_in");

    /**
     * Unused constructor.
     */
    private Cloudwatch() {
    }

    /**
     * Create Scale out alarm.
     *
     * @param cloudWatch cloudWatch instance
     * @param policyArn  policy ARN
     */
    public static void createScaleOutAlarm(final AmazonCloudWatch cloudWatch,
                                           final String policyArn) {
         //TODO: Create scale-out alarm policy
         Dimension dimension = new Dimension()
                                .withName("AutoScalingGroupName")
                                .withValue(AutoScale.AUTO_SCALING_GROUP_NAME);
         PutMetricAlarmRequest request = new PutMetricAlarmRequest()    
                                                .withAlarmName("ScaleOut")
                                                .withComparisonOperator("LessThanThreshold")
                                                .withEvaluationPeriods(ALARM_EVALUATION_PERIODS_SCALE_OUT)
                                                .withMetricName("CPUUtilization")
                                                .withPeriod(ALARM_PERIOD)
                                                .withNamespace("AWS/EC2")
                                                .withStatistic("Average")
                                                .withThreshold(CPU_LOWER_THRESHOLD)
                                                // .withActionsEnabled(false)
                                                // .withAlarmDescription("Alarm when server CPU utilization drops below threshold")
                                                .withDimensions(dimension)
                                                .withAlarmActions(policyArn);

        cloudWatch.putMetricAlarm(request);
    }

    /**
     * Create ScaleIn Alarm.
     *
     * @param cloudWatch cloud watch instance
     * @param policyArn  policy Arn
     */
    public static void createScaleInAlarm(final AmazonCloudWatch cloudWatch,
                                          final String policyArn) {
        //TODO: Create scale-in alarm policy
        Dimension dimension = new Dimension()
                        .withName("AutoScalingGroupName")
                        .withValue(AutoScale.AUTO_SCALING_GROUP_NAME);
        PutMetricAlarmRequest request = new PutMetricAlarmRequest()    
                                                .withAlarmName("ScaleIn")
                                                .withComparisonOperator("GreaterThanThreshold")
                                                .withEvaluationPeriods(ALARM_EVALUATION_PERIODS_SCALE_IN)
                                                .withMetricName("CPUUtilization")
                                                .withPeriod(ALARM_PERIOD)
                                                .withNamespace("AWS/EC2")
                                                .withStatistic("Average")
                                                .withThreshold(CPU_UPPER_THRESHOLD)
                                                // .withActionsEnabled(false)
                                                // .withAlarmDescription("Alarm when server CPU utilization exceeds threshold")
                                                .withDimensions(dimension)
                                                .withAlarmActions(policyArn);
    
        cloudWatch.putMetricAlarm(request);
    }

    /**
     * Delete the two above Alarms.
     *
     * @param cloudWatch cloud watch client
     */
    public static void deleteAlarms(final AmazonCloudWatch cloudWatch) {
         //TODO: Delete all alarm resources
        DeleteAlarmsRequest request = new DeleteAlarmsRequest().withAlarmNames("ScaleIn");
     
        cloudWatch.deleteAlarms(request);

        request = new DeleteAlarmsRequest().withAlarmNames("ScaleOut");
     
        cloudWatch.deleteAlarms(request);
    }
}
