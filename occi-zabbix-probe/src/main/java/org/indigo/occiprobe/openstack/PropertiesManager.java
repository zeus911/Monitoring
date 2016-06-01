package org.indigo.occiprobe.openstack;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

public class PropertiesManager 
{
	public static final String KEYSTONE_LOCATION = "openstack.keystoneurl";
	public static final String KEYSTONE_PORT = "openstack.keystoneport";
	public static final String OCCI_LOCATION = "openstack.occiurl";
	public static final String OCCI_PORT = "openstack.occiport";
	public static final String OPENSTACK_USER = "openstack.user";
	public static final String OPENSTACK_PASSWORD = "openstack.password";	
	public static final String JAVA_KEYSTORE = "java.keystore";
	public static final String ZABBIX_IP = "zabbix.ip";
	public static final String ZABBIX_SENDER = "zabbix.sender.location";
	public static final String CMDB_URL = "cmdb.location";
	
	private HashMap <String, String> propertiesList;
	
	public PropertiesManager ()
	{
		propertiesList = new HashMap <String, String> ();
		readProperties();		
	}
	
	private void readProperties()
	{
		Properties prop = new Properties();
				
        
     
      	// Get class loader, for avoiding problems with Tomcat (loading local files)
        ClassLoader loader = PropertiesManager.class.getClassLoader();
        if(loader==null)
            loader = ClassLoader.getSystemClassLoader();
 
        try 
        {
        	// We want to load file located in WEB-INF/classes/
            String fileName = "occiprobe.properties";
            InputStream is = loader.getResourceAsStream(fileName);
            prop.load(is);
        } 
        catch(IOException e) 
        {
            System.out.println(e.toString());
        }
        
        //Put all the properties into the map
        for (Enumeration e = prop.keys(); e.hasMoreElements() ; ) 
        {
            // Get the property
            String obj = (String) e.nextElement();
            propertiesList.put(obj, prop.getProperty(obj));           
        }
	}
	
	public String getProperty (String propertyName)
	{
		return propertiesList.get(propertyName);
	}
	
	 public static void main(String[] args)
	 {
		 PropertiesManager myProp = new PropertiesManager();
		 System.out.println ("KEYSTONE URL: " + myProp.getProperty("openstack.keystoneurl"));
	 }
}
