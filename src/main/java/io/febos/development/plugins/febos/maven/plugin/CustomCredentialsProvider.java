/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.febos.development.plugins.febos.maven.plugin;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Michel M. <michel@febos.cl>
 */
class CustomCredentialsProvider implements AWSCredentialsProvider {

    public Properties props;
    public FebosCredentials credentials;

    public void load(File properties){
        FebosCredentials c=new FebosCredentials(properties);
        credentials=c;
        props=c.props;
    }
    @Override
    public AWSCredentials getCredentials() {
        return credentials;
    }

    @Override
    public void refresh() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public class FebosCredentials implements AWSCredentials {
        public Properties props;
        public FebosCredentials(File properties) {
            props = new Properties();
            try {
                props.load(new FileInputStream(properties));
            } catch (IOException ex) {
                Logger.getLogger(CustomCredentialsProvider.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public Properties getProps(){
            return this.props;
        }
        
        @Override
        public String getAWSAccessKeyId() {
            return props.getProperty("key");
        }

        @Override
        public String getAWSSecretKey() {
            return props.getProperty("secret");
        }

    }

}
