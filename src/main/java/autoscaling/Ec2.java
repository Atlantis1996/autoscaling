package autoscaling;

import static autoscaling.AutoScale.PROJECT_VALUE;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;

import java.util.Arrays;
import java.util.List;

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
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import java.lang.InterruptedException;
/**
 * Class to manage EC2 resources.
 */
public final class Ec2 {

    /**
     * EC2 Tags.
     */
    public static final List<Tag> EC2_TAGS_LIST = Arrays.asList(
            new Tag().withKey("Project").withValue(PROJECT_VALUE));

    /**
     * Unused default constructor.
     */
    private Ec2() {
    }

    /**
     * Launch an Ec2 Instance.
     * @param ec2 EC2Client
     * @param tagSpecification TagsSpecified to create instance
     * @param amiId amiId
     * @param instanceType Type of instance
     * @param keyName Security key pair name
     * @param securityGroup Security Group
     * @param detailedMonitoring With Detailed Monitoring Enabled
     * @return Instance object
     */
    public static Instance launchInstance(final AmazonEC2 ec2,
                                          final TagSpecification tagSpecification,
                                          final String amiId,
                                          final String instanceType,
                                          final String keyName,
                                          final String securityGroupName,
                                          final Boolean detailedMonitoring) throws InterruptedException{
        //TODO: Launch EC2 instances 
        // - Create a Run Instance Request
        // - Wait for VM to start running
        // - Return the Object Reference of the Instance just Launched
        // - TODO Create Instance and wait for it to start running

        RunInstancesRequest request = new RunInstancesRequest();

        request.withTagSpecifications(tagSpecification)
                                .withImageId(amiId)
                                .withInstanceType(instanceType)
                                .withKeyName(keyName)
                                .withSecurityGroups(securityGroupName)
                                .withMonitoring(detailedMonitoring)
                                .withMinCount(1)
                                .withMaxCount(1);
                
        RunInstancesResult result = ec2.runInstances(request);

        Instance instance = result.getReservation()
                                    .getInstances()
                                    .get(0);

        String instanceId = instance.getInstanceId();
        while(!instance.getState().getName().equals("running")) {
            Thread.sleep(800); // sleep < 1 sec
            instance = getInstance(ec2, instanceId);
        }
        System.out.println("load generator dns is ");
        System.out.println(instance.getPublicDnsName());
        return instance;
    }

    /**
     * Get instance object by ID.
     * @param ec2 Ec2 client instance
     * @param instanceId isntance ID
     * @return Instance Object
     */
    protected static Instance getInstance(final AmazonEC2 ec2,
                                        final String instanceId) {
        //TODO: get Instance by ID
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult response = ec2.describeInstances(request);
        for(Reservation reservation : response.getReservations()) {
            for(Instance instance : reservation.getInstances()) {
                if (instance.getInstanceId().equals(instanceId)) {
                    return instance;
                }
            }
        }
        return null;
    }

    /**
     * Create a new HTTPSecurity Group.
     * @param ec2 EC2Client instance
     * @param securityGroup Security group name
     */
    static void createHttpSecurityGroup(final AmazonEC2 ec2,
                                        final String securityGroupName) {
        //TODO:
        // - Create Security Group
        // - Add permission to security group
        IpPermission ipPermission = new IpPermission();
        IpRange ip_range = new IpRange().withCidrIp("0.0.0.0/0");
        ipPermission.withIpv4Ranges(ip_range)
                    .withIpProtocol("tcp")
                    .withFromPort(22)
                    .withToPort(80);

        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest();

        request.withGroupName(securityGroupName).withDescription("My security group");

        CreateSecurityGroupResult result = ec2.createSecurityGroup(request);


        AuthorizeSecurityGroupIngressRequest request2 = new AuthorizeSecurityGroupIngressRequest();

        request2.withGroupName(securityGroupName)
                .withIpPermissions(ipPermission);

        ec2.authorizeSecurityGroupIngress(request2);

    }

    /**
     * Get the ID of the default VPC of the region.
     * @param ec2 Ec2 Client
     * @return VPC Id
     */
    static String getDefaultVPC(final AmazonEC2 ec2) {
        //TODO: get region default VPC ID

        return ec2.describeVpcs().getVpcs().get(0).getVpcId();
    }

    /**
     * Fetch a Security Group's ID by Name.
     * @param ec2 Ec2 client
     * @param groupName group name String
     * @return group ID
     */
    static String getSecurityGroupId(final AmazonEC2 ec2,
                                     final String groupName) {
        //TODO: Get Security Group ID
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult response = ec2.describeSecurityGroups(request);

        for(SecurityGroup securityGroup : response.getSecurityGroups()) {
            if (securityGroup.getGroupName().equals(groupName)) {
                return securityGroup.getGroupId();
            }
        }
        return null;
    }

    /**
     * Delete a Security group.
     * @param ec2 ec2 client
     * @param elbSecurityGroup security group name
     */
    static void deleteSecurityGroup(final AmazonEC2 ec2,
                                    final String elbSecurityGroup) {
        //TODO: Delete all security groups
        DeleteSecurityGroupRequest request = new DeleteSecurityGroupRequest();

        request.withGroupName(elbSecurityGroup);

        ec2.deleteSecurityGroup(request);
    }
}
