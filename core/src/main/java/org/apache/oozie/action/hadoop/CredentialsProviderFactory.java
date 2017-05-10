/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.action.hadoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.oozie.service.ConfigurationService;
import org.apache.oozie.util.XLog;

public class CredentialsProviderFactory {
    public static final String CRED_KEY = "oozie.credentials.credentialclasses";
    private static final XLog LOG = XLog.getLog(CredentialsProviderFactory.class);
    private static final Map<String, Class<CredentialsProvider>> providerCache = new HashMap<>();
    private static CredentialsProviderFactory instance;

    public static CredentialsProviderFactory getInstance() throws ClassNotFoundException {
        if(instance == null) {
            instance = new CredentialsProviderFactory();
        }
        return instance;
    }

    private CredentialsProviderFactory() throws ClassNotFoundException {
        for (String function : ConfigurationService.getStrings(CRED_KEY)) {
            function = trim(function);
            LOG.debug("Creating Credential class for : " + function);
            String[] str = function.split("=");
            if (str.length > 0) {
                String type = str[0];
                String classname = str[1];
                if (classname != null) {
                    LOG.debug("Creating Credential type : '{0}', class Name : '{1}'", type, classname);
                    Class<?> klass = null;
                    try {
                        klass = Thread.currentThread().getContextClassLoader().loadClass(classname);
                    }
                    catch (ClassNotFoundException ex) {
                        LOG.warn("Exception while loading the class '{0}'", classname, ex);
                        throw ex;
                    }
                    providerCache.put(type, (Class<CredentialsProvider>) klass);
                } else {
                    LOG.warn("Credential provider class is null for '{0}', skipping", type);
                }
            }
        }
    }

    /**
     * Create Credential object
     *
     * @return Credential object
     * @throws Exception
     */
    public CredentialsProvider createCredentialsProvider(String type) throws Exception {
        return providerCache.get(type).newInstance();
    }

    /**
     * Relogs into Kerberos using the Keytab for the Oozie server user.  This should be called before attempting to get delegation
     * tokens via {@link CredentialsProvider} implementations to ensure that the Kerberos credentials are current and won't expire
     * too soon.
     *
     * @throws IOException
     */
    public static void ensureKerberosLogin() throws IOException {
        LOG.debug("About to relogin from keytab");
        UserGroupInformation.getLoginUser().checkTGTAndReloginFromKeytab();
        LOG.debug("Relogin from keytab successful");
    }

    /**
     * To trim string
     *
     * @param str
     * @return trim string
     */
    public String trim(String str) {
        if (str != null) {
            str = str.replaceAll("\\n", "");
            str = str.replaceAll("\\t", "");
            str = str.trim();
        }
        return str;
    }
}
