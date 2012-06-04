package org.jclouds.examples.ec2.windows;

import org.jclouds.ec2.domain.InstanceType;
import org.kohsuke.args4j.Option;

/**
 * A javabean that represents the application's command line arguments.
 *
 * @author Richard Downer
 */
public class Arguments {
   private String identity;
   private String credential;
   private String region;
   private static final String IMAGE_NAME_PATTERN_DEFAULT = "Windows_Server-2008-R2_SP1-English-64Bit-Base-";
   private String imageNamePattern = IMAGE_NAME_PATTERN_DEFAULT;
   private static final String INSTANCE_TYPE_DEFAULT = InstanceType.M1_SMALL;
   private String instanceType = INSTANCE_TYPE_DEFAULT;
   private static final String AMI_OWNER_DEFAULT = "801119661308";
   private String amiOwner = AMI_OWNER_DEFAULT;

   public String getIdentity() {
      return identity;
   }

   public String getCredential() {
      return credential;
   }

   public String getRegion() {
      return region;
   }

   public String getImageNamePattern() {
      return imageNamePattern;
   }

   public String getInstanceType() {
      return instanceType;
   }

   public String getAmiOwner() {
      return amiOwner;
   }

   @Option(name = "--identity", aliases = "-i", required = true, usage = "your AWS access key ID")
   public void setIdentity(String identity) {
      this.identity = identity;
   }

   @Option(name = "--credential", aliases = "-c", required = true, usage = "your AWS secret access key")
   public void setCredential(String credential) {
      this.credential = credential;
   }

   @Option(name = "--region", aliases = "-r", required = true, usage = "AWS region name")
   public void setRegion(String region) {
      this.region = region;
   }

   @Option(name = "--image-pattern", aliases = "-p", usage = "regular expression to select an AMI; default=" + IMAGE_NAME_PATTERN_DEFAULT)
   public void setImageNamePattern(String imageNamePattern) {
      this.imageNamePattern = imageNamePattern;
   }

   @Option(name = "--instance-type", aliases = "-t", usage = "instance type; default=" + INSTANCE_TYPE_DEFAULT)
   public void setInstanceType(String instanceType) {
      this.instanceType = instanceType;
   }

   @Option(name = "--ami-owner", aliases = "-o", usage = "AMI owner account ID; default=" + AMI_OWNER_DEFAULT)
   public void setAmiOwner(String amiOwner) {
      this.amiOwner = amiOwner;
   }
}
