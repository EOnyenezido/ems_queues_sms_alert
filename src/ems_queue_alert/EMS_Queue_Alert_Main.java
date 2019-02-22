package ems_queue_alert;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

public class EMS_Queue_Alert_Main {
   private static boolean triggerToExternalTeam = false;

   public static boolean isInteger(String str) {
      if (str == null) {
         return false;
      } else {
         int length = str.length();
         if (length == 0) {
            return false;
         } else {
            int i = 0;
            if (str.charAt(0) == '-') {
               if (length == 1) {
                  return false;
               }

               i = 1;
            }

            while(i < length) {
               char c = str.charAt(i);
               if (c < '0' || c > '9') {
                  return false;
               }

               ++i;
            }

            return true;
         }
      }
   }

   private static ArrayList<String> getQueues(Properties config) {
      ArrayList<String> queueList = new ArrayList();
      String queue = null;

      try {
         Process newProcess;
         BufferedReader newReader;
         if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            newProcess = Runtime.getRuntime().exec(config.getProperty("ems_home") + "/bin/tibemsadmin -server " + config.getProperty("biz_server") + " -user " + config.getProperty("username") + " -password " + config.getProperty("password") + " -script " + config.getProperty("script"));
            newReader = new BufferedReader(new InputStreamReader(newProcess.getInputStream()));

            while((queue = newReader.readLine()) != null) {
               queueList.add(queue);
            }

            newProcess = Runtime.getRuntime().exec(config.getProperty("ems_home") + "/bin/tibemsadmin -server " + config.getProperty("aud_server") + " -user " + config.getProperty("username") + " -password " + config.getProperty("password") + " -script " + config.getProperty("script"));
            newReader = new BufferedReader(new InputStreamReader(newProcess.getInputStream()));

            while((queue = newReader.readLine()) != null) {
               queueList.add(queue);
            }
         } else {
            newProcess = Runtime.getRuntime().exec(config.getProperty("ems_home") + "/bin/tibemsadmin -server " + config.getProperty("biz_server") + " -user " + config.getProperty("username") + " -password " + config.getProperty("password") + " -script " + config.getProperty("script"));
            newReader = new BufferedReader(new InputStreamReader(newProcess.getInputStream()));

            while((queue = newReader.readLine()) != null) {
               queueList.add(queue);
            }

            newProcess = Runtime.getRuntime().exec(config.getProperty("ems_home") + "/bin/tibemsadmin -server " + config.getProperty("aud_server") + " -user " + config.getProperty("username") + " -password " + config.getProperty("password") + " -script " + config.getProperty("script"));
            newReader = new BufferedReader(new InputStreamReader(newProcess.getInputStream()));

            while((queue = newReader.readLine()) != null) {
               queueList.add(queue);
            }
         }

         newProcess.waitFor();
         newProcess.destroy();
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      return queueList;
   }

   public static ArrayList<String> checkForPendingMessages(Properties config, ArrayList<String> queues) {
      Integer internalPendingMessageLevel = Integer.parseInt(config.getProperty("int_pending_msg_level"));
      Integer externalPendingMessageLevel = Integer.parseInt(config.getProperty("ext_pending_msg_level"));
      Integer bwpmPendingMessageLevel = Integer.parseInt(config.getProperty("bwpm_pending_msg_level"));
      Integer customLoggingPendingMessageLevel = Integer.parseInt(config.getProperty("custom_logging_queue_pending_msg_level"));
      Integer elasticLoggingPendingMessageLevel = Integer.parseInt(config.getProperty("elastic_logging_queue_pending_msg_level"));
      String publishingQueueToBeIgnored = config.getProperty("publishing_queue");
      ArrayList<String> pendingQueues = new ArrayList();
      Iterator var8 = queues.iterator();

      while(true) {
         while(true) {
            while(var8.hasNext()) {
               String line = (String)var8.next();
               String[] queueDetails = line.split("\\s+");
               if (queueDetails.length > 5 && isInteger(queueDetails[5]) && !queueDetails[1].matches("bwpm.*") && !queueDetails[1].equals("EMTS.NG.PROD.RQ.Q.SPF.loggingQueue") && !queueDetails[1].equals("EMTS.NG.PROD.RQ.Q.SPF.elasticsearchQueue")) {
                  if (Integer.parseInt(queueDetails[5]) > externalPendingMessageLevel && !queueDetails[1].equals(publishingQueueToBeIgnored)) {
                     pendingQueues.add(queueDetails[1] + ": " + queueDetails[5] + " pending messages");
                     triggerToExternalTeam = true;
                  } else if (Integer.parseInt(queueDetails[5]) > internalPendingMessageLevel && !queueDetails[1].equals(publishingQueueToBeIgnored)) {
                     pendingQueues.add(queueDetails[1] + ": " + queueDetails[5] + " pending messages");
                  }
               } else if (queueDetails.length > 5 && queueDetails[1].equals("bwpm.event") && isInteger(queueDetails[5]) && Integer.parseInt(queueDetails[5]) > bwpmPendingMessageLevel) {
                  pendingQueues.add(queueDetails[1] + ": " + queueDetails[5] + " pending messages");
               } else if (queueDetails.length > 5 && queueDetails[1].equals("EMTS.NG.PROD.RQ.Q.SPF.loggingQueue") && isInteger(queueDetails[5]) && Integer.parseInt(queueDetails[5]) > customLoggingPendingMessageLevel) {
                   pendingQueues.add(queueDetails[1] + ": " + queueDetails[5] + " pending messages");
               } else if (queueDetails.length > 5 && queueDetails[1].equals("EMTS.NG.PROD.RQ.Q.SPF.elasticsearchQueue") && isInteger(queueDetails[5]) && Integer.parseInt(queueDetails[5]) > elasticLoggingPendingMessageLevel) {
                   pendingQueues.add(queueDetails[1] + ": " + queueDetails[5] + " pending messages");
               }
            }

            if (pendingQueues.isEmpty()) {
               return null;
            }

            return pendingQueues;
         }
      }
   }

   private static int sendSMS(String smsGatewayUrl, String msisdn, String msg, String fromAddress) throws Exception {
      int responseCode = 1;
      String url = smsGatewayUrl + "?username=tester&password=foobar&to=" + msisdn + "&text=" + msg + "&from=" + fromAddress;
      URL obj = new URL(url);
      HttpURLConnection con = (HttpURLConnection)obj.openConnection();
      con.setRequestMethod("GET");
      System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Sending SMS request to URL : " + smsGatewayUrl);
      responseCode = con.getResponseCode();
      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      StringBuilder response = new StringBuilder();

      String inputLine;
      while((inputLine = in.readLine()) != null) {
         response.append(inputLine);
      }

      in.close();
      System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Response from SMS Gateway: " + responseCode + " - " + response.toString());
      if (responseCode == 200 || responseCode == 201 || responseCode == 202) {
         responseCode = 0;
         System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": SMS successfully sent to: " + msisdn);
      }

      return responseCode;
   }

   public static void main(String[] args) throws InterruptedException {
      System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Loading Configuration from File.");
      Properties config = new Properties();
      FileInputStream input = null;

      try {
         input = new FileInputStream("configuration.txt");
         config.load(input);
         System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Configuration loaded successfully.");
      } catch (IOException var24) {
         System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Unable to read properties from File");
         var24.printStackTrace();
      } finally {
         if (input != null) {
            try {
               input.close();
            } catch (IOException var22) {
               var22.printStackTrace();
            }
         }

      }

      Integer checkInterval = isInteger(config.getProperty("chk_interval")) && Integer.parseInt(config.getProperty("chk_interval")) >= 5000 ? Integer.parseInt(config.getProperty("chk_interval")) : 10000;
      Runtime.getRuntime().addShutdownHook(new Thread() {
         public void run() {
            System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Shutting Down...");
            System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Stopped");
         }
      });

      while(true) {
         ArrayList<String> queues = getQueues(config);
         ArrayList<String> pendingQueues = checkForPendingMessages(config, queues);
         if (pendingQueues == null) {
            System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": No pending Messages");
         } else {
            String[] phoneNumbers = null;
            String team = null;
            if (triggerToExternalTeam) {
               phoneNumbers = config.getProperty("external_phone_numbers").split(",");
               team = "External Team";
            } else {
               phoneNumbers = config.getProperty("internal_phone_numbers").split(",");
               team = "Internal Team";
            }

            String msg = "";

            String fromAddress;
            for(Iterator var10 = pendingQueues.iterator(); var10.hasNext(); msg = msg + fromAddress.replace(" ", "+") + "+") {
               fromAddress = (String)var10.next();
            }

            System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Pending messages: " + msg);
            fromAddress = config.getProperty("from_address");
            fromAddress = fromAddress == null ? "TIB_Pending_Msgs" : fromAddress;
            String smsGatewayUrl = config.getProperty("sms_gateway_url");
            System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Sending SMS to " + team);
            String[] var14 = phoneNumbers;
            int var13 = phoneNumbers.length;

            for(int var12 = 0; var12 < var13; ++var12) {
               String number = var14[var12];

               try {
                  sendSMS(smsGatewayUrl, number, msg, fromAddress);
               } catch (Exception var23) {
                  System.out.println((new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.sss")).format(new Date()) + ": Unable to send SMS to: " + number);
                  var23.printStackTrace();
               }
            }
         }

         Thread.sleep((long)checkInterval);
      }
   }
}