package autoscaling;

import java.lang.InterruptedException;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressResult;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.CreateTagsResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;

import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.ec2.model.TagSpecification;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.ini4j.Ini;
import utilities.Configuration;

/**
 * Main AutoScaling Task class.
 */
public final class AutoScale {

    /**
     * Configuration file
     */
    final static Configuration configuration = new Configuration("auto-scaling-config.json");

    /**
     * Project Tag value.
     */
    public static final String PROJECT_VALUE = "2.1";

    /**
     * HTTP Port.
     */
    static final Integer HTTP_PORT = 80;

    /**
     * LG Security group Name.
     */
    private static final String LG_SECURITY_GROUP =
            "lgSecurityGroup";
    /**
     * ELB/ASG Security group Name.
     */
    static final String ELBASG_SECURITY_GROUP =
            "ELBASGSecurityGroup";

    /**
     * Load Generator AMI.
     */
    private static final String LOAD_GENERATOR_AMI_ID
            = configuration.getString("load_generator_ami");

    /**
     * Web Service AMI.
     */
    static final String WEB_SERVICE
            = configuration.getString("web_service_ami");

    /**
     * Instance Type Name.
     */
    static final String INSTANCE_TYPE
            = configuration.getString("instance_type");

    /**
     * Security Key Name 
     */
    static final String KEY_NAME = configuration.getString("key_name");;

    /**
     * Auto Scaling Target Group Name.
     */
    static final String AUTO_SCALING_TARGET_GROUP
            = configuration.getString("auto_scaling_target_group");;

    /**
     * Load Balancer Name.
     */
    static final String LOAD_BALANCER_NAME
            = configuration.getString("load_balancer_name");;

    /**
     * Launch Configuration Name.
     */
    static final String LAUNCH_CONFIGURATION_NAME
            = configuration.getString("launch_configuration_name");;

    /**
     * Auto Scaling group name.
     */
    static final String AUTO_SCALING_GROUP_NAME
            = configuration.getString("auto_scaling_group_name");;

    /**
     * Tags list.
     */
    static final List<Tag> TAGS_LIST = Arrays.asList(
            new Tag().withKey("Project").withValue(PROJECT_VALUE));

    /**
     * Whether the Load Generator should be deleted at the end of the run.
     */
    private static final boolean DELETE_LOAD_GENERATOR = true;

    /**
     * Delay before retrying API call.
     */
    public static final int RETRY_DELAY_MILLIS = 100;



    /**
     *  Main method to run the auto-scaling Task2.
     * @param args No args required
     */
    public static void main(final String[] args) throws InterruptedException{
        AWSCredentialsProvider credentialsProvider =
                new DefaultAWSCredentialsProviderChain();

        // Create an Amazon Ec2 Client
        final AmazonEC2 ec2 = AmazonEC2ClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.US_EAST_1)
                .build();

        // Create an Amazon auto scaling client
        final AmazonAutoScaling aas = AmazonAutoScalingClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.US_EAST_1)
                .build();

        // Create an ELB client
        final AmazonElasticLoadBalancing elb
                = AmazonElasticLoadBalancingClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.US_EAST_1)
                .build();

        // Create a cloudwatch client
        final AmazonCloudWatch cloudWatch = AmazonCloudWatchClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.US_EAST_1)
                .build();

        runAutoScalingTask(ec2, aas, elb, cloudWatch);

    }

    /**
     * Run the autoscaling task.
     * @param ec2 EC2
     * @param aas AAS
     * @param elb ELB
     * @param cloudWatch Cloud watch Interface
     */
    private static void runAutoScalingTask (
            AmazonEC2 ec2,
            AmazonAutoScaling aas,
            AmazonElasticLoadBalancing elb,
            AmazonCloudWatch cloudWatch) throws InterruptedException{
        // BIG PICTURE TODO: Programmatically provision autoscaling resources
        //   - Create security groups for Load Generator and ASG, ELB
        //   - Provision a Load Generator
        //   - Generate a Launch Configuration
        //   - Create a Target Group
        //   - Provision a Load Balancer
        //   - Associate Target Group with Load Balancer 
        //   - Create an Autoscaling Group 
        //   - Initialize Warmup Test
        //   - Initialize Autoscaling Test
        //   - Terminate Resources

        ResourceConfig resourceConfig = initializeResources(ec2, elb, aas, cloudWatch);
        resourceConfig = initializeTestResources(ec2, resourceConfig);

        executeTest(resourceConfig);

        destroy(aas, ec2, elb, cloudWatch, resourceConfig);
    }

    /**
     * Intialize Auto-scaling Task Resources.
     * @param ec2 EC2 client
     * @param elb ELB Client
     * @param aas AAS Client
     * @param cloudWatch Cloud Watch Client
     * @return Load Balancer instance
     */
    private static ResourceConfig initializeResources(final AmazonEC2 ec2,
                                        final AmazonElasticLoadBalancing elb,
                                        final AmazonAutoScaling aas,
                                        final AmazonCloudWatch cloudWatch) throws InterruptedException{

        //TODO: Create a target group and a load balancer instance 
        //      check Elb.java for more information
        //TODO: Create an auto scaling group. 
        //      check Aas.java for more information
        Ec2.createHttpSecurityGroup(ec2, LG_SECURITY_GROUP);
        Ec2.createHttpSecurityGroup(ec2, ELBASG_SECURITY_GROUP);
        
        String lgSecurityGroupId = Ec2.getSecurityGroupId(ec2, LG_SECURITY_GROUP);
        String elbAndAsgSecurityGroupId = Ec2.getSecurityGroupId(ec2, ELBASG_SECURITY_GROUP);

        TargetGroup targetGroup = Elb.createTargetGroup(elb, ec2);
        String targetGroupArn = targetGroup.getTargetGroupArn();

        LoadBalancer loadBalancer = Elb.createLoadBalancer(elb, ec2, elbAndAsgSecurityGroupId, targetGroupArn);
        
        String loadBalancerDNS = loadBalancer.getDNSName();
        String loadBalancerArn = loadBalancer.getLoadBalancerArn();

        Aas.createLaunchConfiguration(aas);
        Aas.createAutoScalingGroup(aas, cloudWatch, targetGroupArn);

        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.setTargetGroupArn(targetGroupArn);
        resourceConfig.setLoadBalancerArn(loadBalancerArn);
        resourceConfig.setLoadBalancerDns(loadBalancerDNS);
        return resourceConfig;
    }

    /**
     * Create a load Generator and initialize test.
     * @param ec2 EC2 client
     * @param config Resource configuration
     * @return config Resource configuration
     */
    public static autoscaling.ResourceConfig initializeTestResources(final AmazonEC2 ec2,
                                                                     final ResourceConfig config) throws InterruptedException {

        //TODO: Create a Load Generator instance
        TagSpecification tagspecification =  new TagSpecification();
        tagspecification.setTags(TAGS_LIST);
        Instance loadGenerator = Ec2.launchInstance(ec2, new TagSpecification().withTags(TAGS_LIST).withResourceType("instance"), 
            LOAD_GENERATOR_AMI_ID, INSTANCE_TYPE, KEY_NAME, LG_SECURITY_GROUP, true);    

        RunInstancesRequest runLoadGeneratorRequest = new RunInstancesRequest();

        config.setLoadGeneratorDns(loadGenerator.getPublicDnsName());
        config.setLoadGeneratorID(loadGenerator.getInstanceId());
        return config;
    }

    /**
     * Execute auto scaling test.
     * @param resourceConfig Resource configuration
     */
    public static void executeTest(ResourceConfig resourceConfig) {
        boolean submissionPasswordUpdated = false;

        while (!submissionPasswordUpdated) {
            try {
                Api.authenticate(resourceConfig.getLoadGeneratorDns());
                submissionPasswordUpdated = true;
            } catch (Exception e) {
                try {
                    Thread.sleep(100); //small sleep > 1s
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }


        //Submit ELB DNS to Load Generator for Warmup test 
        executeWarmUp(resourceConfig);

        //Submit ELB DNS to Load Generator for Auto-scaling test
        boolean testStarted = false;
        String response = "";
        while (!testStarted) {
            try {
                response
                = Api.initializeTest(resourceConfig.getLoadGeneratorDns(),
                        resourceConfig.getLoadBalancerDns());
                testStarted = true;

            } catch (Exception e) {
                //Ignore errors
            }
        }

        //Test started
        waitForTestEnd(resourceConfig, response);
    }


    /**
     * Execute warm-upp test using API.
     * @param resourceConfig Resource Configuration
     */
    private static void executeWarmUp(ResourceConfig resourceConfig) {
        boolean warmupStarted = false;
        String warmupResponse = "";
        while (!warmupStarted) {
            try {
                warmupResponse = Api.initializeWarmup(resourceConfig.getLoadGeneratorDns(),
                        resourceConfig.getLoadBalancerDns());
                warmupStarted = true;
            } catch (Exception e) {
                try {
                    Thread.sleep(RETRY_DELAY_MILLIS);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }

        //Test started
        waitForTestEnd(resourceConfig, warmupResponse);
    }


    /**
     * Wait For Test Execution to be complete.
     * @param resourceConfig Resource Configuration
     * @param response Response from Test Initialization.
     */
    private static void waitForTestEnd(ResourceConfig resourceConfig, String response) {
        try {
            Ini ini = Api.getIniUpdate(resourceConfig.getLoadGeneratorDns(),
                    Api.getTestId(response));
            while (ini == null || !ini.containsKey("Test finished")) {
                ini = Api.getIniUpdate(resourceConfig.getLoadGeneratorDns(),
                        Api.getTestId(response));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Destroy all resources created for the task.
     * @param aas AmazonAutoScaling
     * @param ec2 AmazonEC2
     * @param elb AmazonElasticLoadBalancing
     * @param cloudWatch AmazonCloudWatch
     * @param resourceConfig Resource Configuration
     */
    public static void destroy(final AmazonAutoScaling aas,
                               final AmazonEC2 ec2,
                               final AmazonElasticLoadBalancing elb,
                               final AmazonCloudWatch cloudWatch,
                               final ResourceConfig resourceConfig) throws InterruptedException{

        //TODO: Terminate All Resources
                        
        Elb.deleteLoadBalancer(elb, resourceConfig.getLoadBalancerArn());
        System.out.println("lb deleted");

        Aas.terminateAutoScalingGroup(aas);
        System.out.println("asg deleted");

        Aas.deleteLaunchConfiguration(aas);
        System.out.println("lc deleted");




        Cloudwatch.deleteAlarms(cloudWatch);
        System.out.println("cw deleted");
        TerminateInstancesRequest request = new TerminateInstancesRequest()
                                                .withInstanceIds(resourceConfig.getLoadGeneratorID());
        ec2.terminateInstances(request);
        String dns = resourceConfig.getLoadGeneratorDns();

        while(!dns.equals("")) {
            Thread.sleep(800);
            dns = Ec2.getInstance(ec2, resourceConfig.getLoadGeneratorID()).getPublicDnsName();
        }

        Ec2.deleteSecurityGroup(ec2, ELBASG_SECURITY_GROUP);
        Thread.sleep(10000);
        Ec2.deleteSecurityGroup(ec2, LG_SECURITY_GROUP);

        Elb.deleteTargetGroup(elb, resourceConfig.getTargetGroupArn());
        System.out.println("tg deleted");
    }

}
