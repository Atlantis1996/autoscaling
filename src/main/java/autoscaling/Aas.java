package autoscaling;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import java.lang.InterruptedException;
import java.util.Arrays;
import java.util.List;

import static autoscaling.AutoScale.PROJECT_VALUE;
import static autoscaling.AutoScale.configuration;
// import static autoscaling.AutoScale;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.Instance;

import java.util.ArrayList;
/**
 * Amazon AutoScaling resource class.
 */
public final class Aas {
    /**
     * Max size of AGS.
     */
    private static final Integer MAX_SIZE_ASG
            = configuration.getInt("asg_max_size");

        /**
     * Min size of AGS.
     */
    private static final Integer MIN_SIZE_ASG
                = configuration.getInt("asg_min_size");

    /**
     * Health Check grace period.
     */
    private static final Integer HEALTH_CHECK_GRACE
            = configuration.getInt("health_check_grace_period");
    /**
     * Cool down period Scale In.
     */
    private static final Integer COOLDOWN_PERIOD_SCALEIN
            = configuration.getInt("cool_down_period_scale_in");

    /**
     * Cool down period Scale Out.
     */
    private static final Integer COOLDOWN_PERIOD_SCALEOUT
            = configuration.getInt("cool_down_period_scale_out");

    /**
     * Number of instances to scale out by.
     */
    private static final int SCALING_OUT_ADJUSTMENT
            = configuration.getInt("scale_out_adjustment");
    /**
     * Number of instances to scale in by.
     */
    private static final int SCALING_IN_ADJUSTMENT
            = configuration.getInt("scale_in_adjustment");

    /**
     * ASG Cool down period in seconds.
     */
    private static final Integer COOLDOWN_PERIOD_ASG
            = configuration.getInt("asg_default_cool_down_period");

    /**
     * Unused constructor.
     */
    private Aas() {
    }

    /**
     * AAS Tags List.
     */
    private static final List<Tag> AAS_TAGS_LIST = Arrays.asList(
            new Tag().withKey("Project").withValue(PROJECT_VALUE));

    /**
     * Create launch configuration.
     *
     * @param aas AAS client
     */
    static void createLaunchConfiguration(final AmazonAutoScaling aas) {
        //TODO: Implement this method 

        CreateLaunchConfigurationRequest request = new CreateLaunchConfigurationRequest()
                                                        .withLaunchConfigurationName(AutoScale.LAUNCH_CONFIGURATION_NAME)
                                                        .withImageId(AutoScale.WEB_SERVICE)
                                                        .withSecurityGroups(AutoScale.ELBASG_SECURITY_GROUP)
                                                        .withInstanceType(AutoScale.INSTANCE_TYPE)
                                                        .withInstanceMonitoring(new InstanceMonitoring().withEnabled(true));
        
        aas.createLaunchConfiguration(request);
    }

    /**
     * Create auto scaling group.
     * Create and attach Cloud Watch Policies.
     *
     * @param aas            AAS Client
     * @param cloudWatch     CloudWatch client
     * @param targetGroupArn target group arn
     */
    public static void createAutoScalingGroup(final AmazonAutoScaling aas,
                                              final AmazonCloudWatch cloudWatch,
                                              final String targetGroupArn) {
        //TODO: Implement this method
        CreateAutoScalingGroupRequest request = new CreateAutoScalingGroupRequest()
                                                .withAutoScalingGroupName(AutoScale.AUTO_SCALING_GROUP_NAME)
                                                .withLaunchConfigurationName(AutoScale.LAUNCH_CONFIGURATION_NAME)
                                                .withTargetGroupARNs(targetGroupArn)
                                                .withHealthCheckType("EC2")
                                                .withTags(AAS_TAGS_LIST)
                                                .withMaxSize(MAX_SIZE_ASG)
                                                .withMinSize(MIN_SIZE_ASG)
                                                .withDesiredCapacity(1)
                                                .withDefaultCooldown(COOLDOWN_PERIOD_ASG)
                                                .withHealthCheckGracePeriod(HEALTH_CHECK_GRACE)
                                                .withAvailabilityZones("us-east-1a");

        CreateAutoScalingGroupResult response = aas.createAutoScalingGroup(request);
        System.out.println("ASG created");

        PutScalingPolicyRequest scaleIn = new PutScalingPolicyRequest()
                                                .withAdjustmentType("ChangeInCapacity")
                                                .withAutoScalingGroupName(AutoScale.AUTO_SCALING_GROUP_NAME)
                                                .withCooldown(COOLDOWN_PERIOD_SCALEIN)
                                                .withScalingAdjustment(SCALING_IN_ADJUSTMENT)
                                                .withPolicyName("scaleIn");

        PutScalingPolicyRequest scaleOut = new PutScalingPolicyRequest()
                                                .withAdjustmentType("ChangeInCapacity")
                                                .withAutoScalingGroupName(AutoScale.AUTO_SCALING_GROUP_NAME)
                                                .withCooldown(COOLDOWN_PERIOD_SCALEOUT)
                                                .withScalingAdjustment(SCALING_OUT_ADJUSTMENT)
                                                .withPolicyName("scaleOut");

        String inArn = aas.putScalingPolicy(scaleIn).getPolicyARN();
        String outArn = aas.putScalingPolicy(scaleOut).getPolicyARN();

        Cloudwatch.createScaleInAlarm(cloudWatch, inArn);
        Cloudwatch.createScaleOutAlarm(cloudWatch, outArn);
        
        System.out.println("cw created");
        
    }

    /**
     * Terminate auto scaling group.
     *
     * @param aas AAS client
     */
    public static void terminateAutoScalingGroup(final AmazonAutoScaling aas) throws InterruptedException{
        //TODO: Implement this method
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest()
                                                                        .withAutoScalingGroupName(AutoScale.AUTO_SCALING_GROUP_NAME)
                                                                        .withDesiredCapacity(0)
                                                                        .withMaxSize(0)
                                                                        .withMinSize(0);
        aas.updateAutoScalingGroup(updateAutoScalingGroupRequest);

        DescribeAutoScalingGroupsRequest autoScalingGroupsRequest = new DescribeAutoScalingGroupsRequest()
                                                                        .withAutoScalingGroupNames(AutoScale.AUTO_SCALING_GROUP_NAME);
        List<Instance> instances = aas.describeAutoScalingGroups(autoScalingGroupsRequest).getAutoScalingGroups().get(0).getInstances();  
        while (!instances.isEmpty()) {
                Thread.sleep(800);
                instances = aas.describeAutoScalingGroups(autoScalingGroupsRequest).getAutoScalingGroups().get(0).getInstances();
        } 
        Thread.sleep(30000);                 
        DeleteAutoScalingGroupRequest request = new DeleteAutoScalingGroupRequest()
                                                .withAutoScalingGroupName(AutoScale.AUTO_SCALING_GROUP_NAME);
        DeleteAutoScalingGroupResult response = aas.deleteAutoScalingGroup(request);
    }

    /**
     * Delete launch configuration.
     *
     * @param aas AAS client
     */
    public static void deleteLaunchConfiguration(final AmazonAutoScaling aas) {
        //TODO: Implement this method
        DeleteLaunchConfigurationRequest request = new DeleteLaunchConfigurationRequest()
                                                        .withLaunchConfigurationName(AutoScale.LAUNCH_CONFIGURATION_NAME);
        DeleteLaunchConfigurationResult response = aas.deleteLaunchConfiguration(request);
    }
}
