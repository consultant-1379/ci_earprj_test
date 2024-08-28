/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.test.availability;

import java.io.*;
import java.net.*;
import java.util.Properties;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.services.shm.test.enmupgrade.CppSynchServiceUpgradeTestIT;

public class ConfigParamUpdater {

    private static final String COM_ERICSSON_OSS_ITPF_COMMON__PLATFORMINTEGRARIONBRIDGE_EAR = "com.ericsson.oss.itpf.common:PlatformIntegrationBridge-ear";
    protected static final String PIB = "PIB";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigParamUpdater.class);

    private static String getVersion(String key) {
        Properties properties = new Properties();
        try {
            properties.load(CppSynchServiceUpgradeTestIT.class.getResourceAsStream("/version.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties.getProperty(key);

    }

    public static EnterpriseArchive createPibDeployment() {
        return ShrinkWrap.createFromZipFile(EnterpriseArchive.class, resolveAsFilesForEar(COM_ERICSSON_OSS_ITPF_COMMON__PLATFORMINTEGRARIONBRIDGE_EAR, getVersion("pib.version"))[0]);
    }

    private static File[] resolveAsFilesForEar(final String artifactCoordinates, final String version) {
        return Maven.resolver().loadPomFromFile("pom.xml").resolve(artifactCoordinates + ":ear:" + version).withoutTransitivity().asFile();
    }

    protected String updateConfiguredParam(String paramName, String paramValue) throws MalformedURLException, IOException, ProtocolException, InterruptedException {
        String returnVal = "";
        final URL url = new URL(generateRestURL(paramName, paramValue));
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        LOGGER.info("...Http request sent and Response is ....{}", conn.getResponseCode());
        if (conn.getResponseCode() != 200) {
            returnVal = "Failed : HTTP error code : " + conn.getResponseCode();
        }
        final BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String output;
        while ((output = br.readLine()) != null) {
            returnVal += output;
        }
        LOGGER.info("returnVal id : {}", returnVal);
        conn.disconnect();

        return returnVal;
    }

    private String generateRestURL(String paramName, String paramValue) {
        final String portOffset = System.getProperty("jboss.socket.binding.port-offset");
        final Integer port = new Integer(portOffset) + 8080;
        final StringBuilder triggerUrl = new StringBuilder();
        triggerUrl.append("http://localhost:");
        triggerUrl.append(port.toString());
        triggerUrl.append("/pib/configurationService/updateConfigParameterValue?paramName=");
        triggerUrl.append(paramName);
        triggerUrl.append("&paramValue=");
        triggerUrl.append(paramValue);
        triggerUrl.append("&serviceIdentifier=");
        triggerUrl.append("cppinventorysynchservice");

        LOGGER.info("URL constructed: " + triggerUrl.toString());
        return triggerUrl.toString();
    }

}
