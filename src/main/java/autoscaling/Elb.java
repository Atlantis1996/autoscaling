package autoscaling;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList; // import the ArrayList class
import java.lang.InterruptedException;

import static autoscaling.AutoScale.PROJECT_VALUE;
import static autoscaling.AutoScale.configuration;
// import static autoscaling.AutoScale;

import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteTargetGroupResult;
// import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.ActionTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupAttributesRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyTargetGroupAttributesRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroupAttribute;
/**
 * ELB resources class.
 */
public final class Elb {
    /**
     * ELB Tags.
     */
    public static final List<Tag> ELB_TAGS_LIST = Arrays.asList(
            new Tag().withKey("Project").withValue(PROJECT_VALUE));

    /**
     * Unused default constructor.
     */
    private Elb() {
    }

    /**
     * Create a target group.
     *
     * @param elb elb client
     * @param ec2 ec2 client
     * @return target group instance
     */
    public static TargetGroup createTargetGroup(
            final AmazonElasticLoadBalancing elb,
            final AmazonEC2 ec2) {
        //TODO: Create Target Group

        CreateTargetGroupRequest request = new CreateTargetGroupRequest()
                                        .withHealthCheckPath("/")
                                        .withPort(AutoScale.HTTP_PORT)
                                        .withName(AutoScale.AUTO_SCALING_TARGET_GROUP)
                                        .withProtocol("HTTP")
                                        .withHealthCheckIntervalSeconds(60)
                                        .withHealthCheckTimeoutSeconds(5)
                                        .withUnhealthyThresholdCount(2)
                                        .withHealthyThresholdCount(10)
                                        .withTargetType("instance")
                                        .withVpcId(Ec2.getDefaultVPC(ec2));

        CreateTargetGroupResult response = elb.createTargetGroup(request);
        TargetGroup targetGroup = response.getTargetGroups().get(0);
        System.out.println("target group created");

        TargetGroupAttribute attribute = new TargetGroupAttribute().withKey("deregistration_delay.timeout_seconds").withValue("60");
        ModifyTargetGroupAttributesRequest modifyTargetGroupAttributesRequest = new ModifyTargetGroupAttributesRequest()
                                                                                    .withTargetGroupArn(targetGroup.getTargetGroupArn())
                                                                                    .withAttributes(attribute);
        elb.modifyTargetGroupAttributes(modifyTargetGroupAttributesRequest);

        return targetGroup;
    }

    /**
     * Create a load balancer.
     *
     * @param elb             ELB client
     * @param ec2             EC2 client
     * @param securityGroupId Security group ID
     * @param targetGroupArn  target group ARN
     * @return Load balancer instance
     */
    public static LoadBalancer createLoadBalancer (
            final AmazonElasticLoadBalancing elb,
            final AmazonEC2 ec2,
            final String securityGroupId,
            final String targetGroupArn) throws InterruptedException{

        //TODO: 
        //  - Get Subnets for us-east-1 region
        //  - Create Load Balancer
        //  - Attach Listener
        // Listener listener = new Listener().builder()  
        //                                 .protocol("HTTP")
        //                                 .loadBalancerPort(80)
        //                                 .instancePort(80)
        //                                 .build();

        // List<Listener> listeners =  new ArrayList<Listener>(1);
        // listeners.add(listener);

        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest()
                                                .withName(AutoScale.LOAD_BALANCER_NAME)
                                                .withSubnets("subnet-c040328d", "subnet-2dec4372")
                                                .withTags(ELB_TAGS_LIST)
                                                .withSecurityGroups(securityGroupId)
                                                .withType(LoadBalancerTypeEnum.Application);

        CreateLoadBalancerResult response = elb.createLoadBalancer(request);

        LoadBalancer loadBalancer = response.getLoadBalancers().get(0);
        String loadBalancerArn = loadBalancer.getLoadBalancerArn();

        while (!loadBalancer.getState().getCode().equals("active")) {
            Thread.sleep(900); // sleep < 1 sec
            DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest()
                                                        .withLoadBalancerArns(loadBalancerArn);
            loadBalancer = elb.describeLoadBalancers(describeLoadBalancersRequest).getLoadBalancers().get(0);
        }
        System.out.println("loadbalancer arn is ");
        System.out.println(loadBalancer.getLoadBalancerArn());
        CreateListenerRequest listenerRequest = new CreateListenerRequest()
                                                                .withPort(80)
                                                                .withProtocol("HTTP")
                                                                .withDefaultActions(new Action().withType(ActionTypeEnum.Forward).withTargetGroupArn(targetGroupArn))
                                                                .withLoadBalancerArn(loadBalancerArn);
        elb.createListener(listenerRequest);
        return loadBalancer;
    }

    /**
     * Delete the load balancer.
     * @param elb             LoadBalancing client
     * @param loadBalancerArn load balancer ARN
     */
    public static void deleteLoadBalancer(final AmazonElasticLoadBalancing elb,
                                          final String loadBalancerArn) {
       //TODO: Delete LoadBalancer
       DeleteLoadBalancerRequest request = new DeleteLoadBalancerRequest().withLoadBalancerArn(loadBalancerArn);
       DeleteLoadBalancerResult response = elb.deleteLoadBalancer(request);
    }

    /**
     * Delete Target Group.
     *
     * @param elb            ELB Client
     * @param targetGroupArn target Group ARN
     */
    public static void deleteTargetGroup(final AmazonElasticLoadBalancing elb,
                                         final String targetGroupArn) {
       //TODO: Delete Target Group
       DeleteTargetGroupRequest request = new DeleteTargetGroupRequest().withTargetGroupArn(targetGroupArn);
       DeleteTargetGroupResult response = elb.deleteTargetGroup(request);
    }
}
