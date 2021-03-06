package org.indigo.openstackprobe.openstack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.indigo.zabbix.utils.CloudProviderInfo;
import com.indigo.zabbix.utils.CmdbFeignClient;
import com.indigo.zabbix.utils.ProbeClientFactory;
import com.indigo.zabbix.utils.ProbesTags;
import com.indigo.zabbix.utils.PropertiesManager;
import com.indigo.zabbix.utils.beans.CmdbResponse;
import com.indigo.zabbix.utils.beans.ProviderInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/**
 * The CmdbClient class is in charge of the interactions between the probe and the CMDB component.
 * Such component provides information about the available providers, such as their name, location,
 * list of services provided, etc...
 * 
 * @author Reply
 *
 */
public class CmdbClientForOpenStack {
  private Client client = null;

  private String cmdbUrl;
  private static final String SERVICE_TYPE = "eu.egi.cloud.vm-management.openstack";
  private static final String NOVA_DEFUALT_PORT = "8774";
  private static final String IDENTITY_DEFAULT_PORT = "5000";
  private static final String VERSION_2 = "v2.0";
  private static final String VERSION_3 = "v3";
  private static final String OCCI_DEFAULT_PORT = "8787";

  private CmdbFeignClient cmdbClient;

  private static final Logger log = LogManager.getLogger(CmdbClientForOpenStack.class);

  /**
   * FEIGN used into Cmdb.
   */
  public CmdbClientForOpenStack() {
    try {
      PropertiesManager.loadProperties(OpenStackProbeTags.CONFIG_FILE);
    } catch (IOException e) {
      log.debug("Unable to load property file: {}", OpenStackProbeTags.CONFIG_FILE, e);
    }
    cmdbUrl = PropertiesManager.getProperty(ProbesTags.CMDB_URL);
    // Create the Client
    cmdbClient = ProbeClientFactory.getClient(CmdbFeignClient.class, cmdbUrl);
  }

  /**
   * This is a constructor for unit testing purposes.
   * 
   * @param mock Mock of the Jersey Client class
   */
  public CmdbClientForOpenStack(CmdbFeignClient mock, String cmdburlMocked) {
    cmdbUrl = cmdburlMocked;
    cmdbClient = mock;
  }

  /**
   * Using the created Jersey client, it invokes the CMDB REST API in order to retrieve the full
   * list of Cloud providers which are currently available.
   * 
   * @return Strings array with the identifiers of the providers found.
   */
  public String[] getProvidersList() {

    // Retrieve the services list
    CmdbResponse<ProviderInfo> jelement = cmdbClient.providerList();
    List<ProviderInfo> listArray = jelement.getRows();
    if (listArray != null || listArray.size() == 0) {
      return null;
    }

    ArrayList<String> providersList = new ArrayList<String>();
    Iterator<ProviderInfo> myIter = listArray.iterator();
    while (myIter.hasNext()) {
      String providerId = myIter.next().getId();
      providersList.add(providerId);
    }

    // Prepare the result
    providersList.trimToSize();
    String[] resultList = new String[providersList.size()];
    providersList.toArray(resultList);

    return resultList;
  }

  /**
   * Get the list of images.
   * 
   * @return array of strings
   */
  public String[] getImageList() {
    // Call to CMDB API
    JsonElement jelement = cmdbClient.providerImages();
    JsonObject parsedRes = jelement.getAsJsonObject();
    JsonArray listArray = parsedRes.getAsJsonArray("rows");
    if (listArray.isJsonNull() || listArray.size() == 0) {
      return null;
    }

    ArrayList<String> imageList = new ArrayList<>();
    Iterator<JsonElement> myIter = listArray.iterator();
    while (myIter.hasNext()) {
      JsonObject currentResource = myIter.next().getAsJsonObject();
      JsonObject valueObject = currentResource.getAsJsonObject("value");
      String imageJsonId = valueObject.get("image_id").getAsString();

      String imageId = imageJsonId != null ? imageJsonId : currentResource.get("id").getAsString();
      imageList.add(imageId);
    }

    // Prepare the result
    imageList.trimToSize();
    String[] resultList = new String[imageList.size()];
    imageList.toArray(resultList);

    return resultList;
  }

  /**
   * This method access the CMDB service in order to retrieve the available data from a Cloud
   * Provider (i.e. its location, provided services, etc.)
   * 
   * @param providerId Represents the identifier of the Cloud provider
   * @return An object with all the information retrieved
   */
  public CloudProviderInfo getProviderData(String providerId) {
    // Call to CMDB API

    JsonElement jelement = cmdbClient.providerInfo(providerId);
    JsonObject parsedRes = jelement.getAsJsonObject();
    JsonArray listArray = parsedRes.getAsJsonArray("rows");
    if (listArray.isJsonNull() || listArray.size() == 0) {
      return null;
    }
    String novaEndpoint = null;
    String keystoneEndpoint = null;
    int type = CloudProviderInfo.OPENSTACK;
    boolean isMonitored = false;
    boolean isBeta = false;
    boolean isProduction = false;

    Iterator<JsonElement> myIter = listArray.iterator();
    while (myIter.hasNext()) {
      JsonObject currentResource = myIter.next().getAsJsonObject();
      JsonObject currentDoc = currentResource.get("doc").getAsJsonObject();
      JsonObject currentData = currentDoc.get("data").getAsJsonObject();
      String currentServiceType = currentData.get("service_type").getAsString();
      String identityEndpoint = null;
      for (JsonElement obj : listArray) {
        JsonElement docIter = obj.getAsJsonObject().get("doc");
        JsonElement dataIter = docIter.getAsJsonObject().get("data");
        try {
          if (dataIter.getAsJsonObject().get("service_type").getAsString().equals(SERVICE_TYPE)) {
            currentServiceType = SERVICE_TYPE;
          }

          if (dataIter.getAsJsonObject().get("endpoint").getAsString()
              .contains(IDENTITY_DEFAULT_PORT)) {
            identityEndpoint = dataIter.getAsJsonObject().get("endpoint").getAsString();
          }

        } catch (UnsupportedOperationException uoe) {
          log.debug("unable to get the endpoint for the provider {}", providerId, uoe);
        }
      }
      JsonElement jsonEndpoint = currentData.get("endpoint");

      if (jsonEndpoint == null || jsonEndpoint.isJsonNull()) {
        return null;
      }

      if (currentServiceType.equalsIgnoreCase(SERVICE_TYPE)) {
        keystoneEndpoint = identityEndpoint;

        JsonElement currentBeta = currentData.get("beta");
        JsonElement currentProduction = currentData.get("in_production");
        JsonElement currentMonitored = currentData.get("node_monitored");

        if (currentBeta != null && currentBeta.getAsString().equalsIgnoreCase("Y")) {
          isBeta = true;
        }
        if (currentMonitored != null && currentMonitored.getAsString().equalsIgnoreCase("Y")) {
          isMonitored = true;
        }
        if (currentProduction != null && currentProduction.getAsString().equalsIgnoreCase("Y")) {
          isProduction = true;
        }

        return new CloudProviderInfo(providerId, "", keystoneEndpoint, type, isMonitored, isBeta,
            isProduction);
      }
    }
    return null;
  }

  /**
   * It makes use of the methods for listing providers and for getting individual information in
   * order to perform a filtering of providers, selecting only those suitable for the probe at this
   * stage: OpenStack providers, in production and with monitoring.
   * 
   * @return An ArrayList including info about the selected providers.
   */
  public List<CloudProviderInfo> getFeasibleProvidersInfo() {
    // Create the resulting list object
    List<CloudProviderInfo> myResult = new ArrayList<>();

    // First, retrieve the whole list of providers
    String[] providersList = getProvidersList();

    // Then, iterate all the providers and select
    for (int i = 0; i < providersList.length; i++) {
      // Retrieve all the info
      CloudProviderInfo currentInfo = getProviderData(providersList[i]);

      log.info("Got information about provider: " + providersList[i]);
      // Now check it is compliant with our requirements
      if (currentInfo != null) {
        int cloudType = currentInfo.getCloudType();
        if (cloudType == CloudProviderInfo.OPENSTACK) {
          log.info("Found candidate provider" + providersList[i]
              + " to be monitored by Openstack probe");
          myResult.add(currentInfo);
        } else {
          log.info("Provider" + providersList[i]
              + " not suitable for being processed by Openstack probe");
        }
      }
    }
    return myResult;
  }
}
